package com.intel.realsense.capture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.Extrinsic;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.Intrinsic;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.Pixel;
import com.intel.realsense.librealsense.Point_3D;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.Utils;
import com.intel.realsense.librealsense.VideoFrame;

// Aim of this fragment is to show how the following functions must be used:
// From class Utils:
// deprojectPixelToPoint
// transformPointToPoint
// projectPointToPixel
// getFov
// project2dPixelToDepthPixel



private int framesCounter = 1;
Runnable mStreaming = new Runnable() {
    @Override
    public void run() {
        try {
            try(FrameSet frames = mPipeline.waitForFrames()) {
                try(FrameSet processed = frames.applyFilter(mColorizer)) {
                    mGLSurfaceView.upload(processed);

                    if((++framesCounter % 80) == 0) {
        DepthFrame depthFrame = frames.first(StreamType.DEPTH).as(Extension.DEPTH_FRAME);
        Intrinsic depthFrameIntrinsic = depthFrame.getProfile().getIntrinsic();
        float fov[] = Utils.getFov(depthFrameIntrinsic);
        Log.d(TAG, "fov = (" + fov[0] + ", " + fov[1] + ")");
        VideoFrame colorFrame = frames.first(StreamType.COLOR).as(Extension.VIDEO_FRAME);
        Intrinsic colorFrameIntrinsic = colorFrame.getProfile().getIntrinsic();
        Extrinsic depthToColorExtrinsic = depthFrame.getProfile().getExtrinsicTo(colorFrame.getProfile());
        int w = depthFrameIntrinsic.getWidth();
        int h = depthFrameIntrinsic.getHeight();
        byte[] depthFrameData = new byte[ w * h ];
        depthFrame.getData(depthFrameData);
        int middle = w * h / 2;
        float depthAtMiddleOfFrame = depthFrameData[middle];
        while (depthAtMiddleOfFrame < 0.2) {
            middle += 10;
            depthAtMiddleOfFrame = depthFrameData[middle];
        }
        Log.d(TAG, "depth = " + depthAtMiddleOfFrame);
        Pixel depth_pixel = new Pixel(w/2, h/2);
        Point_3D depth_point = Utils.deprojectPixelToPoint(depthFrameIntrinsic, depth_pixel, depthAtMiddleOfFrame );
        Point_3D color_point = Utils.transformPointToPoint(depthToColorExtrinsic, depth_point );
        Pixel color_pixel = Utils.projectPointToPixel(colorFrameIntrinsic, color_point);
        Log.d(TAG, "depthPoint = (" + depth_point.mX + ", " + depth_point.mY + ", " + depth_point.mZ + ")");
        Log.d(TAG, "colorPoint = (" + color_point.mX + ", " + color_point.mY + ", " + color_point.mZ + ")");
        Log.d(TAG, "pixel = (" + depth_pixel.mX + ", " + depth_pixel.mY + ")");
        Log.d(TAG, "color pixel = (" + color_pixel.mX + ", " + color_pixel.mY + ")");
        float depthMin = 0.1f;
        float depthMax = 10.f;
        Extrinsic colorToDepthExtrinsic = colorFrame.getProfile().getExtrinsicTo(depthFrame.getProfile());
        Pixel toPixel = Utils.project2dPixelToDepthPixel(depthFrame, mDepthScale, depthMin, depthMax,
        depthFrameIntrinsic, colorFrameIntrinsic, colorToDepthExtrinsic, depthToColorExtrinsic, color_pixel);
        Log.d(TAG, "to_pixel = (" + toPixel.mX + ", " + toPixel.mY + ")");
                    }
                }
            }
            mHandler.post(mStreaming);
        }
        catch (Exception e) {
            Log.e(TAG, "streaming, error: " + e.getMessage());
        }
    }
};
