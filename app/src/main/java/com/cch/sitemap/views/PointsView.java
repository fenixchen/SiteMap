package com.cch.sitemap.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.cch.sitemap.BuildConfig;
import com.cch.sitemap.R;
import com.cch.sitemap.objects.Point;

import java.util.SortedMap;

/**
 * Custom {@link View} that displays points from a {@link SortedMap < Float,Point>} depending on their azimuth.<br>
 */
public class PointsView extends View {

    // Tag
    private static final String TAG = PointsView.class.getSimpleName();

    // Constants
    // The size of the arrow placemark
    private static final int ICON_WIDTH = 252 / 2;

    private static final int ICON_HEIGHT = 152 / 2;

    private static final int TEXT_HEIGHT = 40;
    // Drawing
    private final TextPaint mTextPaint;
    // Points
    private SortedMap<Float, Point> mPoints;
    private Point mUserPoint;
    // Device and view orientations
    private float mAzimuthViewLeft;
    private float mAzimuthViewRight;
    private float mVerticalAngleViewTop;
    private float mVerticalAngleViewBottom;
    private float mRoll;
    // Screen to camera angles ratios: the number of pixels on the screen associated to a one degree variation on the camera
    // Default values are those of a Nexus 4 camera
    private float mHorizontalCameraAngle = 54.8f;
    private float mVerticalCameraAngle = 42.5f;
    private float mHorizontalPixelsPerDegree;
    private float mVerticalPixelsPerDegree;
    private String mPointText;

