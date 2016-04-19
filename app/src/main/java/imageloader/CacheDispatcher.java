package imageloader;

import android.os.Process;
import android.util.Log;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by hzlinxuanxuan on 2016/4/13.
 */
public class CacheDispatcher extends Thread {

    private final String TAG = "RequestQueue-CacheD";

    /**
     * 等待去Cache中找的request队列，使用PriorityBlockingQueue当为空时，会阻塞线程，直到有数据放入
     */
    public PriorityBlockingQueue<ImageRequest> mCacheQueue;
    /**
     * 等待去Network中找的request队列
     */
    private PriorityBlockingQueue<ImageRequest> mNetworkQueue;
    /**
     * 读取Cache的类
     */
    private ImageCache mCache;
    private RequestQueue queue;
    private boolean isQuit = false;

    public CacheDispatcher(RequestQueue queue, PriorityBlockingQueue<ImageRequest> cacheQueue, PriorityBlockingQueue<ImageRequest> networkQueue, ImageCache cache) {
        this.queue = queue;
        mCacheQueue = cacheQueue;
        mNetworkQueue = networkQueue;
        mCache = cache;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        ImageRequest request = null;
        while (!isQuit && true) {
            try {
                request = mCacheQueue.take();
                if (request.isCanceled) {
                    request.addTracer("already be canceled in cache and will be removed");
                    queue.removeRequest(request);
                    continue;
                }
                ImageCache.Entry entry = mCache.get(request.getCacheKey());
                if (entry == null) {
                    request.addTracer("can't find in cache");
                    request.addTracer("add to network queue");
                    mNetworkQueue.add(request);
                    continue;
                }
                request.addTracer("find in cache");
                request.onResponse(entry.bitmap);
                queue.finishRequest(request);
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
