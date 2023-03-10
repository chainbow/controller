package com.controller;


import static androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.aromajoin.sdk.android.ble.AndroidBLEController;
import com.aromajoin.sdk.core.callback.ConnectCallback;
import com.aromajoin.sdk.core.callback.DisconnectCallback;
import com.aromajoin.sdk.core.device.AromaShooter;
import com.aromajoin.sdk.core.device.Port;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.module.annotations.ReactModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@ReactModule(name = ControllerModule.NAME)
public class ControllerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "Controller";
  private final int DEFAULT_DURATION = 3000; // Unit: millisecond

  private final ReactApplicationContext reactContext;

  private final Map<String, AromaShooter> aromaShooterMap = new HashMap<>();

  private static final int DEFAULT_BOO_INTENSITY = 50;
  private static final int DEFAULT_FAN_INTENSITY = 50;

  private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 1;

  private Promise permissionPromise;

  private final ExecutorService executor;


  public ControllerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    executor = Executors.newSingleThreadScheduledExecutor();

  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  /**
   * ???????????????????????????????????????
   *
   * @param promise
   */
  @ReactMethod
  public void checkBluetooth(Promise promise) {
    try {
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (bluetoothAdapter == null) {
        promise.resolve(-1);
        return;
      }
      if (bluetoothAdapter.isEnabled()) {
        promise.resolve(1);
        return;
      }
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getReactApplicationContext().startActivity(enableBtIntent);
      promise.resolve(1);
    } catch (SecurityException exception) {
      promise.reject("ERROR", "?????????????????????");
    }

  }


  @ReactMethod
  public void checkPermission(Promise promise) {
    if (ContextCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      promise.resolve(1);
      return;
    }
    this.permissionPromise = promise;
    ActivityCompat.requestPermissions(Objects.requireNonNull(getCurrentActivity()), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_REQUEST_CODE);
  }


  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == ACCESS_COARSE_LOCATION_REQUEST_CODE) {
      permissionPromise.resolve(1);
    } else {
      permissionPromise.resolve(-1);
    }
  }


  /**
   * ???????????????????????????sdk????????????????????????
   *
   * @param promise
   */
  @ReactMethod
  public void deviceList(Promise promise) {
    AndroidBLEController bleController = AndroidBLEController.getInstance();
    try {
      executor.execute(() -> bleController.startScan(reactContext, aromaShooters -> {
        if (aromaShooters != null && aromaShooters.size() > 0) {
          aromaShooters.forEach(item -> aromaShooterMap.put(item.getSerial(), item));
          promise.resolve(JSON.toJSONString(aromaShooters.stream().map(AromaShooter::getSerial).collect(Collectors.toList())));
        }
      }));
    } catch (SecurityException exception) {
      promise.resolve(-1);
    }


  }


  /**
   * ??????????????????????????????????????????????????????????????????sdk???????????????????????????
   *
   * @param promise ??????
   */
  @ReactMethod
  public void deviceCacheList(Promise promise) {
    promise.resolve(JSON.toJSONString(aromaShooterMap.values().stream().map(AromaShooter::getSerial).collect(Collectors.toList())));
  }


  @ReactMethod
  public void clearDeviceCacheList(Promise promise) {
    aromaShooterMap.clear();
    promise.resolve(1);
  }

  @ReactMethod
  public void stopScan(Promise promise) {
    try {
      AndroidBLEController bleController = AndroidBLEController.getInstance();
      bleController.stopScan(reactContext);
      promise.resolve(1);
    } catch (Exception exception) {
      promise.resolve(-1);
    }
  }

  /**
   * ????????????
   *
   * @param serial  ?????????
   * @param promise ??????
   *                ????????????????????????????????????????????????????????????????????????
   */
  @ReactMethod
  public void connectDevice(String serial, Promise promise) {
    AndroidBLEController bleController = AndroidBLEController.getInstance();

    AromaShooter aromaShooter = aromaShooterMap.get(serial);
    if (aromaShooter == null) {
      promise.resolve(-1);
      return;
    }
    bleController.stopScan(reactContext);

    executor.execute(() -> bleController.connect(aromaShooter, new ConnectCallback() {
      @Override
      public void onConnected(AromaShooter aromaShooter1) {
        promise.resolve(1);
      }

      @Override
      public void onFailed(AromaShooter aromaShooter1, String msg) {
        promise.resolve(0);

      }
    }));


  }

  /**
   * ????????????
   *
   * @param serial  ????????????
   * @param promise ??????
   */
  @ReactMethod
  public void disConnectDevice(String serial, Promise promise) {
    AndroidBLEController bleController = AndroidBLEController.getInstance();

    AromaShooter aromaShooter = aromaShooterMap.get(serial);
    if (aromaShooter == null) {
      promise.resolve(-1);
      return;
    }
    bleController.stopScan(reactContext);

    executor.execute(() -> bleController.disconnect(aromaShooter, new DisconnectCallback() {
      @Override
      public void onDisconnect(AromaShooter aromaShooter1) {
        promise.resolve(1);
      }

      @Override
      public void onFailed(AromaShooter aromaShooter1, String msg) {
        promise.resolve(0);

      }
    }));


  }


  /**
   * ??????????????????
   *
   * @param duration         ????????????
   * @param boosterIntensity ????????????
   * @param fanIntensity     ????????????
   * @param portTemplate     ?????????portNumber|intensity,portNumber|intensity
   * @param promise          ??????
   */
  @ReactMethod
  public void play(int duration, int boosterIntensity, int fanIntensity, String portTemplate, Promise promise) {
    try {
      AndroidBLEController bleController = AndroidBLEController.getInstance();
      List<AromaShooter> aromaShooters = bleController.getConnectedDevices();
      if (aromaShooters == null || aromaShooters.size() == 0) {
        promise.resolve(-1);
        return;
      }
      Port[] ports = Utility.convertToPort(portTemplate);
      if (duration == 0) duration = DEFAULT_DURATION;
      if (duration > 10000) duration = 10000;
      if (boosterIntensity == 0) boosterIntensity = DEFAULT_BOO_INTENSITY;
      if (boosterIntensity > 100) boosterIntensity = 100;
      if (fanIntensity == 0) fanIntensity = DEFAULT_FAN_INTENSITY;
      if (fanIntensity > 100) fanIntensity = 100;
      bleController.diffuseAll(duration, boosterIntensity, fanIntensity, ports);
      promise.resolve(1);
    } catch (Exception exception) {
      promise.resolve(exception.getMessage());
    }
  }


  /**
   * ??????????????????
   *
   * @param serial  ?????????
   * @param promise ??????
   */
  @ReactMethod
  public void stop(String serial, Promise promise) {
    AndroidBLEController bleController = AndroidBLEController.getInstance();
    AromaShooter aromaShooter = aromaShooterMap.get(serial);
    if (aromaShooter == null) {
      promise.resolve(-1);
      return;
    }
    bleController.stopAllPorts(aromaShooter);
  }


  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  public void diffuse(ReadableArray ports, Promise promise) {
    AndroidBLEController bleController = AndroidBLEController.getInstance();
    List<AromaShooter> aromaShooters = bleController.getConnectedDevices();
    if (aromaShooters == null || aromaShooters.size() == 0) { // check whether there is any connected devices.
      promise.reject(new Exception("No connected devices"));
      return;
    }
    bleController.diffuseAll(DEFAULT_DURATION, true, Utility.convertToIntArray(ports));
    promise.resolve(null);
  }
}
