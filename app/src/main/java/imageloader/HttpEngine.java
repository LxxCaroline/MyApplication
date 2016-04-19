package imageloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by hzlinxuanxuan on 2016/4/11.
 */
public class HttpEngine {

    private static final int TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 40000;

    public byte[] getBitmapByteFromNetwork(String url) {
        byte[] data = null;
        try {
            URL urlInstance = new URL(url);
            HttpURLConnection con = (HttpsURLConnection) urlInstance.openConnection();
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(READ_TIMEOUT);
            con.setDoInput(true);
            InputStream is = con.getInputStream();
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                byte[] buff = new byte[100];
                int rc = 0;
                while ((rc = is.read(buff, 0, 100)) > 0) {
                    swapStream.write(buff, 0, rc);
                }
                data = swapStream.toByteArray();
            }
            is.close();
        } catch (Exception e) {
            // TODO: 2016/4/11 deal with the exception
            e.printStackTrace();
        }
        return data;
    }

}
