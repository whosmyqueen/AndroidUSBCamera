package com.jiangdg.usbcamera.view;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.application.MyApplication;
import com.jiangdg.usbcamera.utils.FileUtil;
import com.kongzue.dialogx.dialogs.InputDialog;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * UVCCamera use demo
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {
    private static final String TAG = "Debug";
    @BindView(R.id.camera_view)
    View mTextureView;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.seekbar_brightness)
    SeekBar mSeekBrightness;
    @BindView(R.id.seekbar_contrast)
    SeekBar mSeekContrast;
    @BindView(R.id.switch_rec_voice)
    Switch mSwitchVoice;
    @BindView(R.id.btn_record)
    FloatingActionButton btnRecord;


    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private AlertDialog mDialog;

    private boolean isRequest;
    private boolean isPreview;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTextureView.setVisibility(View.VISIBLE);
//                    }
//                });
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTextureView.setVisibility(View.GONE);
//                    }
//                });
                showShortMsg(device.getDeviceName() + "设备断开");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("连接失败,请检查摄像头");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("连接中");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                            mSeekBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
                            mSeekContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("断开连接");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        initView();

        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultPreviewSize(1920, 1080);
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
        btnRecord.setOnClickListener(v -> {
            if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                showShortMsg("摄像头连接失败,请重新拔插");
                return;
            }
            if (!mCameraHelper.isPushing()) {
                String videoPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos/" + System.currentTimeMillis();

//                    FileUtil.createfile(FileUtil.ROOT_PATH + "test666.h264");
                // if you want to record,please create RecordParams like this
                RecordParams params = new RecordParams();
                params.setRecordPath(videoPath);
                params.setRecordDuration(0);                        // auto divide saved,default 0 means not divided
                params.setVoiceClose(mSwitchVoice.isChecked());    // is close voice

                params.setSupportOverlay(true); // overlay only support armeabi-v7a & arm64-v8a
                mCameraHelper.startPusher(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                    @Override
                    public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                        // type = 1,h264 video stream
                        if (type == 1) {
                            FileUtil.putFileStream(data, offset, length);
                        }
                        // type = 0,aac audio stream
                        if (type == 0) {

                        }
                    }

                    @Override
                    public void onRecordResult(String videoPath) {
                        if (TextUtils.isEmpty(videoPath) || !FileUtils.isFileExists(videoPath)) {
                            return;
                        }
                        new InputDialog("重命名", "请输入 胸围-胸宽-体斜长,例: 67.3-45.4-89.4", "确定", "取消", "")
                                .setCancelable(false)
                                .setOkButton((baseDialog, v1, inputStr) -> {
                                    if (ObjectUtils.isEmpty(inputStr)) {
                                        showShortMsg("请检查输入格式,例如: 胸围-胸宽-体斜长");
                                        return true;
                                    }
                                    String[] split = inputStr.split("-");
                                    if (ObjectUtils.isEmpty(split)) {
                                        showShortMsg("请检查输入格式,例如: 胸围-胸宽-体斜长");
                                        return true;
                                    }
                                    if (split.length != 3) {
                                        showShortMsg("请检查输入格式,例如: 胸围-胸宽-体斜长");
                                        return true;
                                    }
                                    for (String s : split) {
                                        try {
                                            Double.parseDouble(s);
                                        } catch (NumberFormatException e) {
                                            showShortMsg("请检查输入格式,例如: 60.2-30.4-80.9");
                                            return true;
                                        }
                                    }
                                    File file = new File(videoPath);
                                    FileUtils.rename(file, inputStr + "-" + file.getName());
                                    return false;
                                })
                                .show();
//                        new Handler(getMainLooper()).post(() -> Toast.makeText(USBCameraActivity.this, "save videoPath:" + videoPath, Toast.LENGTH_SHORT).show());
                    }
                });
                // if you only want to push stream,please call like this
                // mCameraHelper.startPusher(listener);
                showShortMsg("开始录制");
                btnRecord.setImageResource(R.mipmap.stop);
            } else {
                FileUtil.releaseFile();
                mCameraHelper.stopPusher();
                showShortMsg("结束录制");
                btnRecord.setImageResource(R.mipmap.record);
            }
        });

        mCameraHelper.setOnPreviewFrameListener(nv21Yuv -> Log.d(TAG, "onPreviewResult: " + nv21Yuv.length));
    }

    private void initView() {
        setSupportActionBar(mToolbar);

        mSeekBrightness.setMax(100);
        mSeekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekContrast.setMax(100);
        mSeekContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toobar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.menu_takepic:
//                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
//                    showShortMsg("sorry,camera open failed");
//                    return super.onOptionsItemSelected(item);
//                }
//                String picPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/images/"
//                        + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;
//
//                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
//                    @Override
//                    public void onCaptureResult(String path) {
//                        if(TextUtils.isEmpty(path)) {
//                            return;
//                        }
//                        new Handler(getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(USBCameraActivity.this, "save path:"+path, Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                });
//
//                break;
            case R.id.menu_rtc:
                // 远程协助

                ToastUtils.showLong("开发中");
                break;
            case R.id.menu_file_list:
                // 文件列表
                startActivity(new Intent(this, FileListActivity.class));
                break;
            case R.id.menu_resolution:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                showResolutionListDialog();
                break;
            case R.id.menu_focus:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                mCameraHelper.startCameraFoucs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResolutionListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(USBCameraActivity.this);
        View rootView = LayoutInflater.from(USBCameraActivity.this).inflate(R.layout.layout_dialog_list, null);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_dialog);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(USBCameraActivity.this, android.R.layout.simple_list_item_1, getResolutionList());
        if (adapter != null) {
            listView.setAdapter(adapter);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened())
                    return;
                final String resolution = (String) adapterView.getItemAtPosition(position);
                String[] tmp = resolution.split("x");
                if (tmp != null && tmp.length >= 2) {
                    int widht = Integer.valueOf(tmp[0]);
                    int height = Integer.valueOf(tmp[1]);
                    mCameraHelper.updateResolution(widht, height);
                }
                mDialog.dismiss();
            }
        });

        builder.setView(rootView);
        mDialog = builder.create();
        mDialog.show();
    }

    // example: {640x480,320x240,etc}
    private List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtil.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    private void showShortMsg(String msg) {
        ToastUtils.showShort(msg);
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }
}
