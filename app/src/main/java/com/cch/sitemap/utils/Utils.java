package com.cch.sitemap.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.CoordinateConverter;

public class Utils {

    private static final String TAG = "Utils";

    public static boolean hasPermissions(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    static CoordinateConverter converter;

    static {
        converter = new CoordinateConverter();
        converter.from(CoordinateConverter.CoordType.GPS);
    }

    public static LatLng convertGPS(double lat, double lon) {
        LatLng ll = new LatLng(lat, lon);
        converter.coord(ll);
        LatLng llDest = converter.convert();
        Log.w(TAG, String.format("Convert %.7f,%.7f to %.7f,%.7f",
                lat, lon, llDest.latitude, llDest.longitude));
        return llDest;
    }
}
