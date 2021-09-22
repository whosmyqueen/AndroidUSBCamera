package com.jiangdg.usbcamera.adapter.bean;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileListBean implements Serializable {

    private static final long serialVersionUID = 7914173928926261300L;
    private String fileName;
    private String filePath;
}
