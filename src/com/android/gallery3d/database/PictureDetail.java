package com.android.gallery3d.database;

import android.net.Uri;

/**
 * Created by taoxj on 16-4-27.
 *
 */
public class PictureDetail {
    //id
    private int id;
    //cloud id
    private String pictureId;
    //url
    private String url;
    //user photo url
    private String userPhotoUrl;
    //take picture time
    private long takePictureTime;
    //isDeleted
    private int isDeleted; //
    //uri
    private Uri uri;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //file path
    private String path;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    //image type
    private String type;

    @Override
    public String toString() {
        return "PictureDetail{" +
                "id=" + id +
                ", pictureId='" + pictureId + '\'' +
                ", url='" + url + '\'' +
                ", userPhotoUrl='" + userPhotoUrl + '\'' +
                ", takePictureTime='" + takePictureTime + '\'' +
                ", isDeleted=" + isDeleted +
                ",uri=" + uri +
                ",path=" + path +
                ",type=" + type +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPictureId() {
        return pictureId;
    }

    public void setPictureId(String pictureId) {
        this.pictureId = pictureId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserPhotoUrl() {
        return userPhotoUrl;
    }

    public void setUserPhotoUrl(String userPhotoUrl) {
        this.userPhotoUrl = userPhotoUrl;
    }

    public long getTakePictureTime() {
        return takePictureTime;
    }

    public void setTakePictureTime(long takePictureTime) {
        this.takePictureTime = takePictureTime;
    }

    public int getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(int isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
