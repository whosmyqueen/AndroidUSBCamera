package com.jiangdg.usbcamera.view;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.adapter.FileListAdapter;
import com.jiangdg.usbcamera.adapter.bean.FileListBean;
import com.jiangdg.usbcamera.application.MyApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FileListActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.btn_upload)
    FloatingActionButton btn_upload;

    private FileListAdapter mAdapter;
    private File videosDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        ButterKnife.bind(this);
        String s = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos/";
        videosDir = new File(s);
        if (!FileUtils.isFileExists(videosDir)) {
            FileUtils.createOrExistsDir(videosDir);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        initAdapter();
        getFileList();
    }

    private void initAdapter() {
        swipeRefresh.setOnRefreshListener(this::getFileList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FileListAdapter();
        recyclerView.setAdapter(mAdapter);
        mAdapter.setEmptyView(R.layout.empty_view);
    }

    private void getFileList() {
        swipeRefresh.setRefreshing(true);
        List<FileListBean> list = new ArrayList<>();
        List<File> files = FileUtils.listFilesInDir(videosDir);
        if (ObjectUtils.isEmpty(files)) {
            mAdapter.setList(null);
            swipeRefresh.setRefreshing(false);
            return;
        }
        for (File file : files) {
            list.add(new FileListBean(file.getName(), file.getAbsolutePath()));
        }
        mAdapter.setList(list);
        swipeRefresh.setRefreshing(false);
    }
}