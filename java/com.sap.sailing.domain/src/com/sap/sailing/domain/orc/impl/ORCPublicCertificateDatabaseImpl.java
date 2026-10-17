package com.sap.sailing.domain.orc.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.http.HttpMessage;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.orc.ORCCertificate;
import com.sap.sailing.domain.orc.ORCPublicCertificateDatabase;
import com.sap.sse.common.CountryCode;
import com.sap.sse.common.CountryCodeFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class ORCPublicCertificateDatabaseImpl implements ORCPublicCertificateDatabase {
    private static final Logger logger = Logger.getLogger(ORCPublicCertificateDatabaseImpl.class.getName());
    private static final String USER_NAME = "Sailing_Analytics@sap.com";
    private static final String CREDENTIALS = "QUdIVUJU";

    private static final String SEARCH_URL = "https://data.orc.org/public/WPub.dll";
    private static final String SINGLE_CERTIFICATE_DOWNLOAD_URL = SEARCH_URL+"?action=DownBoatRMS";
    private static final String BULK_CERTIFICATES_DOWNLOAD_URL = SEARCH_URL+"?action=DownRMS";
    private static final String EXT_JSON = "ext=json";
    // The legacy XML country-overview action ({@code SEARCH_URL+"/RMS"}) still responds, but the country list is now
    // read from the JSON {@code DownRMS} action's top-level {@code Countries} array to keep a single JSON transport.
    private static final String RMS_ARRAY_KEY = "rms";
    private static final String COUNTRIES_ARRAY_KEY = "Countries";
    private static final String COUNTRY_ID_KEY = "CountryId";
    private static final String COUNTRY_NAME_KEY = "Name";
    private static final String ORC_PSEUDO_COUNTRY_ID = "ORC";

    private static final String KEY_REF_NO = "RefNo";
    private static final String KEY_NAT_AUTH = "NatAuth";
    private static final String KEY_BIN = "BIN";
    private static final String KEY_GPH = "GPH";
    private static final String KEY_YACHT_NAME = "YachtName";
    private static final String KEY_SAIL_NO = "SailNo";
    private static final String KEY_CLASS = "Class";
    private static final String KEY_DESIGNER = "Designer";
    private static final String KEY_BUILDER = "Builder";
    private static final String KEY_AGE_YEAR = "Age_Year";
    private static final String KEY_ISSUE_DATE = "IssueDate";
    private static final String KEY_FAMILY = "Family";

    private static final DateTimeFormatter[] DATE_FORMATTERS = 
            new DateTimeFormatter[] {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    };
    private static final DateTimeFormatter[] DATE_FORMATTERS_WITH_ZONE = 
            new DateTimeFormatter[] {
                    DateTimeFormatter.ISO_INSTANT,
                    DateTimeFormatter.ISO_DATE_TIME,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    };
    
    private static class CountryOverviewImpl implements CountryOverview {
        private final CountryCode issuingCountry;
        private final CertificateFamily family;
        private final Integer certType;
        private final Integer vppYear;
        private final int certCount;
        private final TimePoint lastUpdate;
        private final String certName;
        private final String rmsCode;
        
        protected CountryOverviewImpl(CountryCode issuingCountry, CertificateFamily family, Integer certType,
                Integer vppYear, int certCount, TimePoint lastUpdate, String certName, String rmsCode) {
            super();
            this.issuingCountry = issuingCountry;
            this.family = family;
            this.certType = certType;
            this.vppYear = vppYear;
            this.certCount = certCount;
            this.lastUpdate = lastUpdate;
            this.certName = certName;
            this.rmsCode = rmsCode;
        }
        @Override
        public CountryCode getIssuingCountry() {
            return issuingCountry;
        }
        @Override
        public CertificateFamily getFamily() {
            return family;
        }
        @Override
        public Integer getCertType() {
            return certType;
        }
        @Override
        public Integer getVPPYear() {
            return vppYear;
        }
        @Override
        public int getCertCount() {
            return certCount;
        }
        @Override
        public TimePoint getLastUpdate() {
            return lastUpdate;
        }
        @Override
        public String getCertName() {
            return certName;
        }
        @Override
        public String getRMSCode() {
            return rmsCode;
        }
    }
    
    /**
     * Hash code and equality as based on {@link #getReferenceNumber() the reference number field} only.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private static class CertificateHandleImpl implements CertificateHandle {
        private final CountryCode issuingCountry;
        private final String fileId;
        private final String sssid;
        private final Double gph;
        private final UUID datInGID;
        private final String referenceNumber;
        private final CertificateFamily family;
        private final String yachtName;
        private final String sailNumber;
        private final String boatClassName;
        private final String designer;
        private final String builder;
        private final Integer yearBuilt;
        private final TimePoint issueDate;
        private final Integer certType;
        private final Boolean isOneDesign;
        private final Boolean isProvisional;
        private final Boolean isValid;

        public CertificateHandleImpl(CountryCode issuingCountry, String fileId, Double gph, String sssid,
                UUID datInGID, String referenceNumber, CertificateFamily family, String yachtName, String sailNumber,
                String boatClassName, String designer, String builder, Integer yearBuilt, TimePoint issueDate,
                Integer certType, Boolean oneDesign, Boolean isProvisional, Boolean isValid) {
            super();
            this.issuingCountry = issuingCountry;
            this.fileId = fileId;
            this.gph = gph;
            this.sssid = sssid;
            this.datInGID = datInGID;
            this.referenceNumber = referenceNumber;
            this.family = family;
            this.yachtName = yachtName;
            this.sailNumber = sailNumber;
            this.boatClassName = boatClassName;
            this.designer = designer;
            this.builder = builder;
            this.yearBuilt = yearBuilt;
            this.issueDate = issueDate;
            this.certType = certType;
            this.isOneDesign = oneDesign;
            this.isProvisional = isProvisional;
            this.isValid = isValid;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((referenceNumber == null) ? 0 : referenceNumber.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CertificateHandleImpl other = (CertificateHandleImpl) obj;
            if (referenceNumber == null) {
                if (other.referenceNumber != null)
                    return false;
            } else if (!referenceNumber.equals(other.referenceNumber))
                return false;
            return true;
        }

        @Override
        public CountryCode getIssuingCountry() {
            return issuingCountry;
        }

        @Override
        public String getFileId() {
            return fileId;
        }

        @Override
        public Double getGPH() {
            return gph;
        }

        @Override
        public String getSSSID() {
            return sssid;
        }

        @Override
        public UUID getDatInGID() {
            return datInGID;
        }

        @Override
        public String getReferenceNumber() {
            return referenceNumber;
        }

        @Override
        public CertificateFamily getFamily() {
            return family;
        }

        @Override
        public String getYachtName() {
            return yachtName;
        }

        @Override
        public String getSailNumber() {
            return sailNumber;
        }

        @Override
        public String getBoatClassName() {
            return boatClassName;
        }

        @Override
        public String getDesigner() {
            return designer;
        }

        @Override
        public String getBuilder() {
            return builder;
        }

        @Override
        public Integer getYearBuilt() {
            return yearBuilt;
        }

        @Override
        public TimePoint getIssueDate() {
            return issueDate;
        }
        
        @Override
        public Integer getCertType() {
            return certType;
        }

        @Override
        public Boolean isProvisional() {
            return isProvisional;
        }
        
        @Override
        public Boolean isOd() {
            return isOneDesign;
        }
        
        @Override
        public Boolean isValid() {
            return isValid;
        }

        @Override
        public String toString() {
            return "CertificateHandleImpl [issuingCountry=" + issuingCountry + ", sssid=" + sssid + ", gph=" + gph
                    + ", datInGID=" + datInGID + ", referenceNumber=" + referenceNumber + ", yachtName=" + yachtName
                    + ", sailNumber=" + sailNumber + ", boatClassName=" + boatClassName + ", designer=" + designer
                    + ", builder=" + builder + ", yearBuilt=" + yearBuilt + ", issueDate=" + issueDate + ", certType="
                    + certType + ", isOneDesign=" + isOneDesign + ", isProvisional=" + isProvisional +
                    ", isValid=" + isValid + "]";
        }
    }

    /**
     * Performs an authenticated HTTP GET against {@code url} and parses the response body as a JSON object. The ORC
     * {@code WPub.dll} JSON responses are prefixed with a byte-order mark, so the stream is wrapped in a
     * {@link BOMInputStream} and decoded with the detected charset (UTF-8 otherwise), mirroring the BOM handling in
     * {@link AbstractORCCertificatesImporter} which this class cannot reuse directly. Any
     * {@link org.json.simple.parser.ParseException} is wrapped in an {@link IOException} so that no {@code org.json}
     * type leaks into the signatures of the calling methods.
     */
    private JSONObject httpGetJson(final String url) throws IOException {
        final HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        final HttpGet getRequest = new HttpGet(url);
        addAuthorizationHeader(getRequest);
        logger.fine(()->"Fetching ORC JSON from "+url);
        final InputStream content = client.execute(getRequest).getEntity().getContent();
        final BOMInputStream bomInputStream = new BOMInputStream(content);
        final ByteOrderMark bom = bomInputStream.getBOM();
        final String charsetName = bom == null ? "UTF-8" : bom.getCharsetName();
        final Reader reader = new InputStreamReader(new BufferedInputStream(bomInputStream), charsetName);
        final JSONObject root;
        try {
            root = (JSONObject) new JSONParser().parse(reader);
        } catch (final org.json.simple.parser.ParseException e) {
            throw new IOException("Could not parse ORC JSON response from "+url, e);
        }
        return root;
    }

    /**
     * Maps one {@code rms} row of the ORC {@code WPub.dll} JSON response to a {@link CertificateHandle}. The JSON keys
     * differ from the historic XML element names: {@code NatAuth} carries the IOC country code (formerly
     * {@code CountryId}), {@code BIN} serves as the {@link CertificateHandle#getFileId() file id} (formerly
     * {@code dxtName}), {@code IssueDate} carries the issue instant (formerly {@code dxtDate}) and {@code Age_Year}
     * carries the build year (formerly {@code Age}). The JSON action only ever returns finalized, valid certificates,
     * so {@link CertificateHandle#isValid()} is always {@link Boolean#TRUE} and
     * {@link CertificateHandle#isProvisional()} is {@link Boolean#FALSE}. There is no JSON equivalent for the former
     * {@code SSSID}, {@code DatInGID}, {@code CertType} and {@code IsOd} fields, so those are left {@code null}.
     */
    private CertificateHandle parseHandleFromJson(final JSONObject row) {
        final String natAuth = asTrimmedString(row.get(KEY_NAT_AUTH));
        final CountryCode issuingCountry = natAuth == null ? null
                : CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(natAuth);
        final String fileId = asTrimmedString(row.get(KEY_BIN));
        final Double gph = asDouble(row.get(KEY_GPH));
        final String referenceNumber = asTrimmedString(row.get(KEY_REF_NO));
        final CertificateFamily family = familyFromString(asTrimmedString(row.get(KEY_FAMILY)));
        final String yachtName = asString(row.get(KEY_YACHT_NAME));
        final String sailNumber = asString(row.get(KEY_SAIL_NO));
        final String boatClassName = asString(row.get(KEY_CLASS));
        final String designer = asString(row.get(KEY_DESIGNER));
        final String builder = asString(row.get(KEY_BUILDER));
        final Integer yearBuilt = asInteger(row.get(KEY_AGE_YEAR));
        final String issueDateString = asTrimmedString(row.get(KEY_ISSUE_DATE));
        final TimePoint issueDate = issueDateString == null ? null
                : new MillisecondsTimePoint(parseDate(issueDateString));
        return new CertificateHandleImpl(issuingCountry, fileId, gph, /* sssid */ null, /* datInGID */ null,
                referenceNumber, family, yachtName, sailNumber, boatClassName, designer, builder, yearBuilt, issueDate,
                /* certType */ null, /* oneDesign */ null, /* isProvisional */ Boolean.FALSE, /* isValid */ Boolean.TRUE);
    }

    private static String asString(final Object value) {
        return value == null ? null : value.toString();
    }

    private static String asTrimmedString(final Object value) {
        final String asString = asString(value);
        final String trimmed = asString == null ? null : asString.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static Double asDouble(final Object value) {
        final Double result;
        if (value == null) {
            result = null;
        } else if (value instanceof Number) {
            result = ((Number) value).doubleValue();
        } else {
            Double parsed;
            try {
                parsed = Double.valueOf(value.toString().trim());
            } catch (final NumberFormatException e) {
                parsed = null;
            }
            result = parsed;
        }
        return result;
    }

    private static Integer asInteger(final Object value) {
        final Integer result;
        if (value == null) {
            result = null;
        } else if (value instanceof Number) {
            result = ((Number) value).intValue();
        } else {
            Integer parsed;
            try {
                parsed = Integer.valueOf(value.toString().trim());
            } catch (final NumberFormatException e) {
                parsed = null;
            }
            result = parsed;
        }
        return result;
    }

    /**
     * Resolves the JSON {@code Family} string (e.g. {@code "ORC"}) to a {@link CertificateFamily} by matching it
     * case-insensitively against each family's {@link CertificateFamily#getFamilyQueryParamValue()}. Defaults to
     * {@link CertificateFamily#ORC} when {@code familyString} is {@code null} or unrecognized, because the JSON actions
     * only return standard ORC certificates today.
     */
    private static CertificateFamily familyFromString(final String familyString) {
        CertificateFamily result = CertificateFamily.ORC;
        if (familyString != null) {
            for (final CertificateFamily candidate : CertificateFamily.values()) {
                if (candidate.getFamilyQueryParamValue().equalsIgnoreCase(familyString)) {
                    result = candidate;
                }
            }
        }
        return result;
    }

    @Override
    public Iterable<CertificateHandle> search(final CountryCode issuingCountry, final Integer yearOfIssuance,
            final String referenceNumber, final String yachtName, final String sailNumber, final String boatClassName,
            final boolean includeInvalid) throws Exception {
        // includeInvalid is retained for API compatibility; the JSON actions only ever return valid certificates, so
        // every handle produced here is valid and the flag has no effect.
        final Set<ORCPublicCertificateDatabase.CertificateHandle> result = new HashSet<>();
        final String trimmedReferenceNumber = referenceNumber == null ? null : referenceNumber.trim();
        if (trimmedReferenceNumber != null && !trimmedReferenceNumber.isEmpty()) {
            final JSONObject root = httpGetJson(SINGLE_CERTIFICATE_DOWNLOAD_URL+"&"+EXT_JSON+"&"+KEY_REF_NO+"="+trimmedReferenceNumber);
            addMatchingHandles(root, result, yachtName, sailNumber, boatClassName, yearOfIssuance);
        } else if (issuingCountry != null) {
            final JSONObject root = httpGetJson(BULK_CERTIFICATES_DOWNLOAD_URL+"&"+COUNTRY_ID_KEY+"="+issuingCountry.getThreeLetterIOCCode()+"&"+EXT_JSON);
            addMatchingHandles(root, result, yachtName, sailNumber, boatClassName, yearOfIssuance);
        } else {
            logger.warning("ORC JSON search requires either a reference number or an issuing country; neither was "+
                    "given, returning no results (yachtName="+yachtName+", sailNumber="+sailNumber+", boatClassName="+
                    boatClassName+")");
        }
        logger.fine(()->"ORC search returned "+result.size()+" results");
        return result;
    }

    private void addMatchingHandles(final JSONObject root, final Set<CertificateHandle> result, final String yachtName,
            final String sailNumber, final String boatClassName, final Integer yearOfIssuance) {
        final JSONArray rows = (JSONArray) root.get(RMS_ARRAY_KEY);
        if (rows != null) {
            for (final Object rowObject : rows) {
                final CertificateHandle handle = parseHandleFromJson((JSONObject) rowObject);
                if (matchesClientFilters(handle, yachtName, sailNumber, boatClassName, yearOfIssuance)) {
                    result.add(handle);
                }
            }
        }
    }

    /**
     * Applies the optional client-side restrictions to a {@link CertificateHandle} obtained from the JSON actions. A
     * {@code null} criterion imposes no restriction. The {@code sailNumber} comparison normalizes both sides to their
     * alphanumeric core (uppercased) so that {@code "DEN 13"}, {@code "DEN-13"}, {@code "DEN13"} and the
     * {@code %}-bearing fuzzy variants all collapse to a comparable form. The {@code yearOfIssuance} is compared
     * against the year of the handle's {@link CertificateHandle#getIssueDate() issue date}; note that the JSON actions
     * only return currently valid certificates, so a historical year effectively matches nothing and year search is
     * limited to the current issuance year.
     */
    private static boolean matchesClientFilters(final CertificateHandle handle, final String yachtName,
            final String sailNumber, final String boatClassName, final Integer yearOfIssuance) {
        boolean match = true;
        if (yachtName != null) {
            final String normalizedFilter = normalizeContains(yachtName);
            match = match && handle.getYachtName() != null
                    && handle.getYachtName().toLowerCase().contains(normalizedFilter);
        }
        if (boatClassName != null) {
            final String normalizedFilter = normalizeContains(boatClassName);
            match = match && handle.getBoatClassName() != null
                    && handle.getBoatClassName().toLowerCase().contains(normalizedFilter);
        }
        if (sailNumber != null) {
            final String normalizedFilter = normalizeSailNumber(sailNumber);
            match = match && handle.getSailNumber() != null
                    && normalizeSailNumber(handle.getSailNumber()).contains(normalizedFilter);
        }
        if (yearOfIssuance != null) {
            match = match && handle.getIssueDate() != null
                    && Instant.ofEpochMilli(handle.getIssueDate().asMillis()).atOffset(ZoneOffset.UTC)
                            .getYear() == yearOfIssuance.intValue();
        }
        return match;
    }

    private static String normalizeContains(final String value) {
        return value.replace("%", "").toLowerCase().trim();
    }

    private static String normalizeSailNumber(final String sailNumber) {
        return sailNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    @Override
    public Iterable<CountryOverview> getCountriesWithValidCertificates() throws IOException {
        // Read the country list from the JSON DownRMS action's top-level "Countries" array (the legacy /RMS XML action
        // remains available as a fallback). The JSON does not carry a VPP year or certificate counts, so those are left
        // unset; the first pseudo-entry "ORC" ("Offshore Racing Congress") is skipped.
        final Set<ORCPublicCertificateDatabase.CountryOverview> result = new HashSet<>();
        final JSONObject root = httpGetJson(BULK_CERTIFICATES_DOWNLOAD_URL+"&"+EXT_JSON);
        final JSONArray countries = (JSONArray) root.get(COUNTRIES_ARRAY_KEY);
        if (countries != null) {
            for (final Object countryObject : countries) {
                final JSONObject country = (JSONObject) countryObject;
                final String countryId = asTrimmedString(country.get(COUNTRY_ID_KEY));
                final String countryName = asString(country.get(COUNTRY_NAME_KEY));
                if (countryId != null && !ORC_PSEUDO_COUNTRY_ID.equalsIgnoreCase(countryId)) {
                    final CountryCode issuingCountry = CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(countryId);
                    result.add(new CountryOverviewImpl(issuingCountry, CertificateFamily.ORC, /* certType */ null,
                            /* vppYear */ null, /* certCount */ 0, /* lastUpdate */ null, countryName, countryId));
                }
            }
        }
        logger.fine(()->"ORC country overview returned "+result.size()+" countries");
        return result;
    }

    private void addAuthorizationHeader(final HttpMessage httpMessage) {
        httpMessage.addHeader("Authorization", "Basic "+new String(Base64.getEncoder().encode((USER_NAME+":"+getDecodedCredentials()).getBytes())));
    }

    @Override
    public Date parseDate(final String dateString) throws DateTimeParseException {
        Optional<LocalDateTime> parsedDateWithoutZone = Arrays.asList(DATE_FORMATTERS).stream()
                .map(f -> {
                    try {
                        return LocalDateTime.parse(dateString, f);
                    } catch (DateTimeParseException e) {
                        return null;
                    }
                })
                .filter(out -> out != null)
                .findAny();
        Optional<ZonedDateTime> parsedDateWithZone = Arrays.asList(DATE_FORMATTERS_WITH_ZONE).stream()
                .map(f -> {
                    try {
                        return ZonedDateTime.parse(dateString, f);
                    } catch (DateTimeParseException e) {
                        return null;
                    }
                })
                .filter(out -> out != null)
                .findAny();
        if (parsedDateWithoutZone.isPresent()) {
            LocalDateTime localDateTime = parsedDateWithoutZone.get();
            return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
        }
        if (parsedDateWithZone.isPresent()) {
            ZonedDateTime zonedDateTime = parsedDateWithZone.get();
            return Date.from(zonedDateTime.toInstant());
        }
        logger.fine("Date is not parsable by any of the format :" + dateString);
        throw new DateTimeParseException("Date is not parsable by any of the format", dateString, 0);
    }

    @Override
    public ORCCertificate searchForUpdate(CertificateHandle certificateHandle)
            throws ClientProtocolException, IOException, org.json.simple.parser.ParseException {
        return searchForUpdate(certificateHandle.getReferenceNumber());
    }

    @Override
    public ORCCertificate searchForUpdate(ORCCertificate certificate)
            throws ClientProtocolException, IOException, org.json.simple.parser.ParseException {
        return searchForUpdate(certificate.getReferenceNumber());
    }

    private ORCCertificate searchForUpdate(final String existingCertificateRefNo)
            throws IOException, org.json.simple.parser.ParseException, ClientProtocolException {
        logger.fine("Looking for update of certificate with reference number "+existingCertificateRefNo);
        final String queryParameters = EXT_JSON+"&"+KEY_REF_NO+"="+existingCertificateRefNo;
        final ORCCertificate result = getSingleCertificate(queryParameters);
        if (result == null) {
            logger.info("Couldn't find any valid certificate for that boat");
        } else {
            if (result.getReferenceNumber().equals(existingCertificateRefNo)) {
                logger.info("Found no new certificate; the one with reference number "+existingCertificateRefNo+
                        " is still valid.");
            } else {
                logger.info("Found update for "+existingCertificateRefNo+": "+result);
            }
        }
        return result;
    }

    @Override
    public ORCCertificate getCertificate(String referenceNumber, CertificateFamily family) throws Exception {
        final String queryParameters = "ext=json&RefNo="+referenceNumber+"&family="+family.getFamilyQueryParamValue();
        logger.fine("Obtaining certificate for reference number "+referenceNumber);
        ORCCertificate result;
        try {
            result = getSingleCertificate(queryParameters);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error trying to find ORC certificate with reference number "+referenceNumber, e);
            result = null;
        }
        if (result == null) {
            logger.info("Couldn't find ORC certificate with reference number "+referenceNumber);
        }
        return result;
    }

    private ORCCertificate getSingleCertificate(final String queryParameters)
            throws IOException, org.json.simple.parser.ParseException, ClientProtocolException {
        final HttpGet getRequest = new HttpGet(SINGLE_CERTIFICATE_DOWNLOAD_URL+"&"+queryParameters);
        final HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        addAuthorizationHeader(getRequest);
        final Iterable<ORCCertificate> certificates = new ORCCertificatesJsonImporter().read(client.execute(getRequest).getEntity().getContent())
                .getCertificates();
        final ORCCertificate result;
        if (certificates.iterator().hasNext()) {
            result = certificates.iterator().next();
        } else {
            result = null;
        }
        return result;
    }

    public Iterable<ORCCertificate> getCertificates(Iterable<CertificateHandle> handles) throws Exception {
        final Map<CertificateHandle, Future<ORCCertificate>> futures = new HashMap<>();
        for (final CertificateHandle handle : handles) {
            if (!handle.getReferenceNumber().trim().isEmpty()) {
                final FutureTask<ORCCertificate> task = new FutureTask<ORCCertificate>(()->getCertificate(handle.getReferenceNumber(), handle.getFamily()));
                final Thread backgroundExecutor = new Thread(task, "ORC certificate background download thread for "+handle.getReferenceNumber());
                backgroundExecutor.setDaemon(true);
                backgroundExecutor.start();
                futures.put(handle, task);
            } else {
                logger.fine("Ignoring handle "+handle+" because it has an empty reference number");
            }
        }
        final Set<ORCCertificate> result = new HashSet<>();
        for (final Entry<CertificateHandle, Future<ORCCertificate>> e : futures.entrySet()) {
            final ORCCertificate certificate = e.getValue().get();
            if (certificate != null) {
                logger.fine("Found certificate for handle "+e.getKey());
                result.add(certificate);
            } else {
                logger.fine("Did not find certificate for handle "+e.getKey());
            }
        }
        return result;
    }

    @Override
    public Future<Set<ORCCertificate>> search(final String yachtName, final String sailNumber, final BoatClass boatClass) {
        logger.fine(()->"Looking for ORC certificate for "+yachtName+"/"+sailNumber+"/"+boatClass);
        final FutureTask<Set<ORCCertificate>> result = new FutureTask<Set<ORCCertificate>>(()->{
            final Set<ORCCertificate> certificates = new HashSet<>();
            Iterable<CertificateHandle> certificateHandles = fuzzySearchVaryingSailNumberPadding(yachtName, sailNumber, boatClass);
            if (!containsValidHandle(certificateHandles)) {
                // try swapping yacht name and sail number and go again:
                logger.fine(()->"Nothing found for "+yachtName+"/"+sailNumber+"/"+boatClass+
                        "; trying by swapping sail number and yacht name");
                certificateHandles = fuzzySearchVaryingSailNumberPadding(sailNumber, yachtName, boatClass);
            }
            for (final CertificateHandle handle : certificateHandles) {
                final Iterable<ORCCertificate> certificatesFromHandle = getCertificates(handle);
                if (certificatesFromHandle.iterator().hasNext()) {
                    final ORCCertificate certificate = certificatesFromHandle.iterator().next();
                    if (certificate != null) {
                        certificates.add(certificate);
                    }
                }
            }
            return certificates;
        });
        final Thread backgroundExecutor = new Thread(result, "ORC certificate background lookup thread for "+yachtName+"/"+sailNumber+"/"+boatClass);
        backgroundExecutor.setDaemon(true);
        backgroundExecutor.start();
        return result;
    }

    private Iterable<CertificateHandle> fuzzySearchVaryingSailNumberPadding(final String yachtName, final String sailNumber, final BoatClass boatClass) throws Exception {
        final Set<CertificateHandle> result = new HashSet<>();
        for (final String sailNumberVariant : getSailNumberVariants(sailNumber)) {
            logger.fine(()->"Trying sail number variation "+sailNumberVariant);
            final Iterable<CertificateHandle> certificateHandles = fuzzySearchVaryingBoatClassName(yachtName, sailNumberVariant, boatClass);
            Util.addAll(filterValidHandles(certificateHandles), result);
        }
        if (sailNumber != null && result.isEmpty() && (yachtName != null || boatClass != null)) {
            logger.fine(()->"Nothing found; trying without restricting sail number to "+sailNumber);
            // try without sail number constraint; if that doesn't find anything either, we can stop
            Util.addAll(fuzzySearchVaryingBoatClassName(yachtName, /* sailNumber */ null, boatClass), result);
        }
        return result;
    }

    private boolean containsValidHandle(Iterable<CertificateHandle> certificateHandles) {
        return filterValidHandles(certificateHandles).iterator().hasNext();
    }

    private Iterable<CertificateHandle> filterValidHandles(Iterable<CertificateHandle> certificateHandles) {
        return Util.filter(certificateHandles, handle->handle.isValid());
    }

    /**
     * Varies the boat class name by first using the true boat class name, then, if nothing is found, stepping through
     * the alternative names, and finally removing the boat class name constraint altogether. Because the JSON bulk
     * action needs an issuing country to return rows, the nationality is derived from the {@code sailNumber} prefix via
     * {@link #countryFromSailNumber(String)}; a sail number without a resolvable three-letter IOC prefix will therefore
     * yield no results.
     * 
     * @param yachtName
     *            may be {@code null}
     * @param sailNumber
     *            may be {@code null}
     * @param boatClass
     *            may be {@code null}
     * 
     * @return the boat class name that ultimately led to the matches returned, and the matches in the form of a
     *         sequence of handles; may be {@code null}
     */
    private Iterable<CertificateHandle> fuzzySearchVaryingBoatClassName(final String yachtName, final String sailNumber, final BoatClass boatClass) throws Exception {
        final CountryCode issuingCountry = countryFromSailNumber(sailNumber);
        String successfulBoatClassName = boatClass==null?null:boatClass.getName();
        Iterable<CertificateHandle> certificateHandles = search(issuingCountry, /* yearOfIssuance */ null, /* referenceNumber */ null, yachtName, sailNumber, successfulBoatClassName, /* includeInvalid */ false);
        if (!containsValidHandle(certificateHandles) && successfulBoatClassName != null) {
            if (yachtName != null || sailNumber != null) {
                logger.fine(()->"Nothing found; removing boat class name restriction "+boatClass.getName());
                // try without boat class restriction
                successfulBoatClassName = null;
                certificateHandles = search(issuingCountry, /* yearOfIssuance */ null, /* referenceNumber */ null, yachtName, sailNumber, successfulBoatClassName, /* includeInvalid */ false);
            } else {
                logger.fine(()->"No current certificates found for boat class "+boatClass.getName()+
                        " but yacht name and sail number are not specified either; giving up.");
            }
        }
        // if a valid boatClass was specified, try to filter; if none match the filter, return unfiltered
        if (boatClass != null) {
            final Set<CertificateHandle> restrictedResults = new HashSet<>();
            for (final CertificateHandle handle : filterValidHandles(certificateHandles)) {
                final BoatClassMasterdata boatClassMasterData = BoatClassMasterdata.resolveBoatClass(handle.getBoatClassName());
                if (boatClassMasterData != null && boatClassMasterData.getDisplayName().equals(boatClass.getName())) {
                    restrictedResults.add(handle);
                }
            }
            if (!restrictedResults.isEmpty()) {
                certificateHandles = restrictedResults;
            }
        }
        return certificateHandles;
    }

    /**
     * Extracts the leading three-letter nationality prefix from a sail number (e.g. {@code "GER"} from
     * {@code "GER 5711"}) and resolves it to a {@link CountryCode} via its IOC code, mirroring the prefix extraction in
     * {@link #getSailNumberVariants(String)}. Returns {@code null} when {@code sailNumber} is {@code null} or has no
     * resolvable three-letter prefix.
     */
    private static CountryCode countryFromSailNumber(final String sailNumber) {
        CountryCode result = null;
        if (sailNumber != null) {
            final Matcher matcher = Pattern.compile("[^A-Za-z]*([A-Za-z]*).*([0-9]*).*").matcher(sailNumber);
            if (matcher.find()) {
                final String country = matcher.group(1);
                if (country != null && country.length() == 3) {
                    result = CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(country.toUpperCase());
                }
            }
        }
        return result;
    }

    /**
     * Produces a sequence of sail number variations of which {@code sailNumber} is the first.
     * It then adds separations of a recognized three-letter IOC nationality code at the beginning
     * of {@code sailNumber} and a digit sequence at the end and connects them with combinations
     * of space and dash (-) characters surrounded by the wildcard character "%". This can lead
     * to slightly unprecise results: e.g., "DEN%-%13" can also match "DEN-413". To be fair, the
     * original search also is a prefix search, so "DEN 13" would also match "DEN 134"" and all other
     * sail numbers of which "DEN 13" is a prefix.
     */
    private Iterable<String> getSailNumberVariants(String sailNumber) {
        final List<String> result = new LinkedList<>();
        result.add(sailNumber);
        if (sailNumber != null) {
            final Matcher matcher = Pattern.compile("[^A-Za-z]*([A-Za-z]*).*([0-9]*).*").matcher(sailNumber);
            final boolean findResult = matcher.find();
            if (findResult) {
                final String country = matcher.group(1);
                final String number = matcher.group(2);
                for (final String paddingToTry : new String[] { "% %", "%-%" }) {
                    result.add(country+paddingToTry+number);
                }
            }
        }
        return result;
    }

    private static String getDecodedCredentials() {
        return new String(Base64.getDecoder().decode(CREDENTIALS));
    }
}
