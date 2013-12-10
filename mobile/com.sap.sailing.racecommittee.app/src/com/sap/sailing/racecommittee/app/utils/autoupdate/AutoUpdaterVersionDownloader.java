package com.sap.sailing.racecommittee.app.utils.autoupdate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.racecommittee.app.data.http.HttpGetRequest;
import com.sap.sailing.racecommittee.app.utils.autoupdate.AutoUpdaterChecker.AutoUpdaterState;

public class AutoUpdaterVersionDownloader extends AutoUpdaterDownloader<Pair<Integer, String>> {

    public AutoUpdaterVersionDownloader(AutoUpdaterState state) {
        super(state);
    }

    @Override
    protected Pair<Integer, String> downloadInBackground(URL url) {
        HttpGetRequest request = new HttpGetRequest(url);
        InputStream stream = null;
        try {
            stream = request.execute();
            if (stream != null) {
                String contents = readStream(stream);
                return parseVersionFile(contents);
            }
        } catch (Exception e) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    private Pair<Integer, String> parseVersionFile(String contents) {
        String[] map = contents.split("=");
        if (map.length == 2) {
            String apkFileName = map[0];
            String versionCode = map[1];
            try {
                Integer code = Integer.parseInt(versionCode);
                return new Pair<Integer, String>(code, apkFileName);
            } catch (NumberFormatException e) {
            }
        }
        return null;
    }

    @Override
    protected void onError() {
        state.onError();
    }

    @Override
    protected void onSuccess(Pair<Integer, String> result) {
        state.updateToVersion(result.getA(), result.getB());
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return new String(baos.toByteArray(), Charset.defaultCharset());
    }

}
