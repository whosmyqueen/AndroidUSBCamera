package com.jiangdg.usbcamera.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.blankj.utilcode.util.CollectionUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UriUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.UploadResultBean;
import com.jiangdg.usbcamera.adapter.FileListAdapter;
import com.jiangdg.usbcamera.adapter.bean.FileListBean;
import com.jiangdg.usbcamera.application.MyApplication;
import com.kongzue.dialogx.dialogs.InputDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

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
    private File videosOkDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        ButterKnife.bind(this);
        String s = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos/";
        String okPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos-ok/";
        videosDir = new File(s);
        videosOkDir = new File(okPath);
        if (!FileUtils.isFileExists(videosDir)) {
            FileUtils.createOrExistsDir(videosDir);
        }
        if (!FileUtils.isFileExists(videosOkDir)) {
            FileUtils.createOrExistsDir(videosOkDir);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new InputDialog("提示", "请输入采集者编号", "确定", "取消", "")
                        .setCancelable(false)
                        .setOkButton((baseDialog, v1, inputStr) -> {
                            if (ObjectUtils.isEmpty(inputStr)) {
                                ToastUtils.showShort("请输入采集者编号");
                                return true;
                            }
                            uploadFiles(inputStr);
                            return false;
                        })
                        .show();

            }
        });
        initAdapter();
        getFileList();
    }

    private void uploadFiles(String userCode) {
        List<FileListBean> data = mAdapter.getData();
        if (CollectionUtils.isEmpty(data)) {
            ToastUtils.showShort("请采集数据后再提交");
            return;
        }
        for (int i = 0; i < data.size(); i++) {
            FileListBean datum = data.get(i);
            File file = new File(datum.getFilePath());
            if (!FileUtils.isFileExists(file)) {
                continue;
            }
            int finalI = i;
//            RxHttp.postForm("http://119.253.84.114:9317/video/cow")
            RxHttp.postForm("http://123.56.2.215:9317/video/cow")
                    .addFile("cow", file)
                    .upload(AndroidSchedulers.mainThread(), progress -> {
                        datum.setStatus(1);
                        datum.setProgress(progress.getProgress());
                        mAdapter.notifyItemChanged(finalI);
                    })
                    .add("code", userCode)
                    .add("md5sum", FileUtils.getFileMD5ToString(file))
                    .asClass(UploadResultBean.class).subscribe(result -> {
                        if (result.getStatus() != 1) {
                            datum.setStatus(3);
                            return;
                        }
                        datum.setStatus(2);

                        if (file.getName().endsWith(".zip"))//如果是压缩包的形式
                            FileUtils.move(file.getParentFile(), new File(videosOkDir + File.separator + file.getParentFile().getName()));
                        else
                            FileUtils.move(file, new File(videosOkDir + File.separator + file.getName()));

                        mAdapter.notifyItemChanged(finalI);
                    }, throwable -> {
                        ToastUtils.showLong(ObjectUtils.toString(throwable));
                        datum.setStatus(3);
                        mAdapter.notifyItemChanged(finalI);
                    });

        }
    }

    private void initAdapter() {
        swipeRefresh.setOnRefreshListener(this::getFileList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FileListAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            FileListBean item = mAdapter.getItem(position);
            File file = new File(item.getFilePath());
            if (!file.exists()) {
                ToastUtils.showShort("当前文件不存在");
                return;
            }
            //getUrl()获取文件目录，例如返回值为/storage/sdcard1/MIUI/music/mp3_hd/单色冰淇凌_单色凌.mp3
            //获取父目录
            File parentFlie = new File(file.getParent());
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setDataAndType(UriUtils.file2Uri(parentFlie), "*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        });
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
            if (file.isDirectory()) {//如果是压缩包的形式
                for (File f : Objects.requireNonNull(file.listFiles())) {
                    if (f.getName().endsWith(".zip")) {
                        list.add(new FileListBean(f.getName(), f.getAbsolutePath()));
                    }
                }
            } else {
                list.add(new FileListBean(file.getName(), file.getAbsolutePath()));
            }
        }
        mAdapter.setList(list);
        swipeRefresh.setRefreshing(false);
    }
}