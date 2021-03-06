package com.yc.androidunicodeexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yc.uuid.UDID;
import com.yc.uuid.UDIDInfo;



public class MainActivity extends AppCompatActivity {

    PermissionHelper permissionHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionHelper = new PermissionHelper();
        permissionHelper.checkAndRequestPermission(this, new PermissionHelper.OnRequestPermissionsCallback() {
            @Override
            public void onRequestPermissionSuccess() {
                UDIDInfo udidInfo = UDID.getInstance(MainActivity.this).build();
                Log.d("############", udidInfo.getOaid()+"");
            }

            @Override
            public void onRequestPermissionError() {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHelper.onRequestPermissionsResult(this, requestCode);
    }
}