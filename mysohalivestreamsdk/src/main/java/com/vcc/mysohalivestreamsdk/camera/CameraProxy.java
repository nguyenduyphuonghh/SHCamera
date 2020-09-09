package com.vcc.mysohalivestreamsdk.camera;

/**
 * Modified by nguyenduyphuonghh 17/07/19
 */

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;


import java.io.IOException;
import java.util.List;

public class CameraProxy {
    private static final String TAG = "CameraProxy";

    private static final int RELEASE = 1;
    private static final int AUTOFOCUS = 2;
    private static final int CANCEL_AUTOFOCUS = 3;
    private static final int SET_PREVIEW_CALLBACK_WITH_BUFFER = 4;
    private static final int SET_PARAMETERS = 5;
    private static final int START_SMOOTH_ZOOM = 6;
    private static final int ADD_CALLBACK_BUFFER = 7;
    private static final int SET_ERROR_CALLBACK = 8;
    private static final int SET_PREVIEW_DISPLAY = 9;
    private static final int START_PREVIEW = 10;
    private static final int STOP_PREVIEW = 11;
    private static final int OPEN_CAMERA = 12;
    private static final int SET_DISPLAY_ORIENTATION = 13;
    private static final int SET_PREVIEW_TEXTURE = 14;
    private final HandlerThread handlerThread;

    private Camera camera;
    private final CameraHandler cameraHandler;
    private final ConditionVariable signal = new ConditionVariable();
    private volatile Camera.Parameters parameters;
    private boolean released = false;
    private OnCameraCallbackListener onCameraCallbackListener;

    public CameraProxy(int cameraId) {
        handlerThread = new HandlerThread("Camera Proxy Thread");
        handlerThread.start();

        cameraHandler = new CameraHandler(handlerThread.getLooper());
        signal.close();
        cameraHandler.obtainMessage(OPEN_CAMERA, cameraId, 0).sendToTarget();
        signal.block();
        if (camera != null) {
            cameraHandler.obtainMessage(SET_ERROR_CALLBACK, new ErrorCallback()).sendToTarget();
        }
    }

    public void setCameraCallbackListener(OnCameraCallbackListener onCameraCallbackListener) {
        this.onCameraCallbackListener = onCameraCallbackListener;
        if (camera != null) {
            cameraHandler.obtainMessage(SET_ERROR_CALLBACK, new ErrorCallback(onCameraCallbackListener)).sendToTarget();
        }
    }

    public boolean isCameraAvailable() {
        return camera != null && !isReleased();
    }

    public void release() {
        released = true;
        signal.close();
        cameraHandler.sendEmptyMessage(RELEASE);
        signal.block();
        handlerThread.quitSafely();

    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        cameraHandler.obtainMessage(AUTOFOCUS, callback).sendToTarget();
    }

    public void gainFocus() {
        if (camera != null) {
            camera.autoFocus(null);
        }
    }

    public void cancelAutoFocus() {
        cameraHandler.sendEmptyMessage(CANCEL_AUTOFOCUS);
    }

    public void setPreviewCallbackWithBuffer(Camera.PreviewCallback callback) {
        cameraHandler.obtainMessage(SET_PREVIEW_CALLBACK_WITH_BUFFER, callback).sendToTarget();
    }

