package com.cch.sitemap.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.cch.sitemap.BuildConfig;
import com.cch.sitemap.R;
import com.cch.sitemap.objects.Point;
import com.cch.sitemap.services.Compass;
import com.cch.sitemap.services.PointService;
import com.cch.sitemap.utils.Utils;
import com.cch.sitemap.views.CompassView;
import com.cch.sitemap.views.PointsView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Fragment showing the points around the user location using augmented reality over a camera preview.<br>
 * <p>
 * Requires Manifest.permission.CAMERA and Manifest.permission.ACCESS_FINE_LOCATION permissions.
 */
public class AugmentedRealityFragment extends CameraPreviewFragment
        implements LocationListener, Compass.CompassListener {

    public static final int LEFT_TO_RIGHT = 0;
    public static final int RIGHT_TO_LEFT = 1;
    public static final int TOP_TO_BOTTOM = 2;
    public static final int BOTTOM_TO_TOP = 3;
    // Tag
    private static final String TAG = AugmentedRealityFragment.class.getSimpleName();
    private static final String TAG_ALERT_DIALOG_ENABLE_GPS = AlertDialogFragment.TAG + "_ENABLE_GPS";
    // Request codes
    private static final int REQUEST_PERMISSIONS = (TAG.hashCode() & 0x0000ffff) - 1;
    private static final int REQUEST_ENABLE_GPS = (TAG.hashCode() & 0x0000ffff) - 2;
    // Constants
    // The minimum distance the user must have moved from its previous location to recalculate azimuths and distances, in meters
    private static final int MIN_DISTANCE_DIFFERENCE_BETWEEN_RECALCULATIONS = 10;
    // The minimum distance the user must have moved from its previous location to reload the points from the database, in meters
    private static final int MIN_DISTANCE_DIFFERENCE_BETWEEN_DATABASE_RELOADS = 500;
    // The maximum distance to search and display points around the user's location, in meters
    private static final int MAX_RADIUS_DISTANCE_TO_SEARCH_POINTS_AROUND = 10000;
    // The minimum time interval between GPS location updates, in milliseconds
    private static final long MIN_TIME_INTERVAL_BETWEEN_LOCATION_UPDATES = 5000;
    // The maximum age of a location update from the system to be considered as still valid (in order to avoid working with old positions), in milliseconds
    private static final long MAX_AGE_FOR_A_LOCATION = 3 * 60000;
    // The minimum difference with the last orientation values from Compass for the CompassListener to be notified, in degrees
    private static final float MIN_AZIMUTH_DIFFERENCE_BETWEEN_COMPASS_UPDATES = 3;
    private static final float MIN_VERTICAL_INCLINATION_DIFFERENCE_BETWEEN_COMPASS_UPDATES = 3;
    private static final float MIN_HORIZONTAL_INCLINATION_DIFFERENCE_BETWEEN_COMPASS_UPDATES = 3;
    private static final int POINTS_IN_RANGE = 500;
    // Check for regular GPS updates
    // Init
    private final Handler mCheckGpsHandler = new Handler();
    // Permissions
    private boolean mHasPermissions;
    private String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};
    // Location
    private LocationManager mLocationManager;
    private Location mLastGpsLocation;
    // Compass
    private Compass mCompass;
    // Points
    private Point mUserLocationPoint;
    private Location mUserLocationAtLastDbReading;
    private List<Point> mPoints = new ArrayList<>();
    // Views
    private PointsView mPointsView;
    private CompassView mCompassView;
    //private TextView mVerticalInclinationTextView;
    //private TextView mHorizontalInclinationTextView;
    private TextView mGpsStatusTextView;
    private final Runnable mCheckGpsRunnable = new Runnable() {
        @Override
        public void run() {
            updateGpsStatus();
            mCheckGpsHandler.postDelayed(this, 1000);
        }
    };
    private MapView mMapView;
    private boolean isFirstLoc = true;
    private BitmapDescriptor bitmapDescriptor;
    private float mAccuracy;
    private WebView mWebView;
    private ProgressBar mWebViewLoading;
    private TextView mSelectedPoint;
    private LinearLayout mSceneLayout;
    private TextView mNearbyPoints;
    private Handler handler = new Handler() {  //此函数是属于MainActivity.java所在线程的函数方法，所以可以直接条调用MainActivity的 所有方法。
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x01) {   //
                String mapUrl = String.format("http://43.254.45.8:18080/viewer-shanghai/index.html?id=%s", (String) msg.obj);
                Log.i(TAG, "访问地址:" + mapUrl);
                mWebView.loadUrl(mapUrl);
            }
        }
    };

    public void gesture(int type) {

    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHasPermissions = Utils.hasPermissions(getContext(), REQUIRED_PERMISSIONS);

        // Check permissions
        if (!mHasPermissions) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
        } else {
            // Compass
            mCompass = Compass.newInstance(getContext(), this);
        }
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.map_arrow);
        bmp = Bitmap.createScaledBitmap(bmp, 24, 24, true);
        bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bmp);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView.onDestroy();
    }

    public void onTouchEvent(float x, float y) {
        Point p = mPointsView.getTouchPoint(x, y);
        if (p == null) {
            mSceneLayout.setVisibility(View.GONE);
        } else {
            mSelectedPoint.setText(p.getName());
            SendGetRequest(p.getLongitude(), p.getLatitude());
            mWebView.setVisibility(View.GONE);
            mWebViewLoading.setVisibility(View.VISIBLE);
            mSceneLayout.setVisibility(View.VISIBLE);
        }
    }

    private String getSceneId(String json) {
        String id = "";
        try {
            JSONArray jsonArray = new JSONArray(json);
            if (jsonArray.length() > 0) {
                id = jsonArray.getJSONObject(0).optString("id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }

    public void SendGetRequest(double lon, double lat) {
        final String url = String.format("http://43.254.45.8:18080/jietu-pano-svr-shanghai/api/nearby/%.7f/%.7f/100000", lon, lat);
        final double curLon = lon, curLat = lat;
        new Thread() {
            @Override
            public void run() {
                String pathString = url;
                HttpURLConnection connection;
                try {
                    URL url = new URL(pathString);
                    Log.i(TAG, "请求地址:" + pathString);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    //接受数据
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        String line;
                        StringBuilder lines = new StringBuilder();
                        while ((line = bufferedReader.readLine()) != null) { //不为空进行操作
                            lines.append(line);
                        }
                        Log.w(TAG, "接受到的数据：" + lines);
                        if (lines.length() > 0) {
                            Message msg = Message.obtain();
                            msg.what = 0x01;
                            msg.obj = getSceneId(lines.toString());
                            handler.sendMessage(msg);
                        } else {
                            Toast.makeText(getContext(), String.format("找不到(%.7f, %.7f)对应街景", curLon, curLat), Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_augmented_reality, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views
        mPointsView = view.findViewById(R.id.points_view);
        mCompassView = view.findViewById(R.id.compass_view);
        mCompassView.setVisibility(View.INVISIBLE);
        mGpsStatusTextView = view.findViewById(R.id.gps_status_text_view);
        mWebViewLoading = view.findViewById(R.id.webViewLoading);
        mWebView = view.findViewById(R.id.webView);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mWebView.setVisibility(View.VISIBLE);
                        mWebViewLoading.setVisibility(View.GONE);
                    }
                }, 3000);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        mWebView.getSettings().setJavaScriptEnabled(true);//设置webView属性，运行JS脚本
        mNearbyPoints = view.findViewById(R.id.nearby_points);
        mSelectedPoint = view.findViewById(R.id.selectedPoint);
        mSceneLayout = view.findViewById(R.id.sceneLayout);
        mSceneLayout.setVisibility(View.GONE);
        mMapView = view.findViewById(R.id.bmapView);
        mMapView.getMap().setMyLocationEnabled(true);
        UiSettings uiSettings = mMapView.getMap().getUiSettings();
        uiSettings.setCompassEnabled(true);
        mMapView.getMap().setCompassEnable(true);
        mMapView.getMap().setCompassPosition(new android.graphics.Point(50, 50));
        MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, bitmapDescriptor);
        mMapView.getMap().setMyLocationConfigeration(configuration);
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.zoom(15.0f);
        mMapView.getMap().setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        //mVerticalInclinationTextView = view.findViewById(R.id.pitch_text_view);
        //mHorizontalInclinationTextView = view.findViewById(R.id.roll_text_view);
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onResume() {
        super.onResume();

        if (mHasPermissions) {
            // GPS location listener
            Location sim = PointService.getInstance().readSimGPS();

            mLocationManager = (LocationManager) getActivity().getSystemService(Activity.LOCATION_SERVICE);
            if (sim == null) {
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_INTERVAL_BETWEEN_LOCATION_UPDATES, 5, this);
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_INTERVAL_BETWEEN_LOCATION_UPDATES, 5, this);
                } catch (SecurityException e) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Missing location permission");
                    e.printStackTrace();
                }
            }
            try {
                Location gps = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location network = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if (sim != null) {
                    Log.w(TAG, "Init with SIM GPS " + sim.getLongitude() + "," + sim.getLatitude());
                    onLocationChanged(sim);
                } else if (gps != null) {
                    Log.w(TAG, "Init with last known GPS PROVIDER " + gps.getLongitude() + "," + gps.getLatitude());
                    gps.setTime(System.currentTimeMillis());
                    onLocationChanged(gps);
                } else if (network != null) {
                    Log.w(TAG, "Init with last known NETWORK PROVIDER " + gps.getLongitude() + "," + gps.getLatitude());
                    network.setTime(System.currentTimeMillis());
                    onLocationChanged(network);
                } else {
                    Log.e(TAG, "Unknown GPS point");
                }
            } catch (Exception ex2) {
            }

            // Check GPS status
            updateGpsStatus();

            // Start compass
            if (mCompass != null)
                mCompass.start(MIN_AZIMUTH_DIFFERENCE_BETWEEN_COMPASS_UPDATES, MIN_VERTICAL_INCLINATION_DIFFERENCE_BETWEEN_COMPASS_UPDATES, MIN_HORIZONTAL_INCLINATION_DIFFERENCE_BETWEEN_COMPASS_UPDATES);

            // Start GPS updated checks
            mCheckGpsHandler.postDelayed(mCheckGpsRunnable, 1000);
        }
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mHasPermissions) {
            // Stop GPS updated checks and listener
            mCheckGpsHandler.removeCallbacks(mCheckGpsRunnable);
            mLocationManager.removeUpdates(this);
        }
        mMapView.onPause();
        if (mHasPermissions) {
            // Stop compass
            if (mCompass != null) mCompass.stop();
        }

    }

    // CameraPreviewFragment implementation
    @Override
    protected int getTextureViewResIdForCameraPreview() {
        return R.id.texture_view;
    }

    @Override
    protected void onCameraPreviewReady(float[] cameraPreviewAnglesOfView) {
        // Set camera angles
        if (cameraPreviewAnglesOfView != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Configuring PointsView with camera angles (horizontal x vertical): " + cameraPreviewAnglesOfView[0] + "° x " + cameraPreviewAnglesOfView[1] + "°");
            mPointsView.setCameraAngles(cameraPreviewAnglesOfView[0], cameraPreviewAnglesOfView[1]);
        }
    }

    // CompassListener interface
    @Override
    public void onOrientationChanged(float azimuth, float verticalInclination, float horizontalInclination) {
        mCompassView.updateAzimuth(azimuth);
        //mVerticalInclinationTextView.setText(String.format(getString(R.string.orientation_pitch_degrees), verticalInclination));
        //mHorizontalInclinationTextView.setText(String.format(getString(R.string.orientation_roll_degrees), horizontalInclination));
        mPointsView.updateOrientation(azimuth, verticalInclination, horizontalInclination);
        if (mUserLocationPoint != null) {
            updateMapView(mAccuracy, mUserLocationPoint.getLatitude(), mUserLocationPoint.getLongitude());
        }
    }

    // LocationListener interface
    @Override
    public void onLocationChanged(Location location) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "LocationListener.onLocationChanged(): " + location.toString());
            if (location.isFromMockProvider()) {
                Log.d(TAG, "Location received is from mock provider");
            }
        }

        LatLng ll = Utils.convertGPS(location.getLatitude(), location.getLongitude());
        if (isFirstLoc) {
            isFirstLoc = false;
            MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
            mMapView.getMap().animateMapStatus(u);
        }


        // Check the location validity
        if (location.getTime() >= System.currentTimeMillis() - MAX_AGE_FOR_A_LOCATION) {
            mLastGpsLocation = location;
            if (mUserLocationPoint == null || mUserLocationPoint.distanceTo(location) > MIN_DISTANCE_DIFFERENCE_BETWEEN_RECALCULATIONS) {
                if (BuildConfig.DEBUG)
                    Log.w(TAG, "New Location:" + location.getLongitude() + "," + location.getLatitude());
                mUserLocationPoint = new Point(getString(R.string.gps_your_location), location);
                mAccuracy = location.getAccuracy();
                mPoints = PointService.getInstance().getPointsInRange(location, POINTS_IN_RANGE);
                // Update points view
                mPointsView.setPoints(mUserLocationPoint, PointService.sortPointsByRelativeAzimuth(mUserLocationPoint, mPoints));
                // Update map
                updateMapView(location.getAccuracy(), location.getLatitude(), location.getLongitude());

                mMapView.getMap().clear();
                StringBuilder nearbyPoints = new StringBuilder();
                BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_geo);
                int count = 0;
                for (Point p : mPoints) {
                    LatLng llPoint = Utils.convertGPS(p.getLocation().getLatitude(), p.getLocation().getLongitude());
                    if (BuildConfig.DEBUG)
                        Log.w(TAG, "Add Overlay:" + p.getLocation().getLongitude() + "," + p.getLocation().getLatitude());
                    OverlayOptions option = new MarkerOptions()
                            .position(llPoint)
                            .icon(bitmap);
                    mMapView.getMap().addOverlay(option);
                    nearbyPoints.append(String.format("%d. %s\n", ++count, p.getName()));
                }
                mNearbyPoints.setText(nearbyPoints.toString());
            }
        }
        updateGpsStatus();
    }

    private void updateMapView(float accuracy, double lat, double lon) {
        LatLng cur = Utils.convertGPS(lat, lon);
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(accuracy)
                .direction(mCompassView.getAzimuth())
                .latitude(cur.latitude)
                .longitude(cur.longitude).build();
        mMapView.getMap().setMyLocationData(locData);
    }

    // LocationListener interface
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (BuildConfig.DEBUG) Log.d(TAG, "LocationListener.onStatusChanged()");
        updateGpsStatus();
    }

    // LocationListener interface
    @Override
    public void onProviderEnabled(String provider) {
        if (BuildConfig.DEBUG) Log.d(TAG, "LocationListener.onProviderEnabled()");
        updateGpsStatus();
    }

    // LocationListener interface
    @Override
    public void onProviderDisabled(String provider) {
        if (BuildConfig.DEBUG) Log.d(TAG, "LocationListener.onProviderDisabled()");
        updateGpsStatus();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_GPS && resultCode == Activity.RESULT_OK) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            if (Utils.hasPermissions(getContext(), REQUIRED_PERMISSIONS)) {
                getActivity().recreate();
            } else {
                getActivity().finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Check GPS status and update UI accordingly
    // Should be called whenever GPS status has changed
    @SuppressLint("MissingPermission")
    private void updateGpsStatus() {
        if (isAdded()) {
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location sim = PointService.getInstance().readSimGPS();
                if (sim != null) {
                    mLastGpsLocation = sim;
                    mGpsStatusTextView.setText(String.format("SIM GPS:%.7f, %.7f", mLastGpsLocation.getLongitude(), mLastGpsLocation.getLatitude()));
                } else {
                    mLastGpsLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (mLastGpsLocation != null) {
                        mGpsStatusTextView.setText(String.format("基站定位GPS:%.7f, %.7f", mLastGpsLocation.getLongitude(), mLastGpsLocation.getLatitude()));
                    } else {
                        mGpsStatusTextView.setText("没有基站定位信息");
                    }
                }
                //mGpsStatusTextView.setText(getString(R.string.gps_disabled));
                //mPointsView.setPoints(null, null);
                //mLastGpsLocation.setTime(System.currentTimeMillis());
                //onLocationChanged(mLastGpsLocation);
                //showEnableGpsAlertDialog();
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "GPS is enabled");
                dismissEnableGpsAlertDialog();
                if (mLastGpsLocation != null && mLastGpsLocation.getTime() >= System.currentTimeMillis() - MAX_AGE_FOR_A_LOCATION) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "GPS located");
                    mGpsStatusTextView.setText(String.format(getString(R.string.gps_updated_seconds_ago), (System.currentTimeMillis() - mLastGpsLocation.getTime()) / 1000));
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "GPS waiting for location");
                    mGpsStatusTextView.setText(getString(R.string.gps_waiting_for_location));
                    mPointsView.setPoints(null, null);
                }
            }
        }
    }

    // Display an alert dialog asking the user to enable the GPS
    private void showEnableGpsAlertDialog() {
        assert getFragmentManager() != null;
        if (isAdded() && getFragmentManager().findFragmentByTag(TAG_ALERT_DIALOG_ENABLE_GPS) == null) {
            final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(R.string.gps, R.string.gps_disabled_alert_message, android.R.string.ok, android.R.string.cancel);
            alertDialogFragment.setTargetFragment(this, REQUEST_ENABLE_GPS);
            final FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(alertDialogFragment, TAG_ALERT_DIALOG_ENABLE_GPS);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    // Dismiss the alert dialog asking the user to enable the GPS
    private void dismissEnableGpsAlertDialog() {
        if (getFragmentManager() != null && isAdded() && getFragmentManager().findFragmentByTag(TAG_ALERT_DIALOG_ENABLE_GPS) != null) {
            ((AlertDialogFragment) getFragmentManager().findFragmentByTag(TAG_ALERT_DIALOG_ENABLE_GPS)).dismissAllowingStateLoss();
        }
    }
}
