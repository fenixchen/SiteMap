package com.cch.sitemap.services;

import android.location.Location;
import android.os.Environment;
import android.util.Log;

import com.cch.sitemap.objects.Point;

import java.io.FileInputStream;
import java.io.IOException;
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
    // Tag
    private static final String TAG = PointService.class.getSimpleName();
    // Singleton pattern
    private static PointService sInstance;

    private List<Point> mPointsAll;

    private PointService() {
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

    private List<Point> readCsvPoints() {
        List<Point> points = new ArrayList<>();
        String extFilePath = Environment.getExternalStorageDirectory() + "/ARMap/";
        try {
            String markerPath = extFilePath + "基站.csv";
            Log.w(TAG, "markerPath:" + markerPath);
            FileInputStream fis = new FileInputStream(markerPath);
            CSVReader reader = new CSVReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            for (; ; ) {
                String[] next = reader.readNext();
                if (next == null || next.length < 3) {
                    break;
                }
                double latitude = Double.parseDouble(next[1]);
                double longitude = Double.parseDouble(next[0]);
                String name = next[2];
                String desc = "";
                if (next.length > 3) {
                    desc = next[3];
                }
                Point p = new Point(name, desc, latitude, longitude, 0);
                points.add(p);
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
