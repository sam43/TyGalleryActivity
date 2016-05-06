package com.android.gallery3d.parser;

import com.android.gallery3d.api.InstagramResp;

/**
 * Created by taoxj on 16-5-3.
 */
public class UploadResp implements InstagramResp {
    private String pictureId;

    private boolean isSuccess;

    public String getPictureId() {
        return pictureId;
    }

    public void setPictureId(String pictureId) {
        this.pictureId = pictureId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
