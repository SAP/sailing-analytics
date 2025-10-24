package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.ConcurrentHashBag;
import com.sap.sse.security.util.RemoteServerUtil;
import com.sap.sse.util.HttpUrlConnectionHelper;

@Path(CompareServersResource.V1_COMPARESERVERS)
public class CompareServersResource extends AbstractSailingServerResource {
    public static final String V1_COMPARESERVERS = "/v1/compareservers";

    public static final Logger logger = Logger.getLogger(CompareServersResource.class.getName());

    private static final String LEADERBOARDGROUPSPATH = "/sailingserver/api/v1/leaderboardgroups";
    private static final String LEADERBOARDGROUPSIDENTIFIABLEPATH = LEADERBOARDGROUPSPATH+"/identifiable";
    public static final String SERVER1_FORM_PARAM = "server1";
    public static final String SERVER2_FORM_PARAM = "server2";
    public static final String USER1_FORM_PARAM = "user1";
    public static final String USER2_FORM_PARAM = "user2";
    public static final String PASSWORD1_FORM_PARAM = "password1";
    public static final String PASSWORD2_FORM_PARAM = "password2";
    public static final String BEARER1_FORM_PARAM = "bearer1";
    public static final String BEARER2_FORM_PARAM = "bearer2";
    public static final String LEADERBOARDGROUP_UUID_FORM_PARAM = "leaderboardgroupUUID[]";
    
    /**
     * The list of keys that are compared during a compare run.
     */
    private static final String[] KEYLISTTOCOMPARE = new String[] { LeaderboardGroupConstants.ID,
            LeaderboardGroupConstants.DESCRIPTION, LeaderboardGroupConstants.EVENTS,
            LeaderboardGroupConstants.LEADERBOARDS, LeaderboardGroupConstants.DISPLAYNAME,
            LeaderboardNameConstants.ISMETALEADERBOARD, LeaderboardNameConstants.ISREGATTALEADERBOARD,
            LeaderboardNameConstants.SCORINGCOMMENT, LeaderboardNameConstants.LASTSCORINGUPDATE,
            LeaderboardNameConstants.SCORINGSCHEME, LeaderboardNameConstants.REGATTANAME,
            LeaderboardNameConstants.DISCARDS,
            LeaderboardNameConstants.SERIES, LeaderboardNameConstants.ISMEDALSERIES, LeaderboardNameConstants.FLEETS,
            LeaderboardNameConstants.COLOR, LeaderboardNameConstants.ORDERING, LeaderboardNameConstants.RACES,
            LeaderboardNameConstants.ISMEDALRACE, LeaderboardNameConstants.ISTRACKED,
            LeaderboardNameConstants.REGATTANAME, LeaderboardNameConstants.TRACKEDRACENAME,
            LeaderboardNameConstants.HASGPSDATA, LeaderboardNameConstants.HASWINDDATA,
            LeaderboardGroupConstants.NAME };
    private static final Set<String> KEYSETTOCOMPARE = new HashSet<>(Arrays.asList(KEYLISTTOCOMPARE));
    
    /**
     * The list of keys that are not compared. Represent as a path with ".{fieldname}" for field navigation and
     * "[]" for array expansion. Example: ".leaderboards[].series[].fleets[].races[].raceViewerUrls"
     */
    private static final String[] KEYSTOIGNORE = new String[] { "."+LeaderboardGroupConstants.TIMEPOINT,
            "."+LeaderboardGroupConstants.TIMEPOINT_MILLIS,
            "."+LeaderboardGroupConstants.LEADERBOARDS+"[]."+LeaderboardNameConstants.SERIES+"[]."+LeaderboardNameConstants.FLEETS+"[]."+LeaderboardNameConstants.RACES+"[]."+LeaderboardNameConstants.RACEVIEWERURLS };
    private static final Set<String> KEYSETTOIGNORE = new HashSet<>(Arrays.asList(KEYSTOIGNORE));

