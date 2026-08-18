package com.sap.sse.landscape.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.Release;
import com.sap.sse.landscape.ReleaseRepository;
import com.sap.sse.util.HttpUrlConnectionHelper;

/**
 * Can enumerate the {@link Release}s published by a GitHub repository and search releases whose name starts with a
 * specific prefix. This assumes a public GitHub repository where releases can be freely downloaded from
 * <code>https://github.com/{owner}/{repo}/releases/download/{release-name}</code>. The {@code api.github.com}'s
 * {@code /releases} end point delivers the releases in descending chronological order, so newest releases first. With
 * this, we can cache old results and try to get along with the harsh rate limit of only 60 requests per hour when used
 * without authentication.
 * <p>
 * 
 * Due to the harsh rate limits we restrict loading even of the first page to once every two minutes; multiple requests
 * within this duration will be answered from the cache. With this, a single instance of this class will typically
 * request <em>all</em> releases only once, cache all these releases, and then look for newer releases at most every two
 * minutes, thereby staying well within limits.
 * <p>
 * 
 * Enumerating the releases works through the inner class {@link ReleaseIterator}. If the last loading request for the
 * first page happened more than those two minutes ago, another such request will be made and the new, yet uncached
 * releases obtained from it will be added to the cache. Then, enumeration starts on the cache, delivering the newest
 * release first. When the oldest cached element has been delivered through the iterator, the next action depends on
 * whether or not the cache {@link #cacheContainsOldestRelease contains the oldest release} already. If so, no older
 * release can exist, and iteration ends. Otherwise, more requests for further paginated release documents are sent
 * until no more pages are found or releases older than the so far oldest release from the cache are found and added to
 * the cache. Iteration then continues on the cache again.
 * <p>
 * 
 * The class is thread-safe in that it allows multiple threads to obtain iterators on a single instance of this class.
 * The loading and caching of releases pages from GitHub, the invocation of the {@link #iterator()} method and the
 * {@link ReleaseIterator#hasNext()} and {@link ReleaseIterator#next()} methods all obtain this object's monitor
 * ({@code synchronized}). This may cause one iterator having to wait for another iterator's implicit loading actions.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 */
public class GithubReleasesRepository extends AbstractReleaseRepository implements ReleaseRepository {
    private final static Logger logger = Logger.getLogger(GithubReleasesRepository.class.getName());
    private static final SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    private final static String GITHUB_API_BASE_URL = "https://api.github.com";
    private final static String GITHUB_BASE_URL = "https://github.com";
    private final static int NUMBER_OF_RELEASES_PER_PAGE = 100; // default would be 30; maximum is 100
    private final String owner;
    private final String repositoryName;
    
    /**
     * The cache of releases as loaded from the GitHub web site. The cache is filled when iterating using a
     * {@link ReleaseIterator}, by loading paginated release records, converting them to {@link GithubRelease} objects
     * and storing them in this cache.
     * <p>
     * 
     * The cache does not guarantee to contain the newest releases, nor does it guarantee to go back all the way to the
     * oldest release. Its contents are contiguous in the sense of how the releases are returned by the GitHub API in
     * descending order of publication, from new to old. In other words, if there is a release cached that was published
     * at time point {@code t1} and another at a later time point {@code t2}, then the cache is guaranteed to contain
     * all releases published in the time range {@code [t1:t2]} (inclusive).
     * <p>
     * 
     * Should a {@link ReleaseIterator} have enumerated all releases back to the oldest one, the
     * {@link #cacheContainsOldestRelease} flag will be set to {@code true} which means that when an iteration has
     * reached the oldest release in the cache, iteration is complete, and no further page loading is necessary
     * to complete the iteration.
     */
    private final ConcurrentNavigableMap<TimePoint, Release> releasesByPublishingTimePoint;
    
    private boolean cacheContainsOldestRelease;
    
    /**
     * If {@link #cacheContainsOldestRelease} is {@code false}, meaning the cache hasn't seen all releases back to the oldest
     * one, this field holds the URL of the next page to load that will obtain older releases. Note that if newer releases
     * have been published since loading the so far oldest cached releases, one or more newer releases may have slipped
     * into that next page. But page loading of older releases will add only releases effectively older than the oldest
     * release in the cache so far.<p>
     * 
     * It starts out with {@link #getReleasesURL()}.<p>
     * 
     * Will be {@code null} if {@link #cacheContainsOldestRelease} is {@code true}.
     */
    private String nextPageURLForOlderReleases;
    
    private TimePoint lastFetchOfNewestReleases;
    
    private final static Duration RELOAD_NEWEST_RELEASES_AFTER_DURATION = Duration.ONE_MINUTE.times(2);
    
