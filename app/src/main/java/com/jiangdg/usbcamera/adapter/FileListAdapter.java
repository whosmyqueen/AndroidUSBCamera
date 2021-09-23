package com.jiangdg.usbcamera.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.adapter.bean.FileListBean;
import com.jiangdg.usbcamera.view.CircleProgressView;

import org.jetbrains.annotations.NotNull;

public class FileListAdapter extends BaseQuickAdapter<FileListBean, BaseViewHolder> {

    /**
     * 构造方法，此示例中，在实例化Adapter时就传入了一个List。
     * 如果后期设置数据，不需要传入初始List，直接调用 super(layoutResId); 即可
     */
    public FileListAdapter() {
        super(R.layout.layout_file_list_item);
    }

    /**
     * 在此方法中设置item数据
     */
    @Override
    protected void convert(@NotNull BaseViewHolder helper, @NotNull FileListBean item) {
        helper.setText(R.id.tv_name, item.getFileName());
        helper.setText(R.id.tv_content, item.getFilePath());
        helper.setGone(R.id.tv_status, item.getStatus() == 1);
        String status = "";
        switch (item.getStatus()) {
            case 0:
                status = "未上传";
                break;
            case 1:
                status = "上传中";
                break;
            case 2:
                status = "上传完成";
                break;
            case 3:
                status = "上传失败";
                break;
        }
        helper.setText(R.id.tv_status, status);
        helper.setGone(R.id.pb_progress, item.getStatus() != 1);
        CircleProgressView progressBar = helper.getView(R.id.pb_progress);
        progressBar.setProgress(item.getProgress());
    }
}
