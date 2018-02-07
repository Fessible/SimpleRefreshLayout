package com.example.com.simplerefreshlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.com.simplerefresh.SimpleRefreshLayout;

/**
 * Created by rhm on 2018/2/6.
 */

public class ScrollViewActivity extends AppCompatActivity {
    private SimpleRefreshLayout simpleRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollview_layout);
        simpleRefreshLayout = findViewById(R.id.refresh_layout);
        simpleRefreshLayout.setOnRefresh(new SimpleRefreshLayout.onRefreshListener() {
            @Override
            public void onUpRefresh() {
                simpleRefreshLayout.stopRefresh();
            }

            @Override
            public void onDownRefresh() {
                simpleRefreshLayout.stopRefresh();
            }
        });
    }
}
