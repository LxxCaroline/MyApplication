package com.example.caroline.myapplication;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

import imageloader.RequestQueue;

public class MainActivity extends Activity {

    RequestQueue queue;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.test);
        lv = (ListView) findViewById(R.id.lv);
        queue = new RequestQueue(this);
        queue.start();
    }

    public void onClick(View view) {
        Log.d("RequestQueue", "*********************************************************************************");
        ListAdapter adapter = new ListAdapter(this, queue);
        lv.setAdapter(adapter);
    }


    int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        while (reqWidth <= (width / 2) && reqHeight <= (height / 2)) {
            inSampleSize *= 2;
            width /= 2;
            height /= 2;
        }
        return inSampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        queue.stop();
    }
}