    public Camera.Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Camera.Parameters parameters) {
        this.parameters = parameters;
        cameraHandler.obtainMessage(SET_PARAMETERS, parameters).sendToTarget();
    }

    public void startSmoothZoom(int level) {
        cameraHandler.obtainMessage(START_SMOOTH_ZOOM, level, 0).sendToTarget();
    }

    public void addCallbackBuffer(byte[] buffer) {
        cameraHandler.obtainMessage(ADD_CALLBACK_BUFFER, buffer).sendToTarget();
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        signal.close();
        cameraHandler.obtainMessage(SET_PREVIEW_DISPLAY, holder).sendToTarget();
        signal.block();
    }

    public void startPreview() {
        cameraHandler.sendEmptyMessage(START_PREVIEW);
    }

    public void stopPreview() {
        signal.close();
        cameraHandler.sendEmptyMessage(STOP_PREVIEW);
        signal.block();
    }

    public void setDisplayOrientation(int displayOrientation) {
        cameraHandler.obtainMessage(SET_DISPLAY_ORIENTATION, displayOrientation, 0).sendToTarget();
    }

    public void setPreviewTexture(SurfaceTexture previewTexture) {
        cameraHandler.obtainMessage(SET_PREVIEW_TEXTURE, previewTexture).sendToTarget();
    }

    public boolean turnFlashLightOn() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            if (flashModes != null && flashMode != null) {
                if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(parameters);
                        Log.d(TAG, "Flash opened");
                        return true;
                    } else {
                        Log.d(TAG, "Touch mode not found");
                        return false;
                    }
                } else {
                    Log.d(TAG, "Flash is opening!");
                    return false;
                }
            } else {
                Log.d(TAG, "Flash light not found!");
                return false;
            }

        } else {
            Log.d(TAG, "turnFlashLightOn: camera null");
            return false;
        }
    }

    public boolean turnFlashLightOff() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            if (flashModes != null && flashMode != null) {
                if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                    if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        camera.setParameters(parameters);
                        Log.d(TAG, "Flash closed");
                        return true;
                    } else {
                        Log.d(TAG, "Flashlight: Off mode not found!");
                        return false;
                    }
                } else {
                    Log.d(TAG, "Flash is not opening!");
                    return false;
                }
            } else {
                Log.d(TAG, "Flash light not found!");
                return false;
            }

        } else {
            Log.d(TAG, "turnFlashLightOn: camera null");
            return false;
        }
    }

    public boolean isReleased() {
        return released;
    }

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            try {
                switch (msg.what) {
                    case OPEN_CAMERA:
                        camera = Camera.open(msg.arg1);
                        parameters = camera.getParameters();
                        break;

                    case SET_DISPLAY_ORIENTATION:
                        camera.setDisplayOrientation(msg.arg1);
                        break;

                    case RELEASE:
                        camera.stopPreview();
                        camera.release();
                        break;

                    case AUTOFOCUS:
                        camera.autoFocus((Camera.AutoFocusCallback)msg.obj);
                        break;

                    case CANCEL_AUTOFOCUS:
                        camera.cancelAutoFocus();
                        break;

                    case SET_PREVIEW_TEXTURE:
                        camera.setPreviewTexture((SurfaceTexture) msg.obj);
                        break;

                    case SET_PARAMETERS:
                        try {
                            camera.setParameters((Camera.Parameters)msg.obj);
                        } catch (Exception e) {
                            Log.e(TAG, "Exception setParameter: " + e.getMessage());
                        }

                        break;

                    case START_SMOOTH_ZOOM:
                        camera.startSmoothZoom(msg.arg1);
                        break;

                    case ADD_CALLBACK_BUFFER:
                        camera.addCallbackBuffer((byte[])msg.obj);
                        break;

                    case SET_ERROR_CALLBACK:
                        camera.setErrorCallback((Camera.ErrorCallback)msg.obj);
                        break;

                    case SET_PREVIEW_DISPLAY:
                        camera.setPreviewDisplay((SurfaceHolder)msg.obj);
                        break;

                    case START_PREVIEW:
                        camera.startPreview();
                        break;

                    case STOP_PREVIEW:
                        camera.stopPreview();
                        break;

                    default:
                        Log.e(TAG, "Invalid message: " + msg.what);
                        break;
                }
            }
            catch (RuntimeException e) {
                handleException(msg, e);
            }
            catch (IOException e) {
                handleException(msg, new RuntimeException(e.getMessage(), e));
            }

            signal.open();
        }

        private void handleException(Message msg, RuntimeException e) {
            Log.e(TAG, "Camera operation failed" + e.getMessage());
            if (msg.what != RELEASE && camera != null) {
                try {
                    released = true;
                    camera.release();
                }
                catch (Exception e2) {
                    Log.e(TAG, "Failed to release camera on error" + e.getMessage());
                }
            }
            // throw e;
        }
    }

    private static class ErrorCallback implements Camera.ErrorCallback {

        private OnCameraCallbackListener onCameraCallbackListener;

        public ErrorCallback(OnCameraCallbackListener onCameraCallbackListener) {
            this.onCameraCallbackListener = onCameraCallbackListener;
        }

        public ErrorCallback() {

        }

        @Override
        public void onError(int error, Camera camera) {
            onCameraCallbackListener.onError(error, camera);
            Log.e(TAG, "Got camera error callback. error = " + error + ", camera: " + camera);
        }
    }
}