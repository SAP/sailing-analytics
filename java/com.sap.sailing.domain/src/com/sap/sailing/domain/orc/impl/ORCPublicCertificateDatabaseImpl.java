package com.sap.sailing.domain.orc.impl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.http.HttpResponse;
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

/**
 * Accesses the ORC public certificate database through the key-based JSON API at
 * {@code https://sas.orc.org/api/{key}/<endpoint>} (see {@code https://sas.orc.org/api-docs.php}). The API key is
 * read from the {@value #API_KEY_ENV_VARIABLE_NAME} environment variable and is placed into the request path; it is
 * never logged. If the environment variable is not set, the request methods throw an {@link IOException} so that
 * misconfiguration surfaces as a failure rather than a silent skip.
 * <p>
 * The following endpoints are used:
 * <ul>
 * <li>{@code activecerts} for {@link #search(CountryCode, Integer, String, String, String, String, boolean) searching}
 * certificate handles by optional, AND-combined criteria (country, VPP year, reference number, yacht name, sail number,
 * boat class name). With no criteria it returns every active certificate.</li>
 * <li>{@code RMS} for the per-country/family {@link #getCountriesWithValidCertificates() overview} including the
 * certificate counts and VPP years.</li>
 * <li>{@code DownBoatRMS_byrefno} for downloading a single full {@link ORCCertificate} by its reference number.</li>
 * </ul>
 */
public class ORCPublicCertificateDatabaseImpl implements ORCPublicCertificateDatabase {
    private static final Logger logger = Logger.getLogger(ORCPublicCertificateDatabaseImpl.class.getName());

    private static final String API_KEY_ENV_VARIABLE_NAME = "ORC_API_KEY";
    private static final String API_BASE_URL = "https://sas.orc.org/api/";

    private static final String ENDPOINT_ACTIVECERTS = "activecerts";
    private static final String ENDPOINT_RMS = "RMS";
    private static final String ENDPOINT_DOWNBOATRMS_BYREFNO = "DownBoatRMS_byrefno";
    private static final String EXT_JSON = "ext=json";

    private static final String DATA_KEY = "data";
    private static final String ROWS_KEY = "rows";
    private static final String ERRORS_KEY = "errors";

    private static final String KEY_COUNTRY_ID = "CountryId";
    private static final String KEY_REF_NO = "RefNo";
    private static final String KEY_DXT_DATE = "dxtDate";
    private static final String KEY_DXT_NAME = "dxtName";
    private static final String KEY_YACHT_NAME = "YachtName";
    private static final String KEY_SAIL_NO = "SailNo";
    private static final String KEY_CLASS = "Class";
    private static final String KEY_FAMILY = "Family";
    private static final String KEY_CERT_TYPE = "CertType";
    private static final String KEY_IS_OD = "IsOd";
    private static final String KEY_VPP_YEAR = "VPPYear";
    private static final String KEY_CERT_COUNT = "CertCount";
    private static final String KEY_LAST_UPDATE = "LastUpdate";
    private static final String KEY_CERT_NAME = "CertName";
    private static final String KEY_RMS_CODE = "RMSCode";

    private static final String DXT_SUFFIX = ".dxt";

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

    @Override
    public Iterable<CertificateHandle> search(final CountryCode issuingCountry, final Integer yearOfIssuance,
            final String referenceNumber, final String yachtName, final String sailNumber, final String boatClassName,
            final boolean includeInvalid /* retained for API compatibility; activecerts returns valid certs only */)
            throws IOException {
        final Set<CertificateHandle> result = new HashSet<>();
        final List<String> queryParts = new ArrayList<>();
        if (referenceNumber != null && !referenceNumber.trim().isEmpty()) {
            queryParts.add(KEY_REF_NO + "=" + encode(referenceNumber.trim()));
        }
        if (issuingCountry != null) {
            queryParts.add(KEY_COUNTRY_ID + "=" + encode(issuingCountry.getThreeLetterIOCCode()));
        }
        if (yearOfIssuance != null) {
            queryParts.add(KEY_VPP_YEAR + "=" + yearOfIssuance);
        }
        if (yachtName != null) {
            queryParts.add(KEY_YACHT_NAME + "=" + encode(yachtName));
        }
        if (sailNumber != null) {
            queryParts.add(KEY_SAIL_NO + "=" + encode(sailNumber));
        }
        if (boatClassName != null) {
            queryParts.add(KEY_CLASS + "=" + encode(boatClassName));
        }
        final String query = queryParts.isEmpty() ? null : String.join("&", queryParts);
        final JSONObject root = httpGetJson(endpointUrl(requireApiKey(), ENDPOINT_ACTIVECERTS, query));
        for (final JSONObject row : dataRows(root)) {
            result.add(parseHandleFromActiveCertRow(row));
        }
        logger.fine(() -> "Search on " + ENDPOINT_ACTIVECERTS + " with query " + query + " returned " + result.size()
                + " results");
        return result;
    }

