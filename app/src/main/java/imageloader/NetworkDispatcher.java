package imageloader;

import android.os.Process;
import android.util.Log;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by hzlinxuanxuan on 2016/4/13.
 */
public class NetworkDispatcher extends Thread {

    private final String TAG = "RequestQueue-NetD";
    /**
     * 等待访问Network的request队列
     */
    private PriorityBlockingQueue<ImageRequest> mNetworkQueue;
    /**
     * 在网络请求到达后，需要将数据写入到cache中
     */
    private ImageCache mCache;
    private RequestQueue mRequestQueue;
    private boolean isQuit = false;
    private HttpEngine engine;

    public NetworkDispatcher(RequestQueue queue, PriorityBlockingQueue<ImageRequest> networkQueue, ImageCache cache) {
        mRequestQueue = queue;
        mNetworkQueue = networkQueue;
        mCache = cache;
        engine = new HttpEngine();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        ImageRequest request = null;
        while (!isQuit && true) {
            try {
                request = mNetworkQueue.take();
                if (request.isCanceled) {
                    request.addTracer("already be canceled in network and will be removed");
                    mRequestQueue.removeRequest(request);
                    continue;
                }
                ImageCache.Entry entry = new ImageCache.Entry();
                entry.data = engine.getBitmapByteFromNetwork(request.url);
                if (entry.data == null || entry.data.length == 0) {
                    request.addTracer("can not get data from network");
                    mRequestQueue.removeRequest(request);
                    continue;
                }
                request.addTracer("get data from network");
                request.addTracer("write data into cache");
                mCache.put(request.getCacheKey(), entry);
                request.parseByteData(entry.data);
                mRequestQueue.finishRequest(request);
            } catch (InterruptedException e) {
                Log.d(TAG, "Error InterruptedException:" + e.getMessage());
                if (request != null) {
                    request.addTracer("Error InterruptedException");
                    request.onResponse(null);
                }
                e.printStackTrace();
                if (isQuit) {
                    return;
                }
                continue;
            }
        }
    }

    public void quit() {
        isQuit = true;
    }
}