    /**
     * If {@link GithubReleasesRepository#lastFetchOfNewestReleases} is {@code null} or older than the
     * {@link GithubReleasesRepository#RELOAD_NEWEST_RELEASES_AFTER_DURATION}, the page with newest releases is actually
     * loaded. Otherwise, we assume that within the
     * {@link GithubReleasesRepository#RELOAD_NEWEST_RELEASES_AFTER_DURATION} interval changes are sufficiently
     * unlikely, so we will set the {@link #cachedReleasesIterator} to directly serve the current contents of the cache.
     * <p>
     * 
     * Always fetches the first page from the {@code /releases} end point and starts constructing and
     * {@link GithubReleasesRepository#releasesByPublishingTimePoint caching} releases, until a publishing time point
     * overlap with {@link GithubReleasesRepository#releasesByPublishingTimePoint} is found. Iteration then starts from
     * that cache. If the iterator has returned all elements from the cache going backwards in publishing history, and
     * {@link GithubReleasesRepository#cacheContainsOldestRelease} is {@code false}, indicating that the cache does not
     * go back to the "beginning of time," and still more elements are requested from this iterator, paginated release
     * documents need to get loaded again until we find even older releases than the oldest one from the cache. The
     * loaded elements will be added to the cache, and a new internal iterator is launched on the cache starting from
     * the then loaded element.
     * <p>
     * 
     * All releases found by loading a page are added to the
     * {@link GithubReleasesRepository#releasesByPublishingTimePoint} cache. If the page with the oldest sequence of
     * releases has been loaded (there is no next page then anymore), the
     * {@link GithubReleasesRepository#cacheContainsOldestRelease} flag is set to {@code true}.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class ReleaseIterator implements Iterator<Release> {
        /**
         * Used to enumerate the cached elements; will get assigned a new iterator after having reached the "old" end of
         * the cache and {@link GithubReleasesRepository#loadPageWithNextOlderReleases() loading more older releases}.
         */
        private Iterator<Release> cachedReleasesIterator;
        
        private ReleaseIterator() throws MalformedURLException, IOException, ParseException {
            synchronized (GithubReleasesRepository.this) {
                final TimePoint now = TimePoint.now();
                if (lastFetchOfNewestReleases != null && lastFetchOfNewestReleases.until(now)
                        .compareTo(RELOAD_NEWEST_RELEASES_AFTER_DURATION) < 0) {
                    logger.fine(()->"No need to fetch page with newest releases; did that at "+lastFetchOfNewestReleases);
                } else {
                    logger.fine(()->"Need to fetch page with newest releases because last request was at "+
                            (lastFetchOfNewestReleases==null?"<never>":lastFetchOfNewestReleases));
                    fillCacheWithNewestReleases();
                    lastFetchOfNewestReleases = now;
                }
                cachedReleasesIterator = releasesByPublishingTimePoint.descendingMap().values().iterator();
            }
        }
        
        @Override
        public boolean hasNext() {
            synchronized (GithubReleasesRepository.this) {
                // - we're delivering from the cache and the cache has more elements, or
                // - we've reached the end of the cache but the cache doesn't contain the oldest release so we can load more pages
                return cachedReleasesIterator.hasNext() || !cacheContainsOldestRelease;
            }
        }

