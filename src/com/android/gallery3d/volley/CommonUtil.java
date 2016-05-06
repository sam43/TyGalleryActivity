package com.android.gallery3d.volley;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.annotation.SuppressLint;

@SuppressLint("SimpleDateFormat")
public class CommonUtil {

    public static byte[] GzipCompress(String input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = null;
        try {
            gzos = new GZIPOutputStream(baos);
            gzos.write(input.getBytes("UTF-8"));
        } finally {
            if (gzos != null) try {
                gzos.close();
            } catch (IOException ignore) {};
        }
        return baos.toByteArray();
    }

    public static String GzipExtract(byte[] input) throws IOException {
        String result = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(input);
        GZIPInputStream gzis = null;
        StringWriter writer = null;
        InputStreamReader reader = null;
        try {
            gzis = new GZIPInputStream(bais);
            reader = new InputStreamReader(gzis, "UTF-8");
            writer = new StringWriter();

            char[] buffer = new char[1024];
            for (int length = 0; (length = reader.read(buffer)) > 0;) {
                writer.write(buffer, 0, length);
            }
            result = writer.toString();
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException ignore) {};
            if (reader != null) try {
                reader.close();
            } catch (IOException ignore) {};
            if (gzis != null) try {
                gzis.close();
            } catch (IOException ignore) {};
        }
        return result;
    }

    public static String getErrorInfo(Throwable th) {
        Writer writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        th.printStackTrace(pw);
        pw.close();
        return writer.toString();
    }

    public static String getNowTimeString() {
        Date date = new Date();
        SimpleDateFormat localSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return localSimpleDateFormat.format(date);
    }
    
    
   
}
