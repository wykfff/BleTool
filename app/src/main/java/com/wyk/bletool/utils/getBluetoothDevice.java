package com.wyk.bletool.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.wyk.bletool.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 获取设备已连接的蓝牙遥控器
 */

public class getBluetoothDevice {

    public static boolean isConnected(String macAddress) {
        if (!BluetoothAdapter.checkBluetoothAddress(macAddress)) {
            return false;
        }
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);

        Method isConnectedMethod = null;
        boolean isConnected;
        try {
            isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
            isConnectedMethod.setAccessible(true);
            isConnected = (boolean) isConnectedMethod.invoke(device, (Object[]) null);
        } catch (NoSuchMethodException e) {
            isConnected = false;
        } catch (IllegalAccessException e) {
            isConnected = false;
        } catch (InvocationTargetException e) {
            isConnected = false;
        }
        return isConnected;
    }

    /**
     * 获取系统中已连接的蓝牙设备
     * @return
     */
    @SuppressLint("MissingPermission")
    public static Set<BluetoothDevice> getConnectedDevicesV2(Context context) {

        Set<BluetoothDevice> result = new HashSet<>();
        Set<BluetoothDevice> deviceSet = new HashSet<>();

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        //获取BLE的设备, profile只能是GATT或者GATT_SERVER
        @SuppressLint("MissingPermission") List GattDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        if (GattDevices != null && GattDevices.size() > 0) {
            deviceSet.addAll(GattDevices);
        }
        //获取已配对的设备
        @SuppressLint("MissingPermission") Set ClassicDevices = bluetoothManager.getAdapter().getBondedDevices();
        if (ClassicDevices != null && ClassicDevices.size() > 0) {
            deviceSet.addAll(ClassicDevices);
        }

        for (BluetoothDevice dev : deviceSet
        ) {
            String Type = "";
            switch (dev.getType()) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    Type = "经典";
                    break;
                case BluetoothDevice.DEVICE_TYPE_LE:
                    Type = "BLE";
                    break;
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    Type = "双模";
                    break;
                default:
                    Type = "未知";
                    break;
            }
            String connect = "设备未连接";
            if (isConnected(dev.getAddress())) {
                result.add(dev);
                connect = "设备已连接";
            }
         //   Log.d("zbh", connect + ", address = " + dev.getAddress() + "(" + Type + "), name
            //   --> " + dev.getName());
        }
        return result;
    }

}