    public PointsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        // Paint
        mTextPaint = new TextPaint();
        mTextPaint.setColor(Color.RED);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStrokeWidth(2);
        mTextPaint.setTextSize(30);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mPointText = context.getString(R.string.points_view_display_information);
    }

    /**
     * Sets the device camera angles of view.<br>
     * This angle of view is used to calculate the placement of the points.<br>
     * If not set, default values are those of a Nexus 4: horizontal angle = 54.8° and vertical angle = 42.5°.
     *
     * @param horizontalCameraAngle the horizontal angle of view in degrees such as 0° < angle < 180°.
     * @param verticalCameraAngle   the vertical angle of view in degrees such as 0° < angle < 180°.
     */
    public void setCameraAngles(float horizontalCameraAngle, float verticalCameraAngle) {
        // Camera angles
        if (horizontalCameraAngle > 0 && horizontalCameraAngle < 180 && verticalCameraAngle > 0 && verticalCameraAngle < 180) {
            mHorizontalCameraAngle = horizontalCameraAngle;
            mVerticalCameraAngle = verticalCameraAngle;
            // Force recalculation of pixels per degree in onDraw()
            mHorizontalPixelsPerDegree = 0;
            mVerticalPixelsPerDegree = 0;
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Invalid camera angles, must be: 0° < angle < 180°");
        }
    }

    /**
     * Sets the points that will be displayed in the {@link PointsView}.
     *
     * @param points    the {@link SortedMap < Float , Point>} mapping the relative azimuth of the point as the key with the associated {@link Point} as the value. Must be sorted by ascending azimuths.
     * @param userPoint the current user location point, used as a reference.
     */
    public void setPoints(Point userPoint, SortedMap<Float, Point> points) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Updating points list with " + (points != null ? points.size() : 0) + " points");
        mUserPoint = userPoint;
        mPoints = points;
        invalidate();
    }

    /**
     * Updates the orientation: azimuth, pitch and roll of the device.<br>
     * On them depends which points will be displayed and where will they be on the {@link PointsView};
     *
     * @param azimuth the azimuth in degrees.
     * @param pitch   the vertical inclination in degrees.
     * @param roll    the horizontal inclination in degrees.<br>
     */
    public void updateOrientation(float azimuth, float pitch, float roll) {
        mAzimuthViewLeft = (azimuth - mHorizontalCameraAngle / 2);
        mAzimuthViewRight = (azimuth + mHorizontalCameraAngle / 2);
        // When the device screen is held perpendicular to the ground, its camera pointing horizontally towards the landscape:
        //      - The device pitch = -90°.
        //      - The vertical angle of the points displayed at the center of the view is 0°.
        float verticalAngleViewCenter = -pitch - 90;
        mVerticalAngleViewTop = verticalAngleViewCenter + mVerticalCameraAngle / 2;
        mVerticalAngleViewBottom = verticalAngleViewCenter - mVerticalCameraAngle / 2;
        mRoll = roll;

        // Update view
        if (mPoints != null) {
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Scaling: calculate the number of pixels on the screen associated to a 1° angle variation on the camera
        if (mHorizontalPixelsPerDegree == 0 || mVerticalPixelsPerDegree == 0) {
            mHorizontalPixelsPerDegree = getWidth() / mHorizontalCameraAngle;
            mVerticalPixelsPerDegree = getHeight() / mVerticalCameraAngle;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "View size in pixels = " + getWidth() + "x" + getHeight());
                Log.d(TAG, "Screen pixels associated to 1° camera angle variation: horizontal=" + mHorizontalPixelsPerDegree + "px/° & vertical=" + mVerticalPixelsPerDegree + "px/°");
            }
        }

        // Draw visible points on canvas
        if (mUserPoint != null && mPoints != null && !mPoints.isEmpty()) {
            for (SortedMap.Entry<Float, Point> entry : mPoints.entrySet()) {
                float azimuth = entry.getKey();
                Point point = entry.getValue();
                final int[] xy = getPixelCoordinates(azimuth, mUserPoint.verticalAngleTo(point));
                if (xy == null) {
                    continue;
                }
                // Draw arrow
                final Drawable drawable = getResources().getDrawable(R.drawable.green_mid, null);
                drawable.setBounds(xy[0] - ICON_WIDTH / 2,
                        xy[1] - ICON_HEIGHT / 2,
                        xy[0] + ICON_WIDTH / 2,
                        xy[1] + ICON_HEIGHT / 2);
                drawable.draw(canvas);
                // Draw text
//                Log.i(TAG, String.format("Distance between %.7f,%.7f to %.7f,%.7f",
//                        mUserPoint.getLongitude(), mUserPoint.getLatitude(),
//                        point.getLongitude(), point.getLatitude()));
                final String pointText = String.format(mPointText,
                        entry.getValue().getName(),
                        mUserPoint.distanceTo(entry.getValue()));
                final StaticLayout mTextLayout = new StaticLayout(pointText,
                        mTextPaint,
                        100,
                        Layout.Alignment.ALIGN_NORMAL,
                        1.0f,
                        0.0f,
                        false);
                canvas.save();
                canvas.translate(xy[0] + ICON_WIDTH / 2 + 15, xy[1] - ICON_HEIGHT / 3);
                mTextLayout.draw(canvas);
                canvas.restore();
            }
        }
    }

    /**
     * Returns the x and y coordinates in pixels for a given azimuth and vertical angle of a point.<br>
     * Coordinates are following the usual Android system:<br>
     * 1) (0,0) is top left corner.<br>
     * 2) (maxX,0) is top right corner.<br>
     * 3) (0,maxY) is bottom left corner.<br>
     * 4) (maxX,maxY) is bottom right corner.<br>
     *
     * @param azimuth       the azimuth of the point, in degrees from 0° to 360°.
     * @param verticalAngle the vertical angle of the point, in degrees from -90° to 90°.
     * @return the {@link int[]} coordinates such as:<br>
     * result[0] the x coordinate in pixels.<br>
     * result[y] the y coordinate in pixels.
     */
    private int[] getPixelCoordinates(float azimuth, float verticalAngle) {
        // Coordinates in pixels
        int x;
        int y;

        // Invalid azimuth
        if (azimuth < 0 || azimuth >= 360) {
            return null;
        }

        // Invalid vertical angle or not visible
        if (verticalAngle < -90 || verticalAngle > 90 || verticalAngle > mVerticalAngleViewTop || verticalAngle < mVerticalAngleViewBottom) {
            return null;
        }

        // x coordinates calculation from azimuth
        // Normal case : 0 < azimuthFrom < azimuth < azimuthTo < 360
        if (azimuth > mAzimuthViewLeft && azimuth < mAzimuthViewRight) {
            x = (int) (mHorizontalPixelsPerDegree * (azimuth - mAzimuthViewLeft - (mAzimuthViewRight - mAzimuthViewLeft) / 2) + getWidth() / 2);
            // Special case 1 : azimuthFrom < 0 < azimuth < azimuthTo < 360
        } else if (mAzimuthViewLeft < 0 && azimuth > 360 + mAzimuthViewLeft) {
            x = (int) (mHorizontalPixelsPerDegree * (azimuth - 360 - mAzimuthViewLeft - (mAzimuthViewRight - mAzimuthViewLeft) / 2) + getWidth() / 2);
            // Special case 2 : 0 < azimuthFrom < azimuth < 360 < azimuthTo
        } else if (mAzimuthViewRight > 360 && azimuth < mAzimuthViewRight - 360) {
            x = (int) (mHorizontalPixelsPerDegree * (azimuth + 360 - mAzimuthViewLeft - (mAzimuthViewRight - mAzimuthViewLeft) / 2) + getWidth() / 2);
            // Azimuth not visible
        } else {
            return null;
        }

        // y coordinates calculation from vertical angle
        y = (int) ((mVerticalAngleViewTop - verticalAngle) * mVerticalPixelsPerDegree);

        return applyRollOnPixelCoordinates(x, y);
    }

    /**
     * Calculates the pixel coordinates variation taking into account the roll of the device.
     *
     * @param x the original x coordinate in pixels.
     * @param y the original y coordinate in pixels.
     * @return the {@link int[]} coordinates after applying the roll such as:<br>
     * result[0] the x coordinate in pixels.<br>
     * result[y] the y coordinate in pixels.
     */
    private int[] applyRollOnPixelCoordinates(int x, int y) {
        // Center the frame in the middle of the screen (to apply the rotation from the center)
        x -= getWidth() / 2;
        y -= getHeight() / 2;

        // Apply rotation to the coordinates with the device roll value
        // Rotation around the center of the frame:
        // x' = x cos a - y sin a
        // y' = y cos a + x sin a
        double rollRadians = Math.toRadians(mRoll);
        int x2 = (int) (x * Math.cos(-rollRadians) - y * Math.sin(-rollRadians));
        int y2 = (int) (y * Math.cos(rollRadians) - x * Math.sin(rollRadians));

        // Center the frame in the top left corner (Android pixels coordinate system)
        x2 += getWidth() / 2;
        y2 += getHeight() / 2;

        if (x2 < 0 || y2 < 0 || x2 > getWidth() || y2 > getHeight()) {
            return null;
        } else {
            return new int[]{x2, y2};
        }
    }

    public void touch(float x, float y) {
        if (mUserPoint != null && mPoints != null && !mPoints.isEmpty()) {
            for (SortedMap.Entry<Float, Point> entry : mPoints.entrySet()) {
                float azimuth = entry.getKey();
                Point point = entry.getValue();
                int[] xy = getPixelCoordinates(azimuth, mUserPoint.verticalAngleTo(point));
                if (xy == null) {
                    continue;
                }
                float adjWidth = ICON_WIDTH / 2;
                float adjHeight = ICON_HEIGHT / 2;

                float x1 = xy[0] - adjWidth;
                float x2 = xy[0] + adjWidth + 100;
                float y1 = xy[1] - adjHeight;
                float y2 = xy[1] + adjHeight;

                if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
                    Log.w(TAG, "Touch " + point.getName());
                    break;
                }
            }
        }
    }
}