    /**
     * The list of keys that get always printed. "name" needs a special treatment, as it should be printed, but also
     * during a compare run there is no need to compare entries with different values for "name".
     */
    private static final String[] KEYSTOPRINT = new String[] { LeaderboardGroupConstants.ID };
    private static final Set<String> KEYSETTOPRINT = new HashSet<>(Arrays.asList(KEYSTOPRINT));

    private static final String SERVERTOOOLD = "At least one server you are trying to compare has not yet enabled the "
            + LEADERBOARDGROUPSIDENTIFIABLEPATH
            + " endpoint and therefore you need to fallback to running the compareServers shell script.";
    
    public CompareServersResource() {
    }
    
    @Context
    UriInfo uriInfo;
    
    /**
     * @param server1
     *            optional; if not provided, the server receiving this request will act as the default for "server1"
     * @param user1
     *            the username for authenticating the request for {@code server1}, together with {@code password1};
     *            alternatively to {@code user1} and {@code password1} clients may specify {@code bearer1}. If none of
     *            these are provided, this request's authentication will be used to obtain a bearer token for
     *            {@code server1}.
     * @param password1
     *            use together with {@code user1} to provide authentication for the requests for {@code server1}
     *            (defaulting to this server).
     * @param bearer1
     *            alternative for {@code user1}/{@code password1}, specifying a bearer token that is used to
     *            authenticate a user on {@code server1} (which defaults to the server handling this request). If
     *            neither of {@code user1}, {@code password1} and {@core bearer1} are provided, this request's
     *            authentication is used to obtain a bearer token which is then used to authenticate the requests for
     *            {@code server1}.
     * @param server2
     *            mandatory; specifies the host name or IP address of the host against which to compare the
     *            {@code server1} content
     * @param user2
     *            the username for authenticating the request for {@code server2}, together with {@code password2};
     *            alternatively to {@code user2} and {@code password2} clients may specify {@code bearer2}. If none of
     *            these are provided, this request's authentication will be used to obtain a bearer token for
     *            {@code server2}, assuming that the server responding to this request and {@code server2} share a
     *            common {@code SecurityService} through replication.
     * @param password2
     *            use together with {@code user2} to provide authentication for the requests for {@code server2}.
     * @param bearer2
     *            alternative for {@code user2}/{@code password2}, specifying a bearer token that is used to
     *            authenticate a user on {@code server2}. If neither of {@code user2}, {@code password2} and
     *            {@core bearer2} are provided, this request's authentication is used to obtain a bearer token which is
     *            then used to authenticate the requests for {@code server1}, assuming that the server responding to
     *            this request and {@code server2} share a common {@code SecurityService} through replication.
     * @param uuidset
     *            can optionally be used to specify a set of UUIDs identifying leaderboard groups to compare. If not
     *            specified (represented as an {@link Set#isEmpty() empty} set), all leaderboard groups found on both,
     *            {@code server1} and {@code server2} will be compared.
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response compareServers(
            @FormParam(SERVER1_FORM_PARAM) String server1, 
            @FormParam(SERVER2_FORM_PARAM) String server2,
            @FormParam(LEADERBOARDGROUP_UUID_FORM_PARAM) Set<String> uuidset,
            @FormParam(USER1_FORM_PARAM) String user1,
            @FormParam(USER2_FORM_PARAM) String user2,
            @FormParam(PASSWORD1_FORM_PARAM) String password1,
            @FormParam(PASSWORD2_FORM_PARAM) String password2,
            @FormParam(BEARER1_FORM_PARAM) String bearer1,
            @FormParam(BEARER2_FORM_PARAM) String bearer2) throws MalformedURLException {
        final Map<String, Set<Object>> result = new HashMap<>();
        Response response = null;
        final String effectiveServer1 = !Util.hasLength(server1) ? uriInfo.getBaseUri().getAuthority() : server1;
        final URL url1 = RemoteServerUtil.createBaseUrl(effectiveServer1);
        final URL url2 = RemoteServerUtil.createBaseUrl(server2);
        if (!SharedLandscapeConstants.isTrustedDomain(url1.getHost())) {
            response = badRequest("Untrusted domain for "+url1);
        } else if (!SharedLandscapeConstants.isTrustedDomain(url2.getHost())) {
            response = badRequest("Untrusted domain for "+url2);
        } else if (!validateParameters(server2, uuidset, user1, user2, password1, password2, bearer1, bearer2)) {
            response = badRequest("Specify two trusted server names and optionally a set of valid leaderboardgroup UUIDs.");
        } else {
            final String token1 = getSecurityService().getOrCreateTargetServerBearerToken(effectiveServer1, user1, password1, bearer1);
            final String token2 = getSecurityService().getOrCreateTargetServerBearerToken(server2, user2, password2, bearer2);
            result.put(effectiveServer1, new HashSet<>());
            result.put(server2, new HashSet<>());
            try {
                if (!uuidset.isEmpty()) {
                    for (String uuid : uuidset) {
                        Pair<Object, Object> jsonPair = fetchLeaderboardgroupDetailsAndRemoveDuplicates(effectiveServer1,
                                server2, uuid, token1, token2);
                        if (jsonPair.getA() != null && jsonPair.getB() != null) {
                            result.get(effectiveServer1).add(jsonPair.getA());
                            result.get(server2).add(jsonPair.getB());
                        }
                    }
                } else {
                    final JSONArray leaderboardgroupList1 = getLeaderboardgroupList(effectiveServer1, token1);
                    final JSONArray leaderboardgroupList2 = getLeaderboardgroupList(server2, token2);
                    for (Object lg1 : leaderboardgroupList1) {
                        if (!leaderboardgroupList2.contains(lg1)) {
                            result.get(effectiveServer1).add(lg1);
                        } else {
                            final String lgId = ((JSONObject) lg1).get(LeaderboardGroupConstants.ID).toString();
                            Pair<Object, Object> jsonPair = fetchLeaderboardgroupDetailsAndRemoveDuplicates(effectiveServer1,
                                    server2, lgId, token1, token2);
                            if (jsonPair.getA() != null && jsonPair.getB() != null) {
                                result.get(effectiveServer1).add(jsonPair.getA());
                                result.get(server2).add(jsonPair.getB());
                            }
                        }
                    }
                    for (Object lg2 : leaderboardgroupList2) {
                        if (!leaderboardgroupList1.contains(lg2)) {
                            result.get(server2).add(lg2);
                        }
                    }
                }
                JSONObject json = new JSONObject();
                for (Entry<String, Set<Object>> entry : result.entrySet()) {
                    json.put(entry.getKey(), entry.getValue());
                }
                if (result.get(effectiveServer1).isEmpty() && result.get(server2).isEmpty()) {
                    response = Response.ok(streamingOutput(json)).build();
                } else {
                    response = Response.status(Status.CONFLICT).entity(streamingOutput(json)).build();
                }
            } catch (FileNotFoundException e) {
                response = Response.status(Status.CONFLICT).entity(e.toString()).build();
                logger.warning(e.toString());
            } catch (ConnectException e) {
                response = Response.status(Status.CONFLICT).entity(e.toString()).build();
                logger.warning(e.toString());
            } catch (Exception e) {
                response = returnInternalServerError(e);
            }
        }
        return response;
    }

    private boolean validateParameters(String server2, Set<String> uuidset, String user1, String user2, String password1,
            String password2, String bearer1, String bearer2) {
        boolean result = validateAuthenticationParameters(user1, password1, bearer1) && validateAuthenticationParameters(user2, password2, bearer2) &&
                    validateParameters(server2, uuidset);
        return result;
    }
    
    /**
     * Validates the {@code uuidset} so that it contains only valid UUIDs that parse properly, and ensures that {@code server2}
     * is set
     */
    private boolean validateParameters(String server2, Set<String> uuidset) {
        boolean result = Util.hasLength(server2);
        for (String uuid : uuidset) {
            if (Util.hasLength(uuid) && !UUID.fromString(uuid).toString().equals(uuid)) {
                result = false;
                break;
            }
        }
        return result;
    }
    
