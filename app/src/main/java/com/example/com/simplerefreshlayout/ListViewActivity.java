package com.example.com.simplerefreshlayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.com.simplerefresh.SimpleRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhm on 2018/2/6.
 */

public class ListViewActivity extends AppCompatActivity {
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private List<String> data = new ArrayList<>();
    private SimpleRefreshLayout simpleRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview_layout);
        initView();
    }

    private void initView() {
        initData();
        simpleRefreshLayout = findViewById(R.id.refresh_layout);
        listView = findViewById(R.id.list_view);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, data);
        listView.setAdapter(arrayAdapter);
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
