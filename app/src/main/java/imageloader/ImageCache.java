package imageloader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

import libcore.io.DiskLruCache;

/**
 * Created by hzlinxuanxuan on 2016/4/13.
 * //对cache的读写操作，主要通过LruCache和DiskLruCache
 * 存在内存中的是bitmap,存在硬盘上是byte[]，从网络读过来也是byte[]
 */
public class ImageCache {

    private final String TAG = "ImageC";
    private LruCache<String, Bitmap> mLruCache;
    private DiskLruCache mDiskCache;

    public ImageCache(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            int size = (int) Runtime.getRuntime().maxMemory() / 5;
            Log.d(TAG, "runtime max memory : " + size);
            mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime().maxMemory() / 5) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    int size = value.getRowBytes() * value.getHeight();
                    Log.d(TAG, "size of bitmap : " + size);
                    return size;
                }

                @Override
                protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                    super.entryRemoved(evicted, key, oldValue, newValue);
                    Log.d("tag", "remove==============key:" + key + " is removed.   evicted:" + evicted);
                }
            };
        }
        try {
            File cacheDir = getDiskCacheDir(context, "bitmap");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            mDiskCache = DiskLruCache.open(cacheDir, getAppVersion(context), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: 2016/4/19 防止并发
    //访问cache。读取数据，先从LruCache中读取，若没有，则读取DiskLruCache，读出的entry带有bitmap
    public Entry get(String key) {
        Log.d(TAG, "get the data from cache");
        Entry entry = new Entry();
        Bitmap data = mLruCache.get(key);
        if (data != null) {
            Log.d("tag", "get the data from LruCache");
            entry.bitmap = data;
            return entry;
        }
        try {
            DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
            if (snapshot != null) {
                InputStream stream = snapshot.getInputStream(0);
                Log.d(TAG, "get the data from DiskLruCache");
                entry.bitmap = BitmapFactory.decodeStream(stream);
                mLruCache.put(key,entry.bitmap);
                return entry;
            }
        } catch (IOException e) {
            Log.d(TAG, "Error when get the data from DiskLruCache");
            e.printStackTrace();
        }
        Log.d(TAG, "no data from DiskLruCache");
        return null;
    }

    //从网络读取到数据（byte[]），写入到cache中（包括LruCache和DiskLruCache）
    public void put(String key, Entry entry) {
        try {
            DiskLruCache.Editor editor = mDiskCache.edit(key);
            if (editor != null) {
                OutputStream stream = editor.newOutputStream(0);
                if (entry.data != null && entry.data.length != 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(entry.data, 0, entry.data.length);
                    if (bitmap != null) {
                        Log.d(TAG, "put the bitmap into LruCache");
                        mLruCache.put(key, bitmap);
                    } else {
                        Log.d(TAG, "Error when decode byte array from byte");
                    }
                    stream.write(entry.data);
                    editor.commit();
                } else {
                    editor.abort();
//                    Log.d(TAG, "write the data fail");
                }
            }
            mDiskCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    private int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static class Entry {
        public Bitmap bitmap;
        public byte[] data;
    }
}
