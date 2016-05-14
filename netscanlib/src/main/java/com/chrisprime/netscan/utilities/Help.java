/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package com.chrisprime.netscan.utilities;

import com.chrisprime.netscan.R;
import android.app.Activity;
import android.os.Bundle;

public class Help extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
    }
}
