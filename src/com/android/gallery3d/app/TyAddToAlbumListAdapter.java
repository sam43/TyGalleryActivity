/*
 * TIANYU: yuxin add for New Design Gallery
 */
package com.android.gallery3d.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import com.android.gallery3d.R;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.TyAddToAlbumSetListSlidingWindow;
import com.android.gallery3d.ui.TyAddToAlbumSetListSlidingWindow.AlbumSetEntry;
import android.util.Log;
//TY liuyuchuan  add begin for PROD103692994
import com.android.gallery3d.data.MediaObject;
//TY liuyuchuan  add end for PROD103692994

public class TyAddToAlbumListAdapter extends BaseAdapter{
    private final static int SECTION_ONE = 0;
    private final static int SECTION_TWO = 1;
    private TyAddToAlbumSetListSlidingWindow mDataWindow;
    private int mDataCount;
    private SelectionManager mSelectionManage;
    private LayoutInflater mInflater;
    
    private class AlbumView {
        public TextView mName;
        public TextView mCount;
        public CheckBox mMultiSelect;
        public ImageView mDrag;
        public ImageView mPortCoverBg;
        public View mPortCoverView;//TY liuyuchuan  add for PROD103692994

    }
    
    public TyAddToAlbumListAdapter(Context context, SelectionManager selectionmanage) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSelectionManage = selectionmanage;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null){
            convertView = getCustomView(parent);
        }
        makeCustomView(position, convertView);
        return convertView;
    }

    private View getCustomView(ViewGroup parent){
        View customView = mInflater.inflate(R.layout.ty_albumlist_pic, parent, false);

        AlbumView holder = new AlbumView();
        holder.mPortCoverBg = (ImageView) customView.findViewById(R.id.ty_layout_port_one);
        //TY liuyuchuan  add begin for PROD103692994
        holder.mPortCoverView = (View)customView.findViewById(R.id.ty_layout_port_two);
        //TY liuyuchuan  add end for PROD103692994

        holder.mCount = (TextView) customView.findViewById(R.id.count);
        holder.mName = (TextView) customView.findViewById(R.id.albumpath);
        holder.mMultiSelect = (CheckBox)customView.findViewById(R.id.ty_select_box);
        holder.mDrag = (ImageView) customView.findViewById(R.id.drag_handle);

        customView.setTag(holder);
        return customView;
    }

    private void makeCustomView(final int position, final View customView){
        AlbumView holder = (AlbumView) customView.getTag();
        AlbumSetEntry entry = mDataWindow.getNoCheck(position);
        if (entry == null){
            holder.mMultiSelect.setVisibility(View.GONE);
            holder.mPortCoverBg.setImageResource(R.drawable.ty_ic_loading_album); 
            holder.mPortCoverBg.setVisibility(View.VISIBLE);
            holder.mCount.setText("0");
            holder.mDrag.setVisibility(View.GONE);
        }else{
            //TY liuyuchuan  add begin for PROD103692994
            if(entry.MEDIA_TYPE_VIDEO == MediaObject.MEDIA_TYPE_VIDEO){
                holder.mPortCoverView.setVisibility(View.VISIBLE);

            }else{
                holder.mPortCoverView.setVisibility(View.GONE);

            }
            //TY liuyuchuan  add end for PROD103692994
            holder.mName.setText(entry.title);
            holder.mMultiSelect.setVisibility(View.GONE);
            if(entry.bitmap == null){
                holder.mPortCoverBg.setImageResource(R.drawable.ty_ic_loading_album); 
            }else{
                holder.mPortCoverBg.setImageBitmap(entry.bitmap);
            }
            holder.mPortCoverBg.setVisibility(View.VISIBLE);
            holder.mDrag.setVisibility(View.GONE);
            holder.mCount.setText(String.valueOf(entry.totalCount));
        }
    }

    @Override
    public int getCount() {        
        return mDataCount;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }


    @Override
    public Object getItem(int position) {
        return mDataWindow.getNoCheck(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setDataWindow(TyAddToAlbumSetListSlidingWindow dataWindow) {
        mDataWindow = dataWindow;
        if (mDataWindow == null){
            mDataCount = 0;
        }
        mDataCount = mDataWindow.size();
    }

    public void setDataCount(int dataCount) {
        mDataCount = dataCount;
    }
}
