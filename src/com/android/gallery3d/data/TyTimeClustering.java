/*
 * TIANYU: yuxin add for New Design Gallery
 */

package com.android.gallery3d.data;

import android.content.Context;
import com.android.gallery3d.common.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.LinkedHashMap;
import com.android.gallery3d.R;
//TY wb034 add begin for tygallery
import com.android.gallery3d.app.GalleryApp;
//TY wb034  add end for tygallery

//taoxj add begin
import java.text.SimpleDateFormat;
import java.util.Date;
//taoxj add end

public class TyTimeClustering extends Clustering {
    @SuppressWarnings("unused")
    private static final String TAG = "TyTimeClustering";

    private Context mContext;
    private ArrayList<Cluster> mClusters;
    private String[] mNames;
    private Cluster mCurrCluster;

    private ArrayList<Cluster> mTimeslotClusters;
    private Cluster mTodayCluster;
    private Cluster mYesterdayCluster;
    private Cluster mWeekCluster;
    private Cluster mMonthCluster;
    private Cluster mMonth3Cluster;
    private Cluster mMonth6Cluster;
    private Cluster mAgodayCluster;
    //taoxj modify
    private LinkedHashMap<String, Integer> mTimeslotInfo;
    
    private int mTodayTime;
    //TY wb034 add begin for tygallery
    private GalleryApp mApplication;
    private DataManager dataManager;
    //TY wb034 add end for tygallery
 
    //taoxj add begin
    private Cluster currentCluster;
    //taoxj add end


    private static final Comparator<SmallItem> sDateComparator =
            new DateComparator();

    private static class DateComparator implements Comparator<SmallItem> {
        @Override
        public int compare(SmallItem item1, SmallItem item2) {
            return -Utils.compare(item1.dateInMs, item2.dateInMs);
        }
    }
    //TY wb034 20150306 modify begin for tygallery
    //public TyTimeClustering(Context context) {
    public TyTimeClustering(GalleryApp application,Context context) {
    //TY wb034 20150306 modify end for tygallery
        mContext = context;
        //TY wb034 20150306 add begin for tygallery
        mApplication = application;
        dataManager = mApplication.getDataManager();
        //TY wb034 20150306 add end for tygallery
        mClusters = new ArrayList<Cluster>();
        mCurrCluster = new Cluster();
        
        mTimeslotClusters = new ArrayList<Cluster>();
        //taoxj remove
        /*mTodayCluster = new Cluster();
        mTodayCluster.mTitleString = R.string.ablum_title_today;
        mYesterdayCluster = new Cluster();
        mYesterdayCluster.mTitleString = R.string.ablum_title_yesterday;
        mWeekCluster = new Cluster();
        mWeekCluster.mTitleString = R.string.ablum_title_week;
        mMonthCluster = new Cluster();
        mMonthCluster.mTitleString = R.string.ablum_title_month;
        mMonth3Cluster = new Cluster();
        mMonth3Cluster.mTitleString = R.string.ablum_title_month3;
        mMonth6Cluster = new Cluster();
        mMonth6Cluster.mTitleString = R.string.ablum_title_month6;
        mAgodayCluster = new Cluster();
        mAgodayCluster.mTitleString = R.string.ablum_title_agoday;*/
    }

