package com.walixiwa.aplayer.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aplayer.APlayerAndroid;
import com.walixiwa.aplayer.model.MyMediaInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Thumbnailer {
    private int count = 1;
    private String mediaPath;
    private OnThumbParseFinishListener onThumbParseFinishListener;
    private OnInfoParseFinishListener onInfoParseFinishListener;

    private String bitMapSavePath;


    public Thumbnailer with(Context context, String mediaPath) {
        this.bitMapSavePath = getCachePath(context);
        this.mediaPath = mediaPath;
        return this;
    }

    public Thumbnailer setCount(int count) {
        this.count = count;
        return this;
    }

    public Thumbnailer setOnThumbParseFinishListener(OnThumbParseFinishListener onThumbParseFinishListener) {
        this.onThumbParseFinishListener = onThumbParseFinishListener;
        return this;
    }

    public void start() {
        new Thread(() -> {
            List<MyMediaInfo> list = new ArrayList<>();
            try {
                APlayerAndroid.MediaInfo mediaInfo = APlayerAndroid.parseThumbnail(mediaPath);
                double fix = mediaInfo.duration_ms / count;
                for (int i = 0; i < count; i++) {
                    long timeMs = (long) (i * fix);
                    String path = bitMapSavePath + mediaPath.hashCode() + i + ".jpg";
                    File srcFile = new File(path);
                    MyMediaInfo myMediaInfo;
                    if (srcFile.exists()) {
                        myMediaInfo = new MyMediaInfo(path, timeMs);
                    } else {
                        APlayerAndroid.MediaInfo info = APlayerAndroid.parseThumbnail(mediaPath, timeMs, -1, -1);
                        saveBitmapToLocal(Integer.toString(mediaPath.hashCode()) + i + ".jpg", info.bitMap);
                        myMediaInfo = new MyMediaInfo(path, info.show_ms);
                    }
                    list.add(myMediaInfo);
                    if (onInfoParseFinishListener != null) {
                        new Handler(Looper.getMainLooper()).post(() -> onInfoParseFinishListener.onThumbParseFinish(myMediaInfo));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (onThumbParseFinishListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> onThumbParseFinishListener.onThumbParseFinish(list));
            }
        }).start();
    }

    public interface OnThumbParseFinishListener {
        void onThumbParseFinish(List<MyMediaInfo> list);
    }

    public Thumbnailer setOnInfoParseFinishListener(OnInfoParseFinishListener onInfoParseFinishListener) {
        this.onInfoParseFinishListener = onInfoParseFinishListener;
        return this;
    }

    public interface OnInfoParseFinishListener {
        void onThumbParseFinish(MyMediaInfo list);
    }


    private void saveBitmapToLocal(String fileName, Bitmap bitmap) {
        try {
            File filePath = new File(bitMapSavePath + fileName);
            if (filePath.exists()) {
                filePath.delete();
            }
            // 创建文件流，指向该路径，文件名叫做fileName
            File file = new File(bitMapSavePath, fileName);
            // file其实是图片，它的父级File是文件夹，判断一下文件夹是否存在，如果不存在，创建文件夹
            File fileParent = file.getParentFile();
            if (!fileParent.exists()) {
                // 文件夹不存在
                fileParent.mkdirs();// 创建文件夹
            }
            // 将图片保存到本地
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getCachePath(Context context) {
        String dataBasePath;
        File dir = context.getExternalCacheDir();
        if (dir != null) {
            dataBasePath = dir.getAbsolutePath() + File.separator + "vodSrcCache" + File.separator;
        } else {
            dataBasePath = context.getCacheDir().getAbsolutePath() + File.separator + "vodSrcCache" + File.separator;
        }
        if (!new File(dataBasePath).exists()) {
            new File(dataBasePath).mkdirs();
        }
        return dataBasePath;
    }
}
