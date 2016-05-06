package com.android.gallery3d.parser;

import com.android.gallery3d.api.InstagramResp;
import com.android.gallery3d.database.PictureDetail;
import com.android.gallery3d.database.Pictures;

import java.util.List;

/**
 * Created by taoxj on 16-4-28.
 */
public class PictureListResp implements InstagramResp {
    private List<PictureDetail> picturesList;

    public List<PictureDetail> getPicturesList() {
        return picturesList;
    }

    public void setPicturesList(List<PictureDetail> picturesList) {
        this.picturesList = picturesList;
    }
}
