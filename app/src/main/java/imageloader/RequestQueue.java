package imageloader;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hzlinxuanxuan on 2016/4/13.
 * 仿照Volley写一个图片缓存的简单库
 */
public class RequestQueue {

    private final String TAG = "RequestQueue";

    private AtomicInteger mSequenceGenerator = new AtomicInteger();
    /**
     * 如果检测到与自己url相同的request正在请求中，则将自己加入到mWaitingRequests
     * 当与自己url相同的request返回时，去map中找到与url对应的queue，将里面的request都返回
     */
    private Map<String, Queue<ImageRequest>> mWaitingRequests;

    private CacheDispatcher cacheDispatcher;
    private ImageCache cache;
    private NetworkDispatcher[] networkDispatchers;
    private int NETWORK_DISPATCHER_SIZE = 2;
    private PriorityBlockingQueue<ImageRequest> mNetworkQueue = new PriorityBlockingQueue<>();
    private PriorityBlockingQueue<ImageRequest> mCacheQueue = new PriorityBlockingQueue<>();

    public RequestQueue(Context context) {
        mWaitingRequests = new HashMap<>();
        cache = new ImageCache(context);
    }

    public void start() {
        cacheDispatcher = new CacheDispatcher(this, mCacheQueue, mNetworkQueue, cache);
        cacheDispatcher.start();
        networkDispatchers = new NetworkDispatcher[NETWORK_DISPATCHER_SIZE];
        for (int i = 0; i < NETWORK_DISPATCHER_SIZE; i++) {
            networkDispatchers[i] = new NetworkDispatcher(this, mNetworkQueue, cache);
            networkDispatchers[i].start();
        }
    }

    public void addRequest(ImageRequest request) {
        request.setSequence(mSequenceGenerator.incrementAndGet());
        Log.d(TAG, "addRequest : " + request.sequence);
        synchronized (mWaitingRequests) {
            //如果已经有类似的请求在进行中，则加入到该queue中。
            if (mWaitingRequests.containsKey(request.getCacheKey())) {
                request.addTracer("add to WaitingRequests");
                Queue<ImageRequest> stagedRequests = mWaitingRequests.get(request.getCacheKey());
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<>();
                }
                stagedRequests.add(request);
                mWaitingRequests.put(request.getCacheKey(), stagedRequests);
                Log.d(TAG, "mWaitingRequests.size = " + mWaitingRequests.get(request.getCacheKey()).size() + "-----" + request.url);
            } else {
                //如果没有类似的请求在进行中，则加入
                request.addTracer("add into cache queue");
                mWaitingRequests.put(request.getCacheKey(), null);
                mCacheQueue.add(request);
            }
        }
    }

    //删除一个请求，该请求尚未成功返回
    public void removeRequest(ImageRequest request) {
        //一个request被删掉，会删除所有url一样的request
        synchronized (mWaitingRequests) {
            mWaitingRequests.remove(request.getCacheKey());
        }
    }

    //一个请求结束
    public void finishRequest(ImageRequest request) {
        Log.d(TAG, "--------------finish request-----------------");
        synchronized (mWaitingRequests) {
            Queue<ImageRequest> stagedQueue = mWaitingRequests.get(request.getCacheKey());
            if (stagedQueue == null) {
                Log.d(TAG, "no same request for " + request.url);
                mWaitingRequests.remove(request.getCacheKey());
                return;
            }
            Log.d(TAG, "has " + stagedQueue.size() + " same request for " + request.url);
            while (stagedQueue.size() > 0) {
                ImageRequest temp = stagedQueue.poll();
                temp.addTracer("get the bitmap from waiting request list");
                temp.onResponse(request.response);
            }
            mWaitingRequests.remove(request.getCacheKey());
        }
    }

    public void stop() {
        cacheDispatcher.quit();
        for (int i = 0; i < NETWORK_DISPATCHER_SIZE; i++) {
            networkDispatchers[i].quit();
        }
    }
}
