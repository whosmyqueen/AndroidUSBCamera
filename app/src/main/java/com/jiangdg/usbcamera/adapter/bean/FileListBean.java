package com.jiangdg.usbcamera.adapter.bean;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileListBean implements Serializable {

    private static final long serialVersionUID = 7914173928926261300L;
    private String fileName;
    private String filePath;
    /**
     * 0 未上传 1 上传中 2上传完成 3 上传失败
     */
    private int status;
    private int progress;

    public FileListBean(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }
}
