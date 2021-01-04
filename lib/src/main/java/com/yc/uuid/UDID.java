package com.yc.uuid;

import android.Manifest;
import android.annotation.SuppressLint;
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

import androidx.annotation.RequiresPermission;

import com.bun.miitmdid.core.JLibrary;
import com.bun.supplier.IIdentifierListener;
import com.bun.supplier.IdSupplier;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;
import static android.content.Context.WIFI_SERVICE;

public class UDID {

    public static UDID instance;
    private WeakReference<Context> mContext;
    private UDIDInfo UDIDInfo;

    private UDID(Context context) {
        mContext = new WeakReference<>(context);
    }

    public void init() {
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

    @RequiresPermission(allOf = {ACCESS_WIFI_STATE})
    public  String getMacAddress(final String... excepts) {
        String macAddress = getMacAddressByNetworkInterface();
        if (isAddressNotInExcepts(macAddress, excepts)) {
            return macAddress;
        }
        macAddress = getMacAddressByInetAddress();
        if (isAddressNotInExcepts(macAddress, excepts)) {
            return macAddress;
        }
        macAddress = getMacAddressByWifiInfo();
        if (isAddressNotInExcepts(macAddress, excepts)) {
            return macAddress;
        }
        macAddress = getMacAddressByFile();
        if (isAddressNotInExcepts(macAddress, excepts)) {
            return macAddress;
        }
        return "";
    }

    private  boolean isAddressNotInExcepts(final String address, final String... excepts) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }
        if ("02:00:00:00:00:00".equals(address)) {
            return false;
        }
        if (excepts == null || excepts.length == 0) {
            return true;
        }
        for (String filter : excepts) {
            if (filter != null && filter.equals(address)) {
                return false;
            }
        }
        return true;
    }

    @RequiresPermission(ACCESS_WIFI_STATE)
    private  String getMacAddressByWifiInfo() {
        try {
            final WifiManager wifi = (WifiManager)mContext.get().getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifi != null) {
                final WifiInfo info = wifi.getConnectionInfo();
                if (info != null) {
                    @SuppressLint("HardwareIds")
                    String macAddress = info.getMacAddress();
                    if (!TextUtils.isEmpty(macAddress)) {
                        return macAddress;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    private  String getMacAddressByNetworkInterface() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni == null || !ni.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = ni.getHardwareAddress();
                if (macBytes != null && macBytes.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : macBytes) {
                        sb.append(String.format("%02x:", b));
                    }
                    return sb.substring(0, sb.length() - 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    private  String getMacAddressByInetAddress() {
        try {
            InetAddress inetAddress = getInetAddress();
            if (inetAddress != null) {
                NetworkInterface ni = NetworkInterface.getByInetAddress(inetAddress);
                if (ni != null) {
                    byte[] macBytes = ni.getHardwareAddress();
                    if (macBytes != null && macBytes.length > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : macBytes) {
                            sb.append(String.format("%02x:", b));
                        }
                        return sb.substring(0, sb.length() - 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    private  InetAddress getInetAddress() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                // To prevent phone of xiaomi return "10.0.2.15"
                if (!ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String hostAddress = inetAddress.getHostAddress();
                        if (hostAddress.indexOf(':') < 0) return inetAddress;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private  String getMacAddressByFile() {
        ShellUtils.CommandResult result = ShellUtils.execCmd("getprop wifi.interface", false);
        if (result.result == 0) {
            String name = result.successMsg;
            if (name != null) {
                result = ShellUtils.execCmd("cat /sys/class/net/" + name + "/address", false);
                if (result.result == 0) {
                    String address = result.successMsg;
                    if (address != null && address.length() > 0) {
                        return address;
                    }
                }
            }
        }
        return "02:00:00:00:00:00";
    }

    private  boolean getWifiEnabled() {
        @SuppressLint("WifiManagerLeak")
        WifiManager manager = (WifiManager) mContext.get().getApplicationContext().getSystemService(WIFI_SERVICE);
        if (manager == null) return false;
        return manager.isWifiEnabled();
    }

    @RequiresPermission(CHANGE_WIFI_STATE)
    private void setWifiEnabled(final boolean enabled) {
        @SuppressLint("WifiManagerLeak")
        WifiManager manager = (WifiManager) mContext.get().getApplicationContext().getSystemService(WIFI_SERVICE);
        if (manager == null) return;
        if (enabled == manager.isWifiEnabled()) return;
        manager.setWifiEnabled(enabled);
    }

     public String getMacAddress() {
        String macAddress = getMacAddress((String[]) null);
        if (!TextUtils.isEmpty(macAddress) || getWifiEnabled()) return macAddress;
        setWifiEnabled(true);
        setWifiEnabled(false);
        return getMacAddress((String[]) null);
    }


    public void genWifiMac(Context context) {
        UDIDInfo.setWifimac(getMacAddress());
    }

    public void genUUid() {
        String uniqueID = java.util.UUID.randomUUID().toString().replaceAll("-", "");
        UDIDInfo.setUuid(uniqueID);
    }

    public void genOaId(Context context) {
        try {
            MiitHelper miitHelper = new MiitHelper();
            miitHelper.getDeviceIds(context, new IIdentifierListener() {
                @Override
                public void OnSupport(boolean b, IdSupplier idSupplier) {
                    if (idSupplier != null) {
                        UDIDInfo.setOaid(idSupplier.getOAID());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
