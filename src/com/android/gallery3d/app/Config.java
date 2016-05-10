/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;

import com.android.gallery3d.R;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.SlotView;
//TIANYU: yuxin add begin for New Design Gallery
import com.android.gallery3d.ui.TyAlbumTimeSlotRenderer;
import com.android.gallery3d.ui.TySlotView;
//TIANYU: yuxin add end for New Design Gallery

final class Config {
    public static class AlbumSetPage {
        private static AlbumSetPage sInstance;

        public SlotView.Spec slotViewSpec;
        public AlbumSetSlotRenderer.LabelSpec labelSpec;
        public int paddingTop;
        public int paddingBottom;
        public int placeholderColor;

        public static synchronized AlbumSetPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumSetPage(context);
            }
            return sInstance;
        }

        private AlbumSetPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.albumset_placeholder);

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.albumset_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.ty_albumset_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.albumset_slot_gap);
            slotViewSpec.slotHeightAdditional = 0;

            paddingTop = r.getDimensionPixelSize(R.dimen.albumset_padding_top);
            paddingBottom = r.getDimensionPixelSize(R.dimen.albumset_padding_bottom);

            labelSpec = new AlbumSetSlotRenderer.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(
                    R.dimen.albumset_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(
                    R.dimen.albumset_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(
                    R.dimen.albumset_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(
                    R.dimen.albumset_title_font_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(
                    R.dimen.albumset_count_font_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(
                    R.dimen.albumset_left_margin);
            labelSpec.titleRightMargin = r.getDimensionPixelSize(
                    R.dimen.albumset_title_right_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(
                    R.dimen.albumset_icon_size);
            labelSpec.backgroundColor = r.getColor(
                    R.color.albumset_label_background);
            labelSpec.titleColor = r.getColor(R.color.albumset_label_title);
            labelSpec.countColor = r.getColor(R.color.albumset_label_count);
        }
    }

    public static class AlbumPage {
        private static AlbumPage sInstance;

        public SlotView.Spec slotViewSpec;
        public int placeholderColor;

        public static synchronized AlbumPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumPage(context);
            }
            return sInstance;
        }

        private AlbumPage(Context context) {
            Resources r = context.getResources();

            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.placeholderPageGap = r.getDimensionPixelSize(R.dimen.ty_album_page_gap);
            slotViewSpec.rowsLand = r.getInteger(R.integer.ty_album_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.ty_album_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.ty_album_slot_gap);
            /*TIANYU: liuyuchuan modify begin for PROD103682575
                    Because  value of SlotView---mSlotWidth  calculation of the error of A pixel, 
                    margin display is not correct, the value of slotViewSpec.slotGapEdge  instead of 0 to 1*/
            slotViewSpec.slotGapEdge = 1;
            /*TIANYU: liuyuchuan modify end for PROD103682575*/
        }
        
        public void setMarin(int marinTop, int marinBottom){
            if (slotViewSpec.marinTop != marinTop
                || slotViewSpec.marinBottom != marinBottom){
                slotViewSpec.marinTop = marinTop;
                slotViewSpec.marinBottom = marinBottom;
                slotViewSpec.layoutChange = true;
            }
        }
    }

    //TIANYU: yuxin add begin for New Design Gallery
    public static class  TimeGroupPage {
        private static TimeGroupPage sInstance;

        public TySlotView.Spec slotViewSpec;
        public TyAlbumTimeSlotRenderer.TitleSpec titleSpec;
        public int placeholderTitleColor;
        public int placeholderColor;

        public static synchronized TimeGroupPage get(Context context) {
            if (sInstance == null) {
                sInstance = new TimeGroupPage(context);
            }
            return sInstance;
        }

        private TimeGroupPage(Context context) {
            Resources r = context.getResources();
            placeholderTitleColor = r.getColor(R.color.album_title_placeholder);
            placeholderColor = r.getColor(R.color.album_placeholder);

            slotViewSpec = new TySlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.ty_album_timegroup_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.ty_album_timegroup_rows_port);
            slotViewSpec.areaVSize = r.getInteger(R.integer.ty_album_timegroup_area_vsize);
            slotViewSpec.areaHSize = r.getInteger(R.integer.ty_album_timegroup_area_hsize);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.ty_album_slot_gap);
            slotViewSpec.slotGapEdge = r.getDimensionPixelSize(R.dimen.ty_album_slot_gap_edge);
            slotViewSpec.titleHeight = r.getDimensionPixelSize(R.dimen.album_title_background_height);
            //taoxj add 
            slotViewSpec.slotRowMargin = r.getDimensionPixelSize(R.dimen.ty_album_timegroup_slot_margin);
            slotViewSpec.separatorLineHeight = r.getDimensionPixelSize(R.dimen.ty_album_timegroup_separatorline_height);
            slotViewSpec.slotWidth = r.getDimensionPixelSize(R.dimen.ty_album_timegroup_slot_width);
            slotViewSpec.slotHeight = r.getDimensionPixelSize(R.dimen.ty_album_timegroup_slot_height);
            

            titleSpec = new TyAlbumTimeSlotRenderer.TitleSpec();
            titleSpec.titleBackgroundHeight = slotViewSpec.titleHeight;
            titleSpec.titleFontSize = r.getDimensionPixelSize(
                    R.dimen.album_title_font_size);
            titleSpec.countFontSize = r.getDimensionPixelSize(
                    R.dimen.album_count_font_size);
            titleSpec.fontBottomMargin = r.getDimensionPixelSize(
                    R.dimen.album_title_font_bottom_margin);
            //taoxj add 
            titleSpec.fontTopMargin = r.getDimensionPixelSize(
                    R.dimen.album_title_font_top_margin);
            titleSpec.fontCountBottomMargin = r.getDimensionPixelSize(
                    R.dimen.album_title_count_bottom_margin);
            titleSpec.titleLeftMargin = r.getDimensionPixelSize(
                    R.dimen.album_title_left_margin);
            titleSpec.titleRightMargin = r.getDimensionPixelSize(
                    R.dimen.album_title_right_margin);
            titleSpec.backgroundColor = r.getColor(
                    R.color.album_title_background);
            titleSpec.titleColor = r.getColor(R.color.album_title_title);
            titleSpec.countColor = r.getColor(R.color.album_title_count);
        }

        public void setMarin(int marinTop, int marinBottom){
            android.util.Log.i("koala", "config setMargin = " + slotViewSpec.marinTop + "," + slotViewSpec.marinBottom + ","
            + marinTop + "," + marinBottom);
            if (slotViewSpec.marinTop != marinTop
                || slotViewSpec.marinBottom != marinBottom){
                slotViewSpec.marinTop = marinTop;
                slotViewSpec.marinBottom = marinBottom;
                slotViewSpec.layoutChange = true;
            }
        }
    }
    //TIANYU: yuxin add end for New Design Gallery

    public static class ManageCachePage extends AlbumSetPage {
        private static ManageCachePage sInstance;

        public final int cachePinSize;
        public final int cachePinMargin;

        public static synchronized ManageCachePage get(Context context) {
            if (sInstance == null) {
                sInstance = new ManageCachePage(context);
            }
            return sInstance;
        }

        public ManageCachePage(Context context) {
            super(context);
            Resources r = context.getResources();
            cachePinSize = r.getDimensionPixelSize(R.dimen.cache_pin_size);
            cachePinMargin = r.getDimensionPixelSize(R.dimen.cache_pin_margin);
        }
    }
}