    /**
     * Fetches the details for a given leaderboardgroup UUID and removes all the duplicates in the fields, as well as all
     * fields contained in {@link #KEYSTOIGNORE}.
     */
    private Pair<Object, Object> fetchLeaderboardgroupDetailsAndRemoveDuplicates(String server1, String server2,
            String leaderboardgroupId, String bearer1, String bearer2) throws Exception {
        final JSONObject lgdetail1 = getLeaderboardgroupDetailsById(leaderboardgroupId, RemoteServerUtil.createBaseUrl(server1), bearer1);
        final JSONObject lgdetail2 = getLeaderboardgroupDetailsById(leaderboardgroupId, RemoteServerUtil.createBaseUrl(server2), bearer2);
        final Pair<Object, Object> result = removeUnnecessaryAndDuplicateFields(lgdetail1, lgdetail2);
        return result;
    }

    Pair<Object, Object> removeUnnecessaryAndDuplicateFields(JSONAware lgdetail1, JSONAware lgdetail2) {
        removeUnnecessaryFields(lgdetail1);
        removeUnnecessaryFields(lgdetail2);
        Pair<Object, Object> result = removeDuplicateEntries(lgdetail1, lgdetail2);
        return result;
    }
    /**
     * Fetches the leaderboardgrouplist from a server.
     */
    private JSONArray getLeaderboardgroupList(String server, String bearer) throws Exception {
        final JSONParser parser = new JSONParser();
        final URL baseUrl = RemoteServerUtil.createBaseUrl(server);
        final URLConnection leaderboardgroupListC = HttpUrlConnectionHelper.redirectConnectionWithBearerToken(
                RemoteServerUtil.createRemoteServerUrl(baseUrl, LEADERBOARDGROUPSIDENTIFIABLEPATH, null), bearer);
        if (((HttpURLConnection) leaderboardgroupListC).getResponseCode() == 404) {
            throw new FileNotFoundException(SERVERTOOOLD);
        }
        final JSONArray result = (JSONArray) parser
                .parse(new InputStreamReader(leaderboardgroupListC.getInputStream(), "UTF-8"));
        return result;
    }
    