        @Override
        public Release next() {
            synchronized (GithubReleasesRepository.this) {
                final Release result;
                if (cachedReleasesIterator.hasNext()) {
                    result = cachedReleasesIterator.next();
                } else {
                    if (cacheContainsOldestRelease) {
                        throw new NoSuchElementException();
                    } else {
                        final TimePoint oldestReleaseSoFar = releasesByPublishingTimePoint.firstKey();
                        try {
                            loadPageWithNextOlderReleases();
                        } catch (IOException | ParseException e) {
                            throw new RuntimeException(e);
                        }
                        cachedReleasesIterator = releasesByPublishingTimePoint.headMap(oldestReleaseSoFar).descendingMap().values().iterator();
                        if (!cachedReleasesIterator.hasNext()) {
                            throw new NoSuchElementException();
                        } else {
                            result = cachedReleasesIterator.next();
                        }
                    }
                }
                return result;
            }
        }
    }
    
    public GithubReleasesRepository(String owner, String repositoryName, String defaultReleaseNamePrefix) {
        super(defaultReleaseNamePrefix);
        this.owner = owner;
        this.repositoryName = repositoryName;
        this.releasesByPublishingTimePoint = new ConcurrentSkipListMap<>();
        this.cacheContainsOldestRelease = false;
        this.nextPageURLForOlderReleases = getReleasesURL();
        this.lastFetchOfNewestReleases = null;
    }
    
    /**
     * Loads a page of releases from {@code pageURL} and returns the link to the next page, or {@code null} if this was
     * the last page available (therefore containing the oldest releases). The releases loaded are added to the cache if
     * they are outside of the contiguous time range between oldest and newest publishing date as it was found in the
     * cache when this method is invoked.
     * <p>
     * 
     * If the {@code GITHUB_TOKEN} environment variable is set, it is used as a bearer token forthe Github requests,
     * resulting in a higher rate limit. If no token is set, or if using the token results in a 401 response code
     * (authentication failed), an unauthenticated request is tried instead.
     * <p>
     * 
     * The method makes no changes to the cache or any other state of this instance.
     * 
     * @return the link to the next page in the returned pair's {@link Pair#getA() A component}, and the sequence of
     *         releases loaded from that page with their publishing time points, ordered from newest to oldest.
     */
    private synchronized Pair<String, Iterable<Pair<TimePoint, GithubRelease>>> getReleasesFromPage(String pageURL) throws IOException, ParseException {
        logger.info("Requesting releases page "+pageURL);
        final URLConnection connection;
        final String githubToken = System.getenv("GITHUB_TOKEN");
        if (Util.hasLength(githubToken)) {
            final HttpURLConnection authenticatedConnectionAttempt = (HttpURLConnection) HttpUrlConnectionHelper
                    .redirectConnectionWithBearerToken(new URL(pageURL), githubToken);
            if (authenticatedConnectionAttempt.getResponseCode() == 401) {
                try {
                    authenticatedConnectionAttempt.disconnect();
                } catch (Exception e) {
                    logger.warning("Couldn't disconnect from Github: "+e.getMessage());
                }
                connection = HttpUrlConnectionHelper.redirectConnection(new URL(pageURL));
            } else {
                connection = authenticatedConnectionAttempt;
            }
        } else {
            connection = HttpUrlConnectionHelper.redirectConnection(new URL(pageURL));
        }
        final InputStream index = (InputStream) connection.getContent();
        final String xRatelimitRemaining = connection.getHeaderField("x-ratelimit-remaining");
        logger.fine(()->""+xRatelimitRemaining+" requests left in this hour");
        if (xRatelimitRemaining != null && Integer.valueOf(xRatelimitRemaining) <= 0) {
            throw new RuntimeException("You hit the rate limit of "+connection.getHeaderField("x-ratelimit-limit"));
        }
        final String linkHeader = connection.getHeaderField("link");
        final String nextPageURL = linkHeader == null ? null : getNextPageURL(linkHeader);
        logger.fine(()->nextPageURL==null?"This was the last page":("Next page will be "+nextPageURL));
        final List<Pair<TimePoint, GithubRelease>> publishingTimePointsAndReleases = new LinkedList<>();
        final JSONArray releasesJson = (JSONArray) new JSONParser().parse(new InputStreamReader(index));
        for (final Object releaseObject : releasesJson) {
            publishingTimePointsAndReleases.add(getPublishedAtAndReleaseFromJson((JSONObject) releaseObject));
        }
        return new Pair<>(nextPageURL, publishingTimePointsAndReleases);
    }
    
    /**
     * Loads at least one page of releases, starting with {@link #getReleasesURL()}. The method ensures that a time-wise
     * overlap with any pre-existing cache entries is established so that the cache entries are a contiguous prefix of
     * the list of all releases that exist in the repository.
     * <p>
     * 
     * In particular, if the cache is empty when this method is called, only the first page needs loading.
     * <p>
     * 
     * If the cache already contained one or more releases when this method is called, page loading continues
     * with the next page until a page contains a release published not after the newest release in the cache
     * at the time when this method was called.
     */
    private synchronized void fillCacheWithNewestReleases() throws IOException, ParseException {
        final boolean cacheWasEmpty = releasesByPublishingTimePoint.isEmpty();
        final TimePoint publishingTimePointOfLatestReleaseSoFar = cacheWasEmpty ? null : releasesByPublishingTimePoint.lastKey();
        String nextPageURL = getReleasesURL();
        boolean overlap = false;
        do {
            final Pair<String, Iterable<Pair<TimePoint, GithubRelease>>> pageResults = getReleasesFromPage(nextPageURL);
            for (final Pair<TimePoint, GithubRelease> publishingTimePointAndRelease : pageResults.getB()) {
                overlap = !cacheWasEmpty && !publishingTimePointAndRelease.getA().after(publishingTimePointOfLatestReleaseSoFar);
                if (cacheWasEmpty || publishingTimePointAndRelease.getA().after(publishingTimePointOfLatestReleaseSoFar)) {
                    releasesByPublishingTimePoint.put(publishingTimePointAndRelease.getA(), publishingTimePointAndRelease.getB());
                }
            }
            nextPageURL = pageResults.getA();
            if (cacheWasEmpty) {
                rememberNextPageForOlderReleases(nextPageURL);
            }
        } while (!cacheWasEmpty && !overlap);
    }

    private void rememberNextPageForOlderReleases(String nextPageURL) {
        nextPageURLForOlderReleases = nextPageURL;
        if (nextPageURL == null) {
            cacheContainsOldestRelease = true;
        }
    }
    
    /**
     * Loads the page referenced by {@link #nextPageURLForOlderReleases}, adjusting that very field to point to either
     * the next older page or {@code null}, and adjusting {@link #cacheContainsOldestRelease} accordingly. Releases
     * older than the so far oldest release are added to the cache. If {@link #cacheContainsOldestRelease} is
     * {@code false}, the process continues until at least one older release has been added to the cache.<p>
     * 
     * Precondition: {@code !}{@link #cacheContainsOldestRelease} {@code && }{@link #nextPageURLForOlderReleases}{@code != null}
     */
    private synchronized void loadPageWithNextOlderReleases() throws IOException, ParseException {
        assert !cacheContainsOldestRelease;
        final TimePoint publishingTimePointOfOldestReleaseInCacheSoFar = releasesByPublishingTimePoint.firstKey();
        boolean addedAtLeastOneReleaseToCache = false;
        do {
            final Pair<String, Iterable<Pair<TimePoint, GithubRelease>>> pageResults = getReleasesFromPage(nextPageURLForOlderReleases);
            for (final Pair<TimePoint, GithubRelease> publishedAtAndRelease : pageResults.getB()) {
                if (publishedAtAndRelease.getA().before(publishingTimePointOfOldestReleaseInCacheSoFar)) {
                    addedAtLeastOneReleaseToCache = true;
                    releasesByPublishingTimePoint.put(publishedAtAndRelease.getA(), publishedAtAndRelease.getB());
                }
            }
            rememberNextPageForOlderReleases(pageResults.getA());
        } while (!addedAtLeastOneReleaseToCache && !cacheContainsOldestRelease);            
    }
    
    @Override
    public Release getLatestRelease(String releaseNamePrefix) {
        Release result = null;
        for (final Release release : this) { // invokes the iterator() method
            if (release.getBaseName().equals(releaseNamePrefix)) {
                result = release;
                break; // here we assume that releases are enumerated from newest to oldest
            }
        }
        return result;
    }

    private String getRepositoryPath() {
        return owner+"/"+repositoryName;
    }
    
    private String getReleasesURL() {
        return GITHUB_API_BASE_URL+"/repos/"+getRepositoryPath()+"/releases?per_page="+NUMBER_OF_RELEASES_PER_PAGE;
    }

    @Override
    public Release getRelease(String releaseName) {
        return new GithubRelease(releaseName, GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+releaseName+Release.ARCHIVE_EXTENSION,
                GITHUB_BASE_URL+"/"+getRepositoryPath()+"/releases/download/"+releaseName+"/"+Release.RELEASE_NOTES_FILE_NAME);
    }
    
    @Override
    public Iterator<Release> iterator() {
        try {
            return new ReleaseIterator();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<TimePoint, GithubRelease> getPublishedAtAndReleaseFromJson(JSONObject releaseJson) {
        final String name = releaseJson.get("name").toString();
        final String publishedAtISO = releaseJson.get("published_at").toString();
        TimePoint publishedAt;
        try {
            publishedAt = TimePoint.of(isoDateTimeFormat.parse(publishedAtISO));
        } catch (java.text.ParseException e) {
            logger.warning("Couldn't read published_at time stamp for release "+name+": "+publishedAtISO);
            throw new RuntimeException(e);
        }
        String archiveDownloadURL = null;
        String releaseNotesURL = null;
        for (final Object archiveAsset : (JSONArray) releaseJson.get("assets")) {
            final JSONObject archiveAssetJson = (JSONObject) archiveAsset;
            if (archiveAssetJson.get("content_type").equals("application/x-tar")) {
                archiveDownloadURL = archiveAssetJson.get("browser_download_url").toString();
            } else if (archiveAssetJson.get("name").equals(Release.RELEASE_NOTES_FILE_NAME)) {
                releaseNotesURL = archiveAssetJson.get("browser_download_url").toString();
            }
        }
        final GithubRelease release = new GithubRelease(name, archiveDownloadURL, releaseNotesURL);
        return new Pair<>(publishedAt, release);
    }

    private static final Pattern nextPagePattern = Pattern.compile(".*<([^<]*)>; rel=\"next\".*");
    String getNextPageURL(String linkHeader) {
        final String result;
        final Matcher m = nextPagePattern.matcher(linkHeader);
        if (m.matches()) {
            result = m.group(1);
        } else {
            result = null;
        }
        return result;
    }
}
