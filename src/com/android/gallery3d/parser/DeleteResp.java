package com.android.gallery3d.parser;

import com.android.gallery3d.api.InstagramResp;

/**
 * Created by taoxj on 16-5-4.
 */
public class DeleteResp implements InstagramResp {
    public boolean isSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public boolean isSuccess;

}
