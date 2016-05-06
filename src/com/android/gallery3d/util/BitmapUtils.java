package com.android.gallery3d.util;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by taoxj on 16-5-3.
 */
public class BitmapUtils {

    public static String bitmapToBase64(Bitmap bitmap){

        String result="";

        ByteArrayOutputStream bos=null;

        try {

            if(null!=bitmap){

                bos=new ByteArrayOutputStream();

                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);//��bitmap�����ֽ���������


                bos.flush();//��bos���������ڴ��е�����ȫ���������ջ���

                bos.close();


                byte []bitmapByte=bos.toByteArray();

                result= Base64.encodeToString(bitmapByte, Base64.DEFAULT);

            }

        } catch (Exception e) {

            e.printStackTrace();

        }finally{

            if(null!=null){

                try {

                    bos.close();

                } catch (IOException e) {

                    e.printStackTrace();

                }

            }

        }

        return result;

    }
}
