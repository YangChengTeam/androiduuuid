package com.yc.uuid;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.bun.miitmdid.core.JLibrary;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UDID {

    public static UDID instance;
    private WeakReference<Context> mContext;
    private UDIDInfo UDIDInfo;

    private UDID(Context context) {
        mContext = new WeakReference<>(context);
    }

    public void init(){
        try {
            JLibrary.InitEntry(mContext.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UDIDInfo build() {
        Context context = mContext.get();
        UDIDInfo = new UDIDInfo();
        genAndroidId(context);
        genImei2(context);
        genWifiMac(context);
        genSerialno(context);
        genUUid();
        genOaId(context);
        return UDIDInfo;
    }


    public static UDID getInstance(Context context) {
        if (instance == null) {
            synchronized ((UDID.class)) {
                if (instance == null) {
                    instance = new UDID(context);
                }
            }
        }
        return instance;
    }

    public void genAndroidId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        UDIDInfo.setAndroidId(androidId);
    }

    private void genImei2(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        genImei(context);
                    } else {
                        genImeiAndMeid(context);
                    }
                }
            } else {
                genImeiAndMeid(context);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void genImeiAndMeid(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getSystemService(Activity.TELEPHONY_SERVICE);
        Class<?> clazz = null;
        Method method = null;
        try {
            clazz = Class.forName("android.os.SystemProperties");
            method = clazz.getMethod("get", String.class, String.class);
            String gsm = (String) method.invoke(null, "ril.gsm.imei", "");
            String meid = (String) method.invoke(null, "ril.cdma.meid", "");
            if (!TextUtils.isEmpty(gsm)) {
                String imeiArray[] = gsm.split(",");
                if (imeiArray != null && imeiArray.length > 0) {
                    UDIDInfo.setImei(imeiArray[0]);
                    if (imeiArray.length > 1) {
                        UDIDInfo.setImei2(imeiArray[1]);
                    } else {
                        UDIDInfo.setImei2(mTelephonyManager.getDeviceId(1));
                    }
                } else {
                    UDIDInfo.setImei(mTelephonyManager.getDeviceId(0));
                    UDIDInfo.setImei2(mTelephonyManager.getDeviceId(1));
                }
            } else {
                UDIDInfo.setImei(mTelephonyManager.getDeviceId(0));
                UDIDInfo.setImei2(mTelephonyManager.getDeviceId(1));
            }
            UDIDInfo.setMeid(meid);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void genImei(Context context) {
        TelephonyManager tm = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
        UDIDInfo.setImei(tm.getImei(0));
        UDIDInfo.setImei2(tm.getImei(1));
    }

    private void genSerialno(Context context) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            String serialno = (String) get.invoke(c, "ro.serialno");
            UDIDInfo.setSerialno(serialno);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(UDIDInfo.getSerialno()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
                UDIDInfo.setUuid(android.os.Build.SERIAL);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        UDIDInfo.setUuid(android.os.Build.getSerial());
                    } else {
                        UDIDInfo.setUuid(android.os.Build.SERIAL);
                    }
                }
            }
        }

    }

    public void genWifiMac(Context context) {
        WifiManager wifiMan = (WifiManager) context.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        UDIDInfo.setWifimac(wifiInf.getMacAddress());
    }

    public void genUUid() {
        String uniqueID = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        UDIDInfo.setUuid(uniqueID);
    }

    public void genOaId(Context context) {
        try {
            MiitHelper miitHelper = new MiitHelper();
            JSONObject result = miitHelper.getDeviceIds(context);
            if (result != null && result.getString("oaid") != null) {
                UDIDInfo.setOaid(result.getString("oaid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
