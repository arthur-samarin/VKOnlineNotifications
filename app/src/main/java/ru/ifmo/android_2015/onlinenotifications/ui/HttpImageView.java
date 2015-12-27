package ru.ifmo.android_2015.onlinenotifications.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import ru.ifmo.android_2015.onlinenotifications.R;
import ru.ifmo.android_2015.onlinenotifications.util.Images;

public class HttpImageView extends FrameLayout {
    private ImageView innerImageView;
    private ProgressBar progressBar;
    private String currentUrl;

    public HttpImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_http_image, this);

        innerImageView = (ImageView) findViewById(R.id.innerImageView);
        progressBar = (ProgressBar) findViewById(R.id.imageLoadProgressBar);
    }

    public void setUrl(String url) {
        currentUrl = url;
        innerImageView.setImageBitmap(null);
        if (url != null) {
            progressBar.setVisibility(VISIBLE);
            loadImage();
        } else {
            progressBar.setVisibility(GONE);
        }
    }

    private void loadImage() {
        final String url = currentUrl;
        Images.downloadAsync(currentUrl, new Images.LoadListener() {
            @Override
            public void onCompleted(Bitmap bitmap) {
                if (url.equals(currentUrl)) {
                    innerImageView.setImageBitmap(bitmap);
                }
                progressBar.setVisibility(GONE);
            }

            @Override
            public void onError() {
                progressBar.setVisibility(GONE);
            }
        });
    }

    private static final String TAG = "HttpImageView";
}
