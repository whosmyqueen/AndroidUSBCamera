package com.jiangdg.usbcamera;

import java.io.Serializable;

import lombok.Data;

@Data
public class UploadResultBean implements Serializable {
    private static final long serialVersionUID = 4563219810861568664L;
    private int status;
}