    /**
     * Fetches the JSON for a given leaderboardgroup UUID.
     */
    private JSONObject getLeaderboardgroupDetailsById(String leaderboardgroupId, URL baseUrl, String bearer) throws Exception {
        final URLConnection lgdetails = HttpUrlConnectionHelper.redirectConnectionWithBearerToken(
                RemoteServerUtil.createRemoteServerUrl(baseUrl, createLgDetailPath(leaderboardgroupId), null), bearer);
        JSONObject result = (JSONObject) JSONValue.parse(new InputStreamReader(lgdetails.getInputStream(), "UTF-8"));
        return result;
    }

    private String createLgDetailPath(String leaderboardgroupId) throws URISyntaxException {
        final String result;
        final StringBuilder lgdetailpath = new StringBuilder(LEADERBOARDGROUPSPATH);
        lgdetailpath.append("/");
        lgdetailpath.append(leaderboardgroupId);
        result = lgdetailpath.toString();
        return result;
    }
    
    /**
     * Call this for the top-level object. It invokes {@link #removeUnnecessaryFields(Object, String)} with an
     * empty path string to start with, removing fields to be ignored or not to be compared in-place, modifying
     * the {@code json} object.
     */
    private void removeUnnecessaryFields(JSONAware json) {
        removeUnnecessaryFields(json, "");
    }
    
