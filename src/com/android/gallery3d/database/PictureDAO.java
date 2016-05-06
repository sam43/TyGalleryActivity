package com.android.gallery3d.database;

import android.net.Uri;

import java.util.List;

/**
 * Created by taoxj on 16-4-27.
 */
public interface PictureDAO {
    public void addPicture(PictureDetail picture);
    public void deletePicture(String pictureId);
    public List<PictureDetail> query();
    public void updatePictures(Uri pictureUri);
    public PictureDetail queryByPictureId(String  pid);
    public List<PictureDetail> queryLocalPic();
    public void updateLocalPicture(PictureDetail pic);
    public PictureDetail queryByUri(String uri);
    public void modifyDeleteStatus(String pictureId);
    public List<PictureDetail> queryDeletedItems();
    public List<String> queryAllPictureIds();
    public void deletePictureByUri(String uri);
    public PictureDetail queryByPath(String path) ;
}
