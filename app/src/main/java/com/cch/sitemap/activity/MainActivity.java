package com.cch.sitemap.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.cch.sitemap.R;
import com.cch.sitemap.fragments.AugmentedRealityFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AugmentedRealityFragment mFragment;
    private GestureDetector gestureDetector;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mFragment.onTouchEvent(event.getX(), event.getY());
        }
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.finish();
        System.exit(0);
        Log.e(TAG, "Exit when pause");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_PROGRESS); //show progress on loadup
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        gestureDetector = new GestureDetector(MainActivity.this, new myGestureListener());

        mFragment = new AugmentedRealityFragment();
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, mFragment);
        transaction.commit();
    }

    class myGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        /*识别滑动，第一个参数为刚开始事件，第二个参数为结束事件*/
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            if (e1.getX() - e2.getX() > 100) {
                //Toast.makeText(MainActivity.this, "从右往左滑动" + (e1.getX() - e2.getX()), Toast.LENGTH_LONG).show();
            } else if (e2.getX() - e1.getX() > 100) {
                //Toast.makeText(MainActivity.this, "从左往右滑动" + (e2.getX() - e1.getX()), Toast.LENGTH_LONG).show();
            } else if (e1.getY() - e2.getY() > 100) {
                //Toast.makeText(MainActivity.this, "从下往上滑动" + (e1.getY() - e2.getY()), Toast.LENGTH_LONG).show();
            } else if (e2.getY() - e1.getY() > 100) {
                //Toast.makeText(MainActivity.this, "从上往下滑动" + (e2.getY() - e1.getY()), Toast.LENGTH_LONG).show();
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