    /**
     * Strips a (nested) {@link org.json.simple.JSONObject} from the fields specified in
     * {@link CompareServersResource#KEYSTOIGNORE}, removing fields to be ignored or not to be compared in-place,
     * modifying the {@code json} object.
     * 
     * @param path
     *            the path leading to the {@code json} object; field access is represented by ".{fieldname}" whereas
     *            array access is represented by "[]".
     *
     * @return The modified {@link org.json.simple.JSONObject}.
     */
    private void removeUnnecessaryFields(Object json, String path) {
        if (json instanceof JSONObject) {
            Iterator<Object> iter = ((JSONObject) json).keySet().iterator();
            while (iter.hasNext()) {
                Object key = iter.next();
                final String nextPath = path+"."+key;
                // remove keys that are explicitly to be ignored based on their full path,
                // as well as those that are not to be compared anywhere in the hierarchy
                if (KEYSETTOIGNORE.contains(nextPath) || !KEYSETTOCOMPARE.contains(key)) {
                    iter.remove();
                } else {
                    Object value = ((JSONObject) json).get(key);
                    removeUnnecessaryFields(value, nextPath);
                }
            }
        } else if (json instanceof JSONArray) {
            final String nextPath = path+"[]";
            for (int i = 0; i < ((JSONArray) json).size(); i++) {
                removeUnnecessaryFields(((JSONArray) json).get(i), nextPath);
            }
        }
    }

    /**
     * Takes two (nested) {@link org.json.simple.JSONObject}'s and compares them recursively. They will be modified in
     * place, the keys by which they get compared are listed in {@link CompareServersResource#KEYLISTTOCOMPARE}.
     * 
     * @return the two (nested) {@link org.json.simple.JSONObject}'s, stripped by all fields and values that are equal
     *         for both; {@code (null, null)} in case the two objects passed are equal.
     */
    private Pair<Object, Object> removeDuplicateEntries(Object lg1, Object lg2) {
        final Pair<Object, Object> result;
        if (equalsWithArrayOrderIgnored(lg1, lg2)) {
            result = new Pair<Object, Object>(null, null);
        } else {
            if (lg1 instanceof JSONObject && lg2 instanceof JSONObject) {
                removeDuplicateEntries((JSONObject) lg1, (JSONObject) lg2);
            } else if (lg1 instanceof JSONArray && lg2 instanceof JSONArray) {
                removeDuplicateEntries((JSONArray) lg1, (JSONArray) lg2);
            }
            result = new Pair<Object, Object>(lg1, lg2);
        }
        return result;
    }
    
