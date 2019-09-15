package com.cch.sitemap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.cch.sitemap.R;

import java.util.Timer;
import java.util.TimerTask;


public class SplashActivity extends AppCompatActivity {
    private static final int MESSAGE_TIMER = 1;
    private final int SPLASH_DISPLAY_LENGHT = 3000; //延迟三秒
    private ProgressBar progressBar = null;
    private Timer mTimer;// 定时器
    private int count = 0;
    private int delay = 0;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_TIMER) {
                count += 1;
                progressBar.setProgress(count);//设置主进度条
                progressBar.setSecondaryProgress((int) (count * 1.1));//设置次进度条
                if (count == progressBar.getMax()) {
                    count = 0;
                    mTimer.cancel();
                    Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                    SplashActivity.this.startActivity(mainIntent); //跳转界面
                    SplashActivity.this.finish();  //销毁本界面
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS); //show progress on loadup
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(99);
        progressBar.setProgress(0);
        delay = SPLASH_DISPLAY_LENGHT / progressBar.getMax();//得到定时器的定时时间
        mTimer = new Timer();//创建一个定时器
        timerTask();//开始定时器任务
        //Handler 信息处理函数
    }

    //定时更新时间
    public void timerTask() {
        //创建定时线程执行更新任务
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = mHandler.obtainMessage(MESSAGE_TIMER);
                mHandler.sendMessage(msg);
            }
        }, 0, delay);//第一个0代表在第一次执行之前等待0ms 第二个delay代表第一次执行之后以后每隔多久执行一次
    }
}


