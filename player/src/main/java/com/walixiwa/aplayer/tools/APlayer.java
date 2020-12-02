package com.walixiwa.aplayer.tools;

import android.app.Activity;
import android.content.Intent;

import com.walixiwa.aplayer.APlayerActivity;

public class APlayer {
    private String title = "";
    private String url = "";
    private String cookies = "";
    private Activity activity;
    private boolean isLive = false;
    private int requestCode = -1;
    private int position = 0;

    public APlayer(Activity activity) {
        this.activity = activity;
    }


    public APlayer setTitle(String title) {
        this.title = title;
        return this;
    }

    public APlayer setUrl(String url) {
        this.url = url;
        return this;
    }

    public APlayer setCookies(String cookies) {
        this.cookies = cookies;
        return this;
    }

    public APlayer setLive(boolean live) {
        isLive = live;
        return this;
    }

    public APlayer setRequestCode(int requestCode) {
        this.requestCode = requestCode;
        return this;
    }

    public APlayer setPosition(int position) {
        this.position = position;
        return this;
    }

    public void start() {
        Intent intent = new Intent(activity, APlayerActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.putExtra("cookies", cookies);
        intent.putExtra("isLive", isLive);
        intent.putExtra("position", position);
        if (requestCode == -1) {
            activity.startActivity(intent);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
