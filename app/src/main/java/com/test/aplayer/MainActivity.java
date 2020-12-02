package com.test.aplayer;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.walixiwa.aplayer.tools.APlayer;

public class MainActivity extends AppCompatActivity {
    private ImageView src_over;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        src_over = findViewById(R.id.src_over);
        findViewById(R.id.start).setOnClickListener(v -> {

            new APlayer(MainActivity.this)
                    .setTitle("测试视频")
                    .setUrl("http://vfx.mtime.cn/Video/2019/03/12/mp4/190312143927981075.mp4")
                    .setCookies("Cookie=3dd87828")
                    .setRequestCode(201)
                    .start();
           /* Log.e("info", "bitmap: start" );
            new Thumbnailer()
                    .with(this,"/storage/emulated/0/Pictures/ydcs.mkv")
                    .setCount(100)
                    .setOnThumbParseFinishListener(list -> {
                        for (int i = 0; i < list.size(); i++) {
                            Log.e("info", "bitmap: " + list.get(i).getUri());
                        }
                        Log.e("info", "bitmap: ok" );
                    })
                    .setOnInfoParseFinishListener(list -> {
                        Glide.with(this).load(list.getUri()).into(src_over);
                    })
                    .start();*/
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("info", "onActivityResult: requestCode->" + requestCode + "|resultCode->" + resultCode);
        if (requestCode == 201) {
            Toast.makeText(this, "播放完毕: " + requestCode, Toast.LENGTH_SHORT).show();
        }
    }
}