    /**
     * The two objects {@code a} and {@code b} are considered equal if they are the same, or if {@code a.equals(b)},
     * or if {@code a} and {@code b} are both of type {@link JSONArray} and their elements, when thrown into a "Bag"
     * (ignoring order but keeping track of the count of equal objects), lead to two bags comparing equals, or if
     * {@code a} and {@code b} are both of type {@link JSONObject} and the values for all equal keys pass this test.<p>
     * 
     * Note that this works recursively along {@link JSONArray} and {@link JSONObject} hierarchies.
     */
    boolean equalsWithArrayOrderIgnored(Object a, Object b) {
        final boolean result;
        if (a == b || Util.equalsWithNull(a, b)) {
            result = true;
        } else {
            if (a instanceof JSONArray && b instanceof JSONArray) {
                final ConcurrentHashBag<Object> aBag = new ConcurrentHashBag<>();
                for (final Object aObject : (JSONArray) a) {
                    aBag.add(aObject);
                }
                final ConcurrentHashBag<Object> bBag = new ConcurrentHashBag<>();
                for (final Object bObject : (JSONArray) b) {
                    bBag.add(bObject);
                }
                result = aBag.equals(bBag);
            } else if (a instanceof JSONObject && b instanceof JSONObject) {
                if (((JSONObject) a).keySet().equals(((JSONObject) b).keySet())) {
                    boolean allValuesRecursivelyEqual = true;
                    for (final Object aKey : ((JSONObject) a).keySet()) {
                        if (!equalsWithArrayOrderIgnored(((JSONObject) a).get(aKey), ((JSONObject) b).get(aKey))) {
                            allValuesRecursivelyEqual = false;
                            break;
                        }
                    }
                    result = allValuesRecursivelyEqual;
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        }
        return result;
    }
    
    private void removeDuplicateEntries(JSONObject jsonObject1, JSONObject jsonObject2) {
        // TODO note that we assume that the LeaderboardGroupConstants.NAME attribute name is used throughout the hierarchy to identify object "names" which is used as key during object comparison
        if ((!jsonObject1.containsKey(LeaderboardGroupConstants.NAME) && !jsonObject2.containsKey(LeaderboardGroupConstants.NAME))
                || Util.equalsWithNull(jsonObject1.get(LeaderboardGroupConstants.NAME), jsonObject2.get(LeaderboardGroupConstants.NAME))) {
            final Iterator<Object> iter1 = jsonObject1.keySet().iterator();
            while (iter1.hasNext()) {
                Object key = iter1.next();
                if (jsonObject2.containsKey(key)) {
                    Object value1 = jsonObject1.get(key);
                    Object value2 = jsonObject2.get(key);
                    if (KEYSETTOPRINT.contains(key) && equalsWithArrayOrderIgnored(value1, value2)) {
                        continue;
                    } else if (!key.equals(LeaderboardGroupConstants.NAME) &&
                            equalsWithArrayOrderIgnored(value1, value2) && KEYSETTOCOMPARE.contains(key)) {
                        // keys which are to be compared and whose values are equal are removed from both sides;
                        // never remove the "name" key if present because it helps identifying the differing objects
                        iter1.remove();
                        jsonObject2.remove(key);
                    } else {
                        removeDuplicateEntries(value1, value2);
                    }
                }
            }
        }
    }

    private void removeDuplicateEntries(JSONArray json1, JSONArray json2) {
        if (equalsWithArrayOrderIgnored(json1, json2)) {
            json1.clear();
            json2.clear();
        } else {
            final Map<String, JSONObject> jsonObjects1KeyedByName = getJsonObjectsByName(json1);
            final Map<String, JSONObject> jsonObjects2KeyedByName = getJsonObjectsByName(json2);
            int i = 0;
            while (i < json1.size()) {
                if (jsonObjects1KeyedByName == null || jsonObjects2KeyedByName == null) {
                    // no consistent "name" field for all objects, or no JSONObject content; compare positionally
                    if (i < json2.size()) {
                        if (equalsWithArrayOrderIgnored(json1.get(i), json2.get(i))) {
                            json1.remove(i);
                            json2.remove(i);
                        } else {
                            removeDuplicateEntries(json1.get(i), json2.get(i));
                            i++;
                        }
                    } else {
                        i++; // could also use break, but break is so unstructured...
                    }
                } else {
                    // content is JSONObject only, consistently providing a "name" field for all objects; compare by name:
                    final JSONObject item1 = (JSONObject) json1.get(i);
                    final String name = item1.get(LeaderboardGroupConstants.NAME).toString();
                    final JSONObject item2 = jsonObjects2KeyedByName.get(name);
                    if (equalsWithArrayOrderIgnored(item1, item2)) {
                        // if an equal element is found in json2, remove item from both arrays
                        json1.remove(i);
                        json2.remove(item2);
                    } else {
                        if (item1 != null && item2 != null) {
                            removeDuplicateEntries(item1, item2);
                        }
                        i++;
                    }
                }
            }
        }
    }

    /**
     * @return a map in case {@code jsonArray} contains only {@link JSONObject}s that all have a {@code "name"}
     * field, mapping the {@link JSONObject}s by the value of that {@code "name"} field; {@code null} otherwise.
     */
    private Map<String, JSONObject> getJsonObjectsByName(JSONArray jsonArray) {
        final Map<String, JSONObject> result = new HashMap<>();
        for (final Object o : jsonArray) {
            if (o instanceof JSONObject && ((JSONObject) o).containsKey(LeaderboardGroupConstants.NAME)) {
                result.put(((JSONObject) o).get(LeaderboardGroupConstants.NAME).toString(), ((JSONObject) o));
            }
        }
        return result.size() == jsonArray.size() ? result : null;
    }
}
