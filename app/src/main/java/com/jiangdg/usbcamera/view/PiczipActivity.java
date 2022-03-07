package com.jiangdg.usbcamera.view;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.application.MyApplication;
import com.jiangdg.usbcamera.utils.FileUtil;
import com.jiangdg.usbcamera.utils.GlideEngine;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.listener.OnResultCallbackListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 采集视频：
 * 采集图片：
 * 录入信息：
 * 压缩上传
 * 死牛测长估重原有流程进行改造，拍摄完视频后先保存到本地，命名为：xxx/时间戳/video.mp4,然后跳转影像拍摄界面;
 * 影像界面设计见附件；胸围命名为：1.jpg、胸宽命名为2.jpg、体斜长命名为：3.jpg；顶部为测量值，只能输入数字，保留两位小数；文件也保存到“xxx/时间戳/”目录下；
 * 拍摄完图片后在拍摄的图片右上角增加删除按钮，删除成功后可以继续拍摄；
 * 所有影像拍摄完成后，点击保存后将video.mp4、1.jpg、2.jpg、3.jpg以及weight.txt保存到一个zip中存储在本地，文件命名为：时间戳.zip；
 * weight.txt为json格式[{"name":"1.jpg","val":14.31},{"name":"2.jpg","val":14.31},{"name":"3.jpg","val":14.31},{"name":"video.mp4","val":}]
 * by wenhaiyang 211223
 */
