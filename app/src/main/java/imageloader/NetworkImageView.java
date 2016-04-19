package imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by hzlinxuanxuan on 2016/4/18.
 */
public class NetworkImageView extends ImageView implements ImageRequest.ImageRequestListener {

    private ImageRequest request = null;
    private RequestQueue queue;

    public NetworkImageView(Context context) {
        super(context);
    }

    public NetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRequestQueue(RequestQueue queue) {
        this.queue = queue;
    }

    public void setImageUrl(String url) {
        if (request != null) {
            request.setListener(null);
        }
        request = new ImageRequest(url);
        request.setListener(this);
        if (isAttachedToWindow()) {
            queue.addRequest(request);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (request != null) {
            queue.addRequest(request);
        }
    }

    @Override
    public void onResponse(final Bitmap bitmap) {
        Log.d("RequestQueue-View", "setBitmap " + (bitmap != null));
        if (getHandler() == null) {
            Log.d("RequestQueue-View", "Error no Handler");
        }
        post(new Runnable() {
            @Override
            public void run() {
                Log.d("RequestQueue-View", "setBitmap succ");
                setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        if (request != null) {
            request.isCanceled = true;
            request.setListener(null);
        }
        super.onDetachedFromWindow();
    }
}
