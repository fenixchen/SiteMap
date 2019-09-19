package com.cch.sitemap.services;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.cch.sitemap.app.MainApplication;
import com.cch.sitemap.objects.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Helper class that performs operations related to {@link Point}.
 */

public class PointService {

    // Constants
    // The Earth mean radius in meters
    private static final double EARTH_RADIUS = 6371000;

    public static final String SITE_FOLDER = "/ARMap/";

    // Tag
    private static final String TAG = PointService.class.getSimpleName();
    // Singleton pattern
    private static PointService sInstance;

    private List<Point> mPointsAll;
    public static final String SITE_FILE = "基站.csv";
    public static final String SIM_GPS = "SimGPS.txt";
    //默认站点高度
    private static final int DEFAULT_ALTITUDE = 25;

    private PointService() {
        checkSiteCsv();
        mPointsAll = readCsvPoints();
    }
    /**
     * Initializes if necessary and returns the singleton instance of {@link PointService}.
     *
     * @return the singleton instance of {@link PointService}.
     */
    public static synchronized PointService getInstance() {
        if (sInstance == null) {
            sInstance = new PointService();
        }
        return sInstance;
    }

    private void copyFilesFassets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFassets(context, oldPath + "/" + fileName, newPath + "/" + fileName);
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 如果不存在SDCARD的CSV文件, 则从asserts拷贝一个
     */
    private void checkSiteCsv() {
        String extFilePath = Environment.getExternalStorageDirectory() + PointService.SITE_FOLDER;
        String markerPath = extFilePath + PointService.SITE_FILE;
        File folder = new File(extFilePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File marker = new File(markerPath);
        if (!marker.exists()) {
            copyFilesFassets(MainApplication.getContext(), PointService.SITE_FILE, markerPath);

            String simPath = extFilePath + PointService.SIM_GPS + ".bak";
            copyFilesFassets(MainApplication.getContext(), PointService.SIM_GPS, simPath);
        }
    }

    /**
     * 读取外置存储的CSV文件
     *
     * @return
     */
    private List<Point> readCsvPoints() {
        List<Point> points = new ArrayList<>();
        String extFilePath = Environment.getExternalStorageDirectory() + SITE_FOLDER;
        String markerPath = extFilePath + SITE_FILE;
        try {
            Log.w(TAG, "markerPath:" + markerPath);
            FileInputStream fis = new FileInputStream(markerPath);
            CSVReader reader = new CSVReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            for (; ; ) {
                String[] next = reader.readNext();
                if (next == null || next.length < 3) {
                    break;
                }
                try {
                    int altitude = DEFAULT_ALTITUDE;
                    double latitude = Double.parseDouble(next[1]);
                    double longitude = Double.parseDouble(next[0]);
                    String name = next[2];
                    if (next.length > 3) {
                        altitude = Integer.parseInt(next[3]);
                    }
                    String desc = "";
                    if (next.length > 4) {
                        desc = next[4];
                    }
                    Point p = new Point(name, desc, latitude, longitude, altitude);
                    points.add(p);
                } catch (NumberFormatException ne) {
                    ne.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Load Points:" + points.size());
        return points;
    }
    /**
     * Helper method that calculates the great-circle distance (in meters) over the earth’s surface associated to a latitude or longitude difference (in degrees).
     *
     * @param degrees the latitude or longitude difference (in degrees).
     * @return the distance (in meters).
     */
    public static int degreesToMeters(double degrees) {
        return (int) Math.abs(degrees * 2 * Math.PI * EARTH_RADIUS / 360);
    }

    // Static helper methods

    /**
     * Helper method that calculates the latitude or longitude difference (in degrees) associated to a great-circle distance (in meters) over the earth’s surface.
     *
     * @param distance the distance (in meters).
     * @return the latitude or longitude difference (in degrees).
     */
    public static double metersToDegrees(int distance) {
        return distance * 360 / (2 * Math.PI * EARTH_RADIUS);
    }

    /**
     * Returns a valid latitude value in degrees comprised between -90° and 90° using modulo.
     *
     * @param latitude the latitude value to correct.
     * @return the valid latitude value.
     */
    public static double getValidLatitude(double latitude) {
        double l = latitude % 360;
        if (l >= -90 && l <= 90) {
            return l;
        } else if (l > 90 && l < 180) {
            return 90 - l % 90;
        } else if ((l > 180 && l < 270) || (l < -180 && l > -270)) {
            return -l % 90;
        } else if (l > 270 && l < 360) {
            return -90 + l % 90;
        } else if (l < -90 && l > -180) {
            return -90 - l % 90;
        } else if (l < -270 && l > -360) {
            return 90 + l % 90;
        } else if (l == 180 || l == -180) {
            return 0;
        } else if (l == 270) {
            return -90;
        } else if (l == -270) {
            return 90;
        } else {
            return 0;
        }
    }

    /**
     * Returns a valid longitude value in degrees comprised between -180° and 180° using modulo.
     *
     * @param longitude the longitude value to correct.
     * @return the valid longitude value.
     */
    public static double getValidLongitude(double longitude) {
        double l = longitude % 360;
        if (l >= -180 && l <= 180) {
            return l;
        } else if (l > 180 && l < 360) {
            return -180 + l % 180;
        } else if (l < -180 && l > -360) {
            return 180 + l % 180;
        } else {
            return 0;
        }
    }

    /**
     * Calculates the relative azimuth of each {@link Point} from {@param points} as seen from {@param originPoint} (which is for instance the user location).<br>
     * Returns a {@link SortedMap <>} mapping:<br>
     * - As key: each point azimuth, as seen from {@param originPoint}.<br>
     * - As value: each {@link Point} from {@param points}.<br>
     * The {@link SortedMap <>} is sorted by key value (which means by point azimuth).
     *
     * @param originPoint the {@link Point} from which to calculate the relative azimuths of the other points. For instance, the user location.
     * @param points      the {@link List <Point>} to sort by relative azimuth.
     * @return the {@link SortedMap <>} of points sorted by azimuth as seen from {@param originPoint}, and using azimuth values as keys.
     */
    public static SortedMap<Float, Point> sortPointsByRelativeAzimuth(Point originPoint, List<Point> points) {
        SortedMap<Float, Point> pointsSortedMap = new TreeMap<>();
        for (Point p : points) {
            pointsSortedMap.put(originPoint.azimuthTo(p), p);
        }
        return pointsSortedMap;
    }

    public List<Point> getPointsInRange(Location location, int pointsInRange) {
        ArrayList<Point> rangePoints = new ArrayList<>();
        for (Point p : mPointsAll) {
            if (location.distanceTo(p.getLocation()) <= pointsInRange) {
                rangePoints.add(p);
            }
        }
        return rangePoints;
    }

    private Location locSim = null;
    private boolean firstLoadSim = true;
    public Location readSimGPS() {
        if (firstLoadSim) {
            firstLoadSim = false;
            Location loc = null;
            String extFilePath = Environment.getExternalStorageDirectory() + "/ARMap/";
            String simPath = extFilePath + "SimGPS.txt";
            FileInputStream fis;
            try {
                fis = new FileInputStream(simPath);
                CSVReader reader = new CSVReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                String[] next = reader.readNext();
                if (next != null && next.length >= 2) {
                    double lon = Double.parseDouble(next[0]);
                    double lat = Double.parseDouble(next[1]);
                    int altitude = 0;
                    if (next.length > 2) {
                        altitude = Integer.parseInt(next[2]);
                    }
                    loc = new Location("SIM");
                    loc.setAltitude(altitude);
                    loc.setLatitude(lat);
                    loc.setLongitude(lon);
                    loc.setTime(System.currentTimeMillis());
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            locSim = loc;
        }
        return locSim;
    }
}
