package imageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by hzlinxuanxuan on 2016/4/13.
 */
public class ImageRequest implements Comparable<ImageRequest> {

    public int sequence;
    public String url;
    public Bitmap response;
    //可以取消这个request
    public boolean isCanceled = false;
    public ImageRequestListener listener;
    private ArrayList<String> tracer;

    public ImageRequest(String url) {
        this.url = url;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setListener(ImageRequestListener listener) {
        this.listener = listener;
    }

    public String getCacheKey() {
        String cacheKey;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    //解析来自网络的数据
    public void parseByteData(byte[] data) {
        if (data != null && data.length != 0) {
            onResponse(BitmapFactory.decodeByteArray(data, 0, data.length));
        }
    }

    public void onResponse(Bitmap bitmap) {
        response = bitmap;
        Log.d("RequestQueue--ImgReq", "Request "+sequence + " finish -- " + url);
        for (int i = 0; tracer != null && i < tracer.size(); i++) {
            Log.d("RequestQueue--ImgReq", tracer.get(i));
        }
        if (listener != null && !isCanceled) {
            listener.onResponse(bitmap);
        }else{
            Log.d("RequestQueue--ImgReq","no listener");
        }
    }

    public synchronized void addTracer(String trace) {
        if (tracer == null) {
            tracer = new ArrayList<>();
        }
        tracer.add(trace);
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    @Override
    public int compareTo(ImageRequest another) {
        return sequence - another.sequence;
    }

    public interface ImageRequestListener {
        void onResponse(Bitmap bitmap);
    }
}
