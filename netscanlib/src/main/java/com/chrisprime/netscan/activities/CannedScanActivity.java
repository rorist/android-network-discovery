/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package com.chrisprime.netscan.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import com.chrisprime.netscan.BuildConfig;
import com.chrisprime.netscan.R;
import com.chrisprime.netscan.utilities.NetScanInitHelper;

public class CannedScanActivity extends Activity {

    public static final String PKG = BuildConfig.APPLICATION_ID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_canned_scan);
        setTitle(R.string.app_loading);
        NetScanInitHelper.initializeNetScan(this);
    }
}
