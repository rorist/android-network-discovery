/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.Network;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class HostBean implements Parcelable {

    public static final String PKG = "info.lamatricexiste.network";
    public static final String EXTRA = PKG + ".extra";
    public static final String EXTRA_POSITION = PKG + ".extra_position";
    public static final String EXTRA_HOST = PKG + ".extra_host";
    public static final String EXTRA_TIMEOUT = PKG + ".network.extra_timeout";
    public static final String EXTRA_HOSTNAME = PKG + ".extra_hostname";
    public static final String EXTRA_BANNERS = PKG + ".extra_banners";
    public static final String EXTRA_PORTSO = PKG + ".extra_ports_o";
    public static final String EXTRA_PORTSC = PKG + ".extra_ports_c";
    public static final String EXTRA_SERVICES = PKG + ".extra_services";

    public int isGateway = 0;
    public String ipAddress = null;
    public String hostname = null;
    public String hardwareAddress = "00:00:00:00:00:00";
    public String nicVendor = "Unknown";
    public String os = "Unknown";
    public float responseTime = 0;
    public int position = 0;
    public String[] services = null;
    public String[] banners = null;
    public ArrayList<Integer> portsOpen = null;
    public ArrayList<Integer> portsClosed = null;

    public HostBean() {
        // New object
    }

    public HostBean(Parcel in) {
        // Object from parcel
        readFromParcel(in);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(isGateway);
        dest.writeString(ipAddress);
        dest.writeString(hostname);
        dest.writeString(hostname);
        dest.writeString(hostname);
        dest.writeString(hostname);
        dest.writeFloat(responseTime);
        dest.writeInt(position);
        // dest.writeStringArray(services);
        // dest.writeStringArray(banners);
        dest.writeList(portsOpen);
        dest.writeList(portsClosed);
    }

    @SuppressWarnings("unchecked")
    private void readFromParcel(Parcel in) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        isGateway = in.readInt();
        ipAddress = in.readString();
        hostname = in.readString();
        hardwareAddress = in.readString();
        nicVendor = in.readString();
        os = in.readString();
        responseTime = in.readFloat();
        position = in.readInt();
        // services = (String[]) in.readArray(cl);
        // banners = (String[]) in.readArray(cl);
        portsOpen = in.readArrayList(cl);
        portsClosed = in.readArrayList(cl);
    }

    @SuppressWarnings("unchecked")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public HostBean createFromParcel(Parcel in) {
            return new HostBean(in);
        }

        public HostBean[] newArray(int size) {
            return new HostBean[size];
        }
    };
}
