package com.yc.uuid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.bun.miitmdid.core.JLibrary;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UUID {

    public static UUID instance;
    private WeakReference<Context> mContext;
    private UUIDInfo uuidInfo;

    private UUID(Context context) {
        mContext = new WeakReference<>(context);
        try {
            JLibrary.InitEntry(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public UUIDInfo build() {
        Context context = mContext.get();
        uuidInfo = new UUIDInfo();
        genAndroidId(context);

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
        genBtMac(context);
        genWifiMac(context);
        genSerialno();
        genUUid();
        genOaId(context);
        return uuidInfo;
    }

    public static UUID getInstance(Context context) {
        if (instance == null) {
            synchronized ((UUID.class)) {
                if (instance == null) {
                    instance = new UUID(context);
                }
            }
        }
        return instance;
    }

    public void genAndroidId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        uuidInfo.setAndroidId(androidId);
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
                    uuidInfo.setImei(imeiArray[0]);
                    if (imeiArray.length > 1) {
                        uuidInfo.setImei2(imeiArray[1]);
                    } else {
                        uuidInfo.setImei2(mTelephonyManager.getDeviceId(1));
                    }
                } else {
                    uuidInfo.setImei(mTelephonyManager.getDeviceId(0));
                    uuidInfo.setImei2(mTelephonyManager.getDeviceId(1));
                }
            } else {
                uuidInfo.setImei(mTelephonyManager.getDeviceId(0));
                uuidInfo.setImei2(mTelephonyManager.getDeviceId(1));
            }
            uuidInfo.setMeid(meid);
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
        uuidInfo.setImei(tm.getImei(0));
        uuidInfo.setImei2(tm.getImei(1));
    }

    private void genSerialno() {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            String serialno = (String) get.invoke(c, "ro.serialno");
            uuidInfo.setSerialno(serialno);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("ServiceCast")
    public void genBtMac(Context context) {
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        ;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                Field mServiceField = ba.getClass().getDeclaredField("mService");
                mServiceField.setAccessible(true);
                Object btManagerService = mServiceField.get(ba);
                if (btManagerService != null) {
                    uuidInfo.setBtmac((String) btManagerService.getClass().getMethod("getAddress").invoke(btManagerService));
                }
            } catch (NoSuchFieldException e) {

            } catch (NoSuchMethodException e) {

            } catch (IllegalAccessException e) {

            } catch (InvocationTargetException e) {

            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ba = (BluetoothAdapter) context.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        uuidInfo.setBtmac(ba.getAddress());
    }

    public void genWifiMac(Context context) {
        WifiManager wifiMan = (WifiManager) context.getSystemService(
                Context.WIFI_SERVICE);
        WifiInfo wifiInf = wifiMan.getConnectionInfo();
        uuidInfo.setWifimac(wifiInf.getMacAddress());
    }

    public void genUUid() {
        String uniqueID = java.util.UUID.randomUUID().toString();
        uuidInfo.setUuid(uniqueID);
    }

    public void genOaId(Context context) {
        try {
            MiitHelper miitHelper = new MiitHelper();
            JSONObject result = miitHelper.getDeviceIds(context);
            if (result != null && result.getString("oaid") != null) {
                uuidInfo.setOaid(result.getString("oaid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
