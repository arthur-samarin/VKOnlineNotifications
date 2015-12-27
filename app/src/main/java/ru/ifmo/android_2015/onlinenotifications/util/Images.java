package ru.ifmo.android_2015.onlinenotifications.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Images {
    private static LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(16 << 20 /* 16Mb */) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void downloadAsync(final String url, final LoadListener callback) {
        Bitmap cachedImage = imageCache.get(url);
        if (cachedImage != null) {
            callSuccessHandler(cachedImage, callback);
            return;
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                downloadImage(url, callback);
            }
        });
    }

    public static Bitmap downloadSyncOrNull(final String url) {
        try {
            return downloadSync(url);
        } catch (IOException e) {
            Log.w(TAG, "Error loading " + url);
            return null;
        }
    }

    public static Bitmap downloadSync(final String url) throws IOException {
        Bitmap cachedImage = imageCache.get(url);
        if (cachedImage != null) {
            return cachedImage;
        }

        return downloadImageImpl(url);
    }

    private static void downloadImage(String url, LoadListener callback) {
        try {
            Bitmap image = downloadImageImpl(url);
            if (image != null) {
                callSuccessHandler(image, callback);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            callErrorHandler(callback);
        }
    }

    private static Bitmap downloadImageImpl(String url) throws IOException {
        Log.d(TAG, "Loading image from " + url);

        URL address = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) address.openConnection();
        InputStream in = null;
        try {
            in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap != null) {
                imageCache.put(url, bitmap);
            }
            return bitmap;
        } finally {
            if (in != null) {
                in.close();
            }
            connection.disconnect();
        }
    }

    private static void callSuccessHandler(final Bitmap bitmap, final LoadListener listener) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onCompleted(bitmap);
            }
        });
    }

    private static void callErrorHandler(final LoadListener listener) {
        Handler uiHandler = new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                listener.onError();
            }
        });
    }

    public interface LoadListener {
        void onCompleted(Bitmap bitmap);
        void onError();
    }

    private static final String TAG = "Images";
}
