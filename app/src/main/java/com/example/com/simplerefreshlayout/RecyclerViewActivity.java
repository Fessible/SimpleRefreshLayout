package com.example.com.simplerefreshlayout;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.example.com.simplerefresh.SimpleRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhm on 2018/2/6.
 */

public class RecyclerViewActivity extends AppCompatActivity {
    private List<String> data = new ArrayList<>();
    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;
    private SimpleRefreshLayout simpleRefreshLayout;
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview_layout);
        initView();
    }

    private void initView() {
        initData();
        recyclerView = findViewById(R.id.recycler_view);
        simpleRefreshLayout = findViewById(R.id.refresh_layout);
        adapter = new RecyclerViewAdapter(this, data);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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

    private void initData() {
        for (int i = 0; i < 20; i++) {
            data.add("hehe->" + i);
        }
    }
}