public class PiczipActivity extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.edit_chest_bust)
    EditText editChestBust;
    @BindView(R.id.iv_chest_bust)
    ImageView ivChestBust;
    @BindView(R.id.btn_chest_bust)
    ImageView btnChestBust;
    @BindView(R.id.edit_chest_width)
    EditText editChestWidth;
    @BindView(R.id.iv_chest_width)
    ImageView ivChestWidth;
    @BindView(R.id.btn_chest_width)
    ImageView btnChestWidth;
    @BindView(R.id.edit_body_length)
    EditText editBodyLength;
    @BindView(R.id.iv_body_length)
    ImageView ivBodyLength;
    @BindView(R.id.btn_body_length)
    ImageView btnBodyLength;
    @BindView(R.id.btn_zip)
    Button btnZip;
    @BindView(R.id.btn_chest_bust_rm)
    ImageView btnBustRm;
    @BindView(R.id.btn_chest_width_rm)
    ImageView btnWidthRm;
    @BindView(R.id.btn_body_length_rm)
    ImageView btnBodyRm;
    @BindView(R.id.btn_vadio)
    ImageView btnVadio;
    @BindView(R.id.btn_clean)
    Button btnClean;
    @BindView(R.id.btn_vadio_rm)
    ImageView btnVadioRm;
    @BindView(R.id.btn_list)
    TextView btnList;

    private final int PICTYPE_BUST = 1;
    private final int PICTYPE_WIDTH = 2;
    private final int PICTYPE_BODY = 3;
    private final int PICTYPE_VADIO = 4;

    private String picBustPath = "";
    private String picWidthPath = "";
    private String picBodyPath = "";
    private String vadioPath = "";
    private String parentPath = "";

    private void clean() {
        removePicture(PICTYPE_BUST);
        removePicture(PICTYPE_WIDTH);
        removePicture(PICTYPE_BODY);
        Glide.with(this).load(R.mipmap.ic_add_pic).centerCrop().into(btnVadio);
        FileUtils.delete(parentPath);

        picBustPath = "";
        picWidthPath = "";
        picBodyPath = "";
        vadioPath = "";
        parentPath = "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piczip);
        ButterKnife.bind(this);
        toolbar.setNavigationOnClickListener(v -> finish());
        btnChestWidth.setOnClickListener(this);
        btnChestBust.setOnClickListener(this);
        btnBodyLength.setOnClickListener(this);
        btnZip.setOnClickListener(this);
        btnVadio.setOnClickListener(this);
        btnVadioRm.setOnClickListener(this);

        btnBustRm.setOnClickListener(this);
        btnWidthRm.setOnClickListener(this);
        btnBodyRm.setOnClickListener(this);
        btnClean.setOnClickListener(this);
        btnList.setOnClickListener(this);

        editBodyLength.addTextChangedListener(new CustTextWatcher(editBodyLength, 2));
        editChestWidth.addTextChangedListener(new CustTextWatcher(editChestWidth, 2));
        editChestBust.addTextChangedListener(new CustTextWatcher(editChestBust, 2));
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_vadio:
                takeVadio();
                break;
            case R.id.btn_list:
                startActivity(new Intent(this, FileListActivity.class));
                break;
            case R.id.btn_clean:
                clean();
                break;
            case R.id.btn_chest_bust:
                tackPicture(PICTYPE_BUST);
                break;
            case R.id.btn_chest_width:
                tackPicture(PICTYPE_WIDTH);
                break;
            case R.id.btn_body_length:
                tackPicture(PICTYPE_BODY);
                break;
            case R.id.btn_chest_bust_rm:
                removePicture(PICTYPE_BUST);
                break;
            case R.id.btn_chest_width_rm:
                removePicture(PICTYPE_WIDTH);
                break;
            case R.id.btn_body_length_rm:
                removePicture(PICTYPE_BODY);
                break;
            case R.id.btn_vadio_rm:
                Glide.with(this).load(R.mipmap.ic_add_pic).centerCrop().into(btnVadio);
                break;
            case R.id.btn_zip:
                zipValue();
                break;
        }
    }

    private void takeVadio() {
        if (parentPath.isEmpty())
            parentPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos/" + System.currentTimeMillis();
        Intent intent = new Intent(this, USBCameraActivity.class);
        intent.putExtra("path", parentPath);
        startActivityForResult(intent, PICTYPE_VADIO);
    }

    /**
     * 拍照
     *
     * @param picType
     */
    private void tackPicture(int picType) {
        if (parentPath.isEmpty())
            parentPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME + "/videos/" + System.currentTimeMillis();

//        Intent intent = new Intent(this, USBCameraToolActivity.class);
//        intent.putExtra("path", parentPath + "/" + picType + ".jpg");
//        startActivityForResult(intent, picType);


        PictureSelector.create(this)
                .openGallery(PictureMimeType.ofImage())
                .imageEngine(GlideEngine.createGlideEngine())
                .isCompress(true)
//                .imageFormat(PictureMimeType.PNG)
                .selectionMode(PictureConfig.SINGLE)
//                .minimumCompressSize(1000)
                .forResult(new OnResultCallbackListener<LocalMedia>() {
                    @Override
                    public void onResult(List<LocalMedia> result) {
                        if (ObjectUtils.isNotEmpty(result)) {
                            LocalMedia image = result.get(0);
                            String path = image.getRealPath();
                            Uri build = Uri.fromFile(new File(path));
                            switch (picType) {
                                case PICTYPE_BUST:
                                    picBustPath = path;
                                    Glide.with(btnChestBust).load(build).centerInside().into(btnChestBust);
                                    break;
                                case PICTYPE_WIDTH:
                                    picWidthPath = path;
                                    Glide.with(btnChestWidth).load(build).centerInside().into(btnChestWidth);
                                    break;
                                case PICTYPE_BODY:
                                    picBodyPath = path;
                                    Glide.with(btnChestWidth).load(build).centerInside().into(btnBodyLength);
                                    break;
                            }
                        } else {
//                            ToastUtils.error("拍照数据为空");
                        }
                    }

                    @Override
                    public void onCancel() {
                        LogUtils.e("拍照取消");
                    }
                });
    }

    /**
     * 删除图片
     *
     * @param picType
     */
    private void removePicture(int picType) {
        switch (picType) {
            case PICTYPE_BUST:
                FileUtils.delete(picBustPath);
                Glide.with(this).load(R.mipmap.ic_add_pic).centerCrop().into(btnChestBust);
                editChestBust.setText("");
                picBustPath = "";
                break;
            case PICTYPE_WIDTH:
                FileUtils.delete(picWidthPath);
                editChestWidth.setText("");
                Glide.with(this).load(R.mipmap.ic_add_pic).centerCrop().into(btnChestWidth);
                picWidthPath = "";
                break;
            case PICTYPE_BODY:
                FileUtils.delete(picBodyPath);
                editBodyLength.setText("");
                Glide.with(btnBodyLength).load(R.mipmap.ic_add_pic).centerCrop().into(btnBodyLength);
                picBodyPath = "";
                break;
        }
    }

    /**
     * 打包资料
     */
    private void zipValue() {
        if (editChestBust.getText().toString().isEmpty() ||
                editChestWidth.getText().toString().isEmpty() ||
                editBodyLength.getText().toString().isEmpty() ||
                picBustPath.isEmpty() ||
                picWidthPath.isEmpty() ||
                vadioPath.isEmpty() ||
                picBodyPath.isEmpty()
        ) {
            ToastUtils.showShort("请完善信息");
            return;
        }


        ToastUtils.showLong("开始压缩，请稍等！");

        new Thread(new Runnable() {
            @Override
            public void run() {
                JsonArray array = new JsonArray();
                JsonObject obj1 = new JsonObject();
                obj1.addProperty("val", editChestBust.getText().toString().trim());
                obj1.addProperty("name", "1.jpg");
                array.add(obj1);
                JsonObject obj2 = new JsonObject();
                obj2.addProperty("val", editChestWidth.getText().toString().trim());
                obj2.addProperty("name", "2.jpg");
                array.add(obj2);
                JsonObject obj3 = new JsonObject();
                obj3.addProperty("val", editBodyLength.getText().toString().trim());
                obj3.addProperty("name", "3.jpg");
                array.add(obj3);
                JsonObject obj = new JsonObject();
                obj.addProperty("val", "");
                obj.addProperty("name", "video.mp4");
                array.add(obj);
                String string = array.toString();
                FileUtil.createfile(parentPath + "/weight.txt");
                FileUtil.putFileStream(string.getBytes());
                FileUtil.releaseFile();


                List<File> files = FileUtils.listFilesInDir(parentPath);
                files.add(new File(picBustPath));
                files.add(new File(picWidthPath));
                files.add(new File(picBodyPath));
                String zipFile = parentPath + "/" + System.currentTimeMillis() + String.format(getString(R.string.fmt_zip_name), editChestBust.getText().toString().trim(), editChestWidth.getText().toString().trim(), editBodyLength.getText().toString().trim());
//                ToastUtils.showLong(parentPath + "     " + files.size());

                zipFile(files, zipFile);
                ToastUtils.showLong("打包完成，可以上传");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(PiczipActivity.this, FileListActivity.class));
                    }
                });


            }
        }).start();
    }


    /**
     * 压缩文件
     *
     * @param fileList
     * @param destFile
     */
    private void zipFile(List<File> fileList, String destFile) {
        try (
                //创建zip输出流
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(destFile));
                //创建缓冲输出流
                BufferedOutputStream bos = new BufferedOutputStream(out);
        ) {
            out.setComment("UTF-8");
            for (File sourceFile : fileList) {
                if (sourceFile.isDirectory() || sourceFile.getName().endsWith(".zip")) continue;
                out.putNextEntry(new ZipEntry(sourceFile.getName()));
                FileInputStream fos = new FileInputStream(sourceFile);
                BufferedInputStream bis = new BufferedInputStream(fos);
                int tag;
//                System.out.println(base);
                //将源文件写入到zip文件中
                while ((tag = bis.read()) != -1) {
                    bos.write(tag);
                }
                bos.flush();
                fos.close();
                bis.close();
            }

        } catch (IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtils.showLong(e.getMessage());
                }
            });
            FileUtils.delete(destFile);
            e.printStackTrace();
        }
    }


    @SuppressLint("CheckResult")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            String path = data.getStringExtra("path");
            Uri build;
//            Uri build = Uri.parse(path);
//            Uri build = new Uri.Builder().appendEncodedPath(path).build();
            if (requestCode == PICTYPE_VADIO) {
                build = Uri.fromFile(new File(path));
                vadioPath = path;
                Glide.with(this).asBitmap().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).load(build).centerInside().into(btnVadio);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    class CustTextWatcher implements TextWatcher {
        private EditText edit;
        private int point = 2;

        public CustTextWatcher(EditText edit, int point) {
            this.edit = edit;
            this.point = point;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence content, int start, int before, int count) {
            try {
                if (content != null && !content.toString().isEmpty()) {
                    String number = content.toString();
                    Log.e("onTextChanged", number);
                    if (number.contains(".")) {
                        if (number.length() - number.indexOf(".") - 1 > point) {
                            edit.setTextKeepState(number.substring(0, number.indexOf(".") + point + 1));
                        }
                    }
                }

            } catch (Exception e) {
                edit.setTextKeepState("0");
                e.printStackTrace();
            }
        }

        @Override
        public void afterTextChanged(Editable content) {

        }
    }
}