    @Override
    public void run(MediaSet baseSet) {
        final int total = baseSet.getTotalMediaItemCount();
        final SmallItem[] buf = new SmallItem[total];
        final double[] latLng = new double[2];
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (index < 0 || index >= total) return;
                if (item == null) return;
                SmallItem s = new SmallItem();
                s.path = item.getPath();
                //s.dateInMs = item.getDateModifiedInSec()*1000;
                s.dateInMs = item.getDateInMs();
                item.getLatLong(latLng);
                s.lat = latLng[0];
                s.lng = latLng[1];
                buf[index] = s;
            }
        });
        ArrayList<SmallItem> items = new ArrayList<SmallItem>();
        for (int i = 0; i < total; i++) {
            if (buf[i] != null) {
                items.add(buf[i]);
            }
        }

        Collections.sort(items, sDateComparator);

        int n = items.size();
        long minTime = 0;
        long maxTime = 0;
        for (int i = 0; i < n; i++) {
            long t = items.get(i).dateInMs;
            if (t == 0) continue;
            if (minTime == 0) {
                minTime = maxTime = t;
            } else {
                minTime = Math.min(minTime, t);
                maxTime = Math.max(maxTime, t);
            }
        }
        mTimeslotInfo = new LinkedHashMap<String, Integer>();//taoxj add 
        Calendar todayCal = Calendar.getInstance();
        int y = todayCal.get(Calendar.YEAR);
        int m = todayCal.get(Calendar.MONTH);
        int d = todayCal.get(Calendar.DAY_OF_MONTH);
        todayCal.set(y, m, d, 0, 0, 0);
        mTodayTime = (y << 14) + (m << 7) + d;
    	for (int i = 0; i < n; i++) {
            if(items.get(i) != null){
        		mCurrCluster.addItem(items.get(i));
                computeTimesolt(todayCal, items.get(i));
        	}
        }
        mClusters.add(mCurrCluster);
        mTimeslotClusters.add(currentCluster); //taoxj add
        /* taoxj remove
        if (mTodayCluster.size() > 0){
            mTimeslotClusters.add(mTodayCluster);
        }
        if (mYesterdayCluster.size() > 0){
            mTimeslotClusters.add(mYesterdayCluster);
        }
        if (mWeekCluster.size() > 0){
            mTimeslotClusters.add(mWeekCluster);
        }
        if (mMonthCluster.size() > 0){
            mTimeslotClusters.add(mMonthCluster);
        }
        if (mMonth3Cluster.size() > 0){
            mTimeslotClusters.add(mMonth3Cluster);
        }
        if (mMonth6Cluster.size() > 0){
            mTimeslotClusters.add(mMonth6Cluster);
        }
        if (mAgodayCluster.size() > 0){
            mTimeslotClusters.add(mAgodayCluster);
        }*/

        int cSize = mClusters.size();
        mNames = new String[cSize];
        for (int i = 0; i < cSize; i++) {
            mNames[i] = mClusters.get(i).generateCaption(mContext)+i;
        }

        //taoxj modify
        /*cSize = mTimeslotClusters.size();
        if (cSize > 0){
            mTimeslotInfo = new LinkedHashMap<Integer, Integer>();
            Cluster tempCluster = null;
            for (int i = 0; i < cSize; i++) {
                tempCluster = mTimeslotClusters.get(i);
                mTimeslotInfo.put(tempCluster.mTitleString, tempCluster.size());
            }
        }*/
    }

    @Override
    public int getNumberOfClusters() {
        return mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        ArrayList<SmallItem> items = mClusters.get(index).getItems();
        ArrayList<Path> result = new ArrayList<Path>(items.size());
        for (int i = 0, n = items.size(); i < n; i++) {
            result.add(items.get(i).path);
        }
        return result;
    }

    @Override
    public String getClusterName(int index) {
        return mNames[index];
    }

    public int getTodayTime() {
        return mTodayTime;
    }

    private void computeTimesolt(Calendar todayCal, SmallItem currentItem) {
        if (todayCal == null || currentItem == null){
            return;
        }
        //taoxj modify begin
        /*Calendar itemCal = Calendar.getInstance();
        itemCal.setTimeInMillis(currentItem.dateInMs);
        
        Calendar yesterdayCal = (Calendar)todayCal.clone();
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);

        Calendar weekCal = (Calendar)todayCal.clone();
        weekCal.add(Calendar.WEEK_OF_MONTH, -1);

        Calendar monthCal = (Calendar)todayCal.clone();
        monthCal.add(Calendar.MONTH, -1);

        Calendar month3Cal = (Calendar)todayCal.clone();
        month3Cal.add(Calendar.MONTH, -3);
        
        Calendar month6Cal = (Calendar)todayCal.clone();
        month6Cal.add(Calendar.MONTH, -6);

        if (todayCal.before(itemCal)){
            mTodayCluster.addItem(currentItem);
        }else if (yesterdayCal.before(itemCal)){
            mYesterdayCluster.addItem(currentItem);
        }else if (weekCal.before(itemCal)){
            mWeekCluster.addItem(currentItem);
        }else if (monthCal.before(itemCal)){
            mMonthCluster.addItem(currentItem);
        }else if (month3Cal.before(itemCal)){
            mMonth3Cluster.addItem(currentItem);
        }else if (month6Cal.before(itemCal)){
            mMonth6Cluster.addItem(currentItem);
        }else{
            mAgodayCluster.addItem(currentItem);
        }*/

        long itemTime = currentItem.dateInMs;
        Date d = new Date();
        d.setTime(itemTime);
        Date now = new Date();
        int now_year = now.getYear();
        SimpleDateFormat myFmt = null;
        //is this year?
        if(d.getYear() == now_year){
	   myFmt = new SimpleDateFormat(mContext.getString(R.string.timegroup_dateformate_without_year));
	}else{
	 myFmt = new SimpleDateFormat(mContext.getString(R.string.timegroup_dateformate_with_year));
	}
        String itemClusterTitle = myFmt.format(d);
        //is today or yesterday ?
        Calendar itemCal = Calendar.getInstance();
        itemCal.setTimeInMillis(currentItem.dateInMs);
        
        Calendar yesterdayCal = (Calendar)todayCal.clone();
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);  
         if (todayCal.before(itemCal)){
            itemClusterTitle = mContext.getString(R.string.ablum_title_today);
        }else if (yesterdayCal.before(itemCal)){
            itemClusterTitle = mContext.getString(R.string.ablum_title_yesterday);
        }      

        if(mTimeslotInfo.containsKey(itemClusterTitle)){
        	int size = mTimeslotInfo.get(itemClusterTitle) + 1;
        	mTimeslotInfo.put(itemClusterTitle, size);
        	currentCluster.addItem(currentItem);
        }else{
        	Cluster tempCluster = new Cluster();
        	tempCluster.mTitleString = itemClusterTitle;
        	tempCluster.addItem(currentItem);
        	if(currentCluster != null && currentCluster.size() != 0)
        	 mTimeslotClusters.add(currentCluster);
        	currentCluster = tempCluster;
        	mTimeslotInfo.put(itemClusterTitle, 1);       	
        }
        //taoxj modify end
    }
    
    //taoxj modify to String
    @Override
    public LinkedHashMap<String, Integer> getTimeslotInfo() {
        return mTimeslotInfo;
    }

    private static class SmallItem {
        Path path;
        long dateInMs;
        double lat, lng;
    }

    private static class Cluster {
        @SuppressWarnings("unused")
        private static final String TAG = "Cluster";
        public String mTitleString; //taoxj modify

        private ArrayList<SmallItem> mItems = new ArrayList<SmallItem>();

        public Cluster() {
        }

        public void addItem(SmallItem item) {
            mItems.add(item);
        }

        public int size() {
            return mItems.size();
        }

        public SmallItem getLastItem() {
            int n = mItems.size();
            return (n == 0) ? null : mItems.get(n - 1);
        }

        public ArrayList<SmallItem> getItems() {
            return mItems;
        }

        public String generateCaption(Context context) {
            return "TyTimeClustering";
        }
    }
}