    @Override
    public Iterable<CountryOverview> getCountriesWithValidCertificates() throws IOException {
        final Set<CountryOverview> result = new HashSet<>();
        final JSONObject root = httpGetJson(endpointUrl(requireApiKey(), ENDPOINT_RMS, null));
        for (final JSONObject row : dataRows(root)) {
            final CountryOverview countryOverview = parseCountryOverviewFromRmsRow(row);
            if (countryOverview.getVPPYear() != null) {
                result.add(countryOverview);
            }
        }
        logger.fine(() -> "Overview on " + ENDPOINT_RMS + " returned " + result.size() + " results");
        return result;
    }

    @Override
    public ORCCertificate getCertificate(final String referenceNumber, final CertificateFamily family)
            throws IOException {
        logger.fine("Obtaining certificate for reference number " + referenceNumber);
        ORCCertificate result;
        try {
            result = getSingleCertificate(ENDPOINT_DOWNBOATRMS_BYREFNO,
                    KEY_REF_NO + "=" + encode(referenceNumber) + "&" + EXT_JSON);
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Error trying to find ORC certificate with reference number " + referenceNumber,
                    e);
            result = null;
        }
        if (result == null) {
            logger.info("Couldn't find ORC certificate with reference number " + referenceNumber);
        }
        return result;
    }

    @Override
    public ORCCertificate searchForUpdate(final CertificateHandle certificateHandle) throws IOException {
        return searchForUpdate(certificateHandle.getReferenceNumber());
    }

    @Override
    public ORCCertificate searchForUpdate(final ORCCertificate certificate) throws IOException {
        return searchForUpdate(certificate.getReferenceNumber());
    }

    private ORCCertificate searchForUpdate(final String existingCertificateRefNo) throws IOException {
        logger.fine("Looking for update of certificate with reference number " + existingCertificateRefNo);
        final ORCCertificate result = getSingleCertificate(ENDPOINT_DOWNBOATRMS_BYREFNO,
                KEY_REF_NO + "=" + encode(existingCertificateRefNo) + "&" + EXT_JSON);
        if (result == null) {
            logger.info("Couldn't find any valid certificate for that boat");
        } else {
            if (result.getReferenceNumber().equals(existingCertificateRefNo)) {
                logger.info("Found no new certificate; the one with reference number " + existingCertificateRefNo
                        + " is still valid.");
            } else {
                logger.info("Found update for " + existingCertificateRefNo + ": " + result);
            }
        }
        return result;
    }

    private ORCCertificate getSingleCertificate(final String endpoint, final String query) throws IOException {
        final ORCCertificate result;
        try {
            final Iterable<ORCCertificate> certificates = new ORCCertificatesJsonImporter()
                    .read(httpGetStream(endpointUrl(requireApiKey(), endpoint, query))).getCertificates();
            result = certificates.iterator().hasNext() ? certificates.iterator().next() : null;
        } catch (final org.json.simple.parser.ParseException e) {
            throw new IOException("Failed to parse ORC certificate JSON", e);
        }
        return result;
    }

    private CertificateHandle parseHandleFromActiveCertRow(final JSONObject row) throws DateTimeParseException {
        final CountryCode issuingCountry = countryFromIoc(asTrimmedString(row.get(KEY_COUNTRY_ID)));
        final String dxtName = asString(row.get(KEY_DXT_NAME));
        final String fileId = dxtName != null && dxtName.toLowerCase().endsWith(DXT_SUFFIX)
                ? dxtName.substring(0, dxtName.length() - DXT_SUFFIX.length())
                : dxtName;
        final Integer familyId = asInteger(row.get(KEY_FAMILY));
        final CertificateFamily family = familyId == null ? CertificateFamily.UNKNOWN
                : Optional.ofNullable(CertificateFamily.fromId(familyId)).orElse(CertificateFamily.UNKNOWN);
        final String dxtDate = asTrimmedString(row.get(KEY_DXT_DATE));
        final TimePoint issueDate = dxtDate == null ? null : new MillisecondsTimePoint(parseDate(dxtDate));
        return new CertificateHandleImpl(issuingCountry, fileId, /* gph */ null, /* sssid */ null, /* datInGID */ null,
                asTrimmedString(row.get(KEY_REF_NO)), family, asString(row.get(KEY_YACHT_NAME)),
                asString(row.get(KEY_SAIL_NO)), asString(row.get(KEY_CLASS)), /* designer */ null, /* builder */ null,
                /* yearBuilt */ null, issueDate, asInteger(row.get(KEY_CERT_TYPE)), asBoolean(row.get(KEY_IS_OD)),
                /* isProvisional */ Boolean.FALSE, /* isValid */ Boolean.TRUE);
    }

    private CountryOverview parseCountryOverviewFromRmsRow(final JSONObject row) throws DateTimeParseException {
        final CountryCode issuingCountry = countryFromIoc(asTrimmedString(row.get(KEY_COUNTRY_ID)));
        final Integer familyId = asInteger(row.get(KEY_FAMILY));
        final CertificateFamily family = familyId == null ? null : CertificateFamily.fromId(familyId);
        final String lastUpdate = asTrimmedString(row.get(KEY_LAST_UPDATE));
        final Integer certCount = asInteger(row.get(KEY_CERT_COUNT));
        return new CountryOverviewImpl(issuingCountry, family, asInteger(row.get(KEY_CERT_TYPE)),
                asInteger(row.get(KEY_VPP_YEAR)), certCount == null ? 0 : certCount.intValue(),
                lastUpdate == null ? null : TimePoint.of(parseDate(lastUpdate)), asTrimmedString(row.get(KEY_CERT_NAME)),
                asTrimmedString(row.get(KEY_RMS_CODE)));
    }

    private static CountryCode countryFromIoc(final String iocCode) {
        return iocCode == null ? null : CountryCodeFactory.INSTANCE.getFromThreeLetterIOCName(iocCode);
    }

    private static String encode(final String value) {
        final String result;
        try {
            result = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is always supported", e);
        }
        return result;
    }

    private String endpointUrl(final String key, final String endpoint, final String query) {
        return API_BASE_URL + key + "/" + endpoint + (query == null ? "" : "?" + query);
    }

    private static String requireApiKey() throws IOException {
        final String key = System.getenv(API_KEY_ENV_VARIABLE_NAME);
        if (key == null || key.trim().isEmpty()) {
            throw new IOException("The " + API_KEY_ENV_VARIABLE_NAME
                    + " environment variable is not set; cannot access the ORC public certificate API.");
        }
        return key;
    }

    private InputStream httpGetStream(final String url) throws IOException {
        final HttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes()).build();
        final HttpResponse response = client.execute(new HttpGet(url));
        return response.getEntity().getContent();
    }

    private JSONObject httpGetJson(final String url) throws IOException {
        final JSONObject root;
        try (final BOMInputStream bomInputStream = new BOMInputStream(httpGetStream(url))) {
            final ByteOrderMark bom = bomInputStream.getBOM();
            final String charsetName = bom == null ? StandardCharsets.UTF_8.name() : bom.getCharsetName();
            final Reader reader = new BufferedReader(
                    new InputStreamReader(new BufferedInputStream(bomInputStream), charsetName));
            final Object parsed = new JSONParser().parse(reader);
            root = parsed instanceof JSONObject ? (JSONObject) parsed : new JSONObject();
        } catch (final org.json.simple.parser.ParseException e) {
            throw new IOException("Failed to parse ORC API JSON response", e);
        }
        final Object errors = root.get(ERRORS_KEY);
        if (errors instanceof JSONArray && !((JSONArray) errors).isEmpty()) {
            throw new IOException("ORC API returned errors: " + errors);
        }
        return root;
    }

    private static Iterable<JSONObject> dataRows(final JSONObject root) {
        final List<JSONObject> result = new ArrayList<>();
        final Object data = root.get(DATA_KEY);
        if (data instanceof JSONObject) {
            final Object rows = ((JSONObject) data).get(ROWS_KEY);
            if (rows instanceof JSONArray) {
                for (final Object row : (JSONArray) rows) {
                    if (row instanceof JSONObject) {
                        result.add((JSONObject) row);
                    }
                }
            }
        }
        return result;
    }

    private static String asString(final Object value) {
        return value == null ? null : value.toString();
    }

    private static String asTrimmedString(final Object value) {
        final String string = value == null ? null : value.toString().trim();
        return string == null || string.isEmpty() ? null : string;
    }

    private static Integer asInteger(final Object value) {
        Integer result;
        if (value instanceof Number) {
            result = ((Number) value).intValue();
        } else {
            final String string = asTrimmedString(value);
            if (string == null) {
                result = null;
            } else {
                try {
                    result = Integer.valueOf(string);
                } catch (final NumberFormatException e) {
                    result = null;
                }
            }
        }
        return result;
    }

    private static Boolean asBoolean(final Object value) {
        final Boolean result;
        if (value instanceof Boolean) {
            result = (Boolean) value;
        } else {
            final String string = asTrimmedString(value);
            result = string == null ? null : Boolean.valueOf(string);
        }
        return result;
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
     * the alternative names, and finally removing the boat class name constraint altogether.
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
        String successfulBoatClassName = boatClass==null?null:boatClass.getName();
        Iterable<CertificateHandle> certificateHandles = search(/* issuingCountry */ null, /* yearOfIssuance */ null, /* referenceNumber */ null, yachtName, sailNumber, successfulBoatClassName, /* includeInvalid */ false);
        if (!containsValidHandle(certificateHandles) && successfulBoatClassName != null) {
            if (yachtName != null || sailNumber != null) {
                logger.fine(()->"Nothing found; removing boat class name restriction "+boatClass.getName());
                // try without boat class restriction
                successfulBoatClassName = null;
                certificateHandles = search(/* issuingCountry */ null, /* yearOfIssuance */ null, /* referenceNumber */ null, yachtName, sailNumber, successfulBoatClassName, /* includeInvalid */ false);
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
}
