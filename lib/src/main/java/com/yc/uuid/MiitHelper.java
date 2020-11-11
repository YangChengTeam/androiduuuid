package com.yc.uuid;

import android.content.Context;
import android.util.Log;

import com.bun.miitmdid.core.ErrorCode;
import com.bun.miitmdid.core.MdidSdkHelper;
import com.bun.supplier.IIdentifierListener;


public class MiitHelper {
    public void getDeviceIds(Context cxt, IIdentifierListener iIdentifierListener) {
        int code = MdidSdkHelper.InitSdk(cxt, true, iIdentifierListener);
        Log.d("MiitHelper", descriptionCode(code));
    }

    private String descriptionCode(int code) {
        switch (code) {
            case ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT:
                return "DEVICE_NOSUPPORT";
            case ErrorCode.INIT_ERROR_LOAD_CONFIGFILE:
                return "LOAD_CONFIGFILE";
            case ErrorCode.INIT_ERROR_MANUFACTURER_NOSUPPORT:
                return "MANUFACTURER_NOSUPPORT";
            case ErrorCode.INIT_ERROR_RESULT_DELAY:
                return "RESULT_DELAY";
            case ErrorCode.INIT_HELPER_CALL_ERROR:
                return "HELPER_CALL_ERROR";
            default:
                return "SUCCESS";
        }
    }
}
