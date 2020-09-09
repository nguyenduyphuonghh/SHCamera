package com.vcc.mysohalivestreamsdk.camera;

import android.hardware.Camera;

public interface OnCameraCallbackListener {
    void onError(int error, Camera camera);
}
