package com.jiangdg.usbcamera.application;

import android.app.Application;

import com.blankj.utilcode.util.Utils;
import com.innovation.ai.technical.RTCSDK;
import com.jiangdg.usbcamera.BuildConfig;
import com.jiangdg.usbcamera.utils.CrashHandler;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.style.MaterialStyle;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import rxhttp.RxHttpPlugins;

/**application class
 *
 * Created by jianddongguo on 2017/7/20.
 */

public class MyApplication extends Application {
    private CrashHandler mCrashHandler;
    // File Directory in sd card
    public static final String DIRECTORY_NAME = "USBCamera";

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        RTCSDK.initConfig(this); //测试的
        RTCSDK.init(this, "4z3hlwrv42hbt"); //测试的
        mCrashHandler = CrashHandler.getInstance();
        mCrashHandler.init(getApplicationContext(), getClass());
        DialogX.init(this);
        DialogX.globalStyle = MaterialStyle.style();
        DialogX.autoShowInputKeyboard = true;
        DialogX.globalTheme = DialogX.THEME.AUTO;


        //设置读、写、连接超时时间为15s
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.SECONDS)
                .readTimeout(1000, TimeUnit.SECONDS)
                .writeTimeout(1000, TimeUnit.SECONDS)
                .build();
        RxHttpPlugins.init(client).setDebug(BuildConfig.DEBUG);
    }
}
