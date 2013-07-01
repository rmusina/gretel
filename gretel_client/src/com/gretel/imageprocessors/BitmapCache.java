package com.gretel.imageprocessors;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;

import java.io.IOException;
import java.net.URL;

public class BitmapCache implements ComponentCallbacks2{
    private LruCache<String, Bitmap> cache;

    public BitmapCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass() * 1024 * 1024;
        this.cache = new LruCache<String, Bitmap>(memoryClass);
    }

    public Bitmap getImage(String url) {
        Bitmap image = this.cache.get(url);

        if (image == null) {
            new DownloadImageTask().execute(url);
        }

        return image;
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= TRIM_MEMORY_MODERATE) {
            this.cache.evictAll();
        }
        else if (level >= TRIM_MEMORY_BACKGROUND) {
            this.cache.trimToSize(cache.size() / 2);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {

    }

    @Override
    public void onLowMemory() {

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Boolean> {
        private Bitmap image;

        @Override
        protected Boolean doInBackground(String... params) {
            String url = params[0];

            try {
                this.image = downloadBitmap(url);

                if (image != null) {
                    cache.put(url, this.image);
                    return true;
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return false;
        }

        private Bitmap downloadBitmap(String url) throws IOException {
            URL imageUrl = new URL(url);
            return BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());
        }
    }
}
