package com.wyk.bletool;





import static com.wyk.bletool.contain.ServiceContain.Device_information;
import static com.wyk.bletool.contain.ServiceContain.Software_Revision_String_Service;

import android.annotation.SuppressLint;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aispeech.tvui.keyevent.IKeyEventListener;
import com.aispeech.tvui.keyevent.TvuiKeyEvent;
import com.wyk.bletool.utils.getBluetoothDevice;
import com.wyk.bletool.utils.hex;


import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    TextView name;
    TextView mac;
    TextView bluetoothclass;
    TextView version;
    TextView pid;
    TextView vid;
    Button btn1;
    Button btn2;
    TextView keyevent;

    private getBluetoothDevice mgetBluetoothDevice;

    private static BluetoothDevice mBluetoothDevice;

    private Handler mHandler = new Handler(Looper.myLooper()); // 声明一个处理器对象

    private BluetoothGatt mBluetoothGatt; // 声明一个蓝牙GATT客户端对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        name = findViewById(R.id.name);
        mac = findViewById(R.id.mac);
        bluetoothclass = findViewById(R.id.bluetoothclass);
        version = findViewById(R.id.version);
        pid = findViewById(R.id.productid);
        vid = findViewById(R.id.vendorid);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        keyevent = findViewById(R.id.keyevent);
//        StringBuilder builder = new StringBuilder();
        TvuiKeyEvent.getInstance().init(MainActivity.this);
        long id = TvuiKeyEvent.getInstance().registerListener(new IKeyEventListener() {
            @Override
            public void onKey(int i, int i1, int i2, String s) {
                if (4 == i) {
//                    builder.append(s + ":"
//                            + " " + String.format("%04x", i).toUpperCase()
//                            + " " + String.format("%04x", i1).toUpperCase()
//                            + " " + String.format("%08x", i2).toUpperCase()
//                            + "\r\n");
                    keyevent.append(s + ":"
                            + " " + String.format("%04x", i).toUpperCase()
                            + " " + String.format("%04x", i1).toUpperCase()
                            + " " + String.format("%08x", i2).toUpperCase()
                            + "\r\n");
//                    keyevent.setText(builder);


                }
            }

            @Override
            public void onDev(int i, String s) {
                Log.d(TAG, "devType:" + i + ", keyPath:" + s);
            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.postDelayed(mRefresh, 50);
        //获取版本号
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 连接GATT服务器
                mBluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this, false,
                        mGattCallback);
                mBluetoothGatt.connect();
            }
        });

        //清空蓝牙码值
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keyevent.setText("");
            }
        });


    }


    // 定义一个刷新任务，每隔两秒刷新扫描到的蓝牙设备
    private Runnable mRefresh = new Runnable() {
        @SuppressLint({"MissingPermission", "NewApi"})
        @Override
        public void run() {
         // 开始扫描周围的蓝牙设备

            Set<BluetoothDevice> connectedDevicesV2 =
                    getBluetoothDevice.getConnectedDevicesV2(MainActivity.this);
            if (connectedDevicesV2.size()==1) {
                for (BluetoothDevice bluetoothDevice:connectedDevicesV2) {
                    mBluetoothDevice = bluetoothDevice;
                }
            }else if(connectedDevicesV2.size()==0) {
                Toast.makeText(MainActivity.this,"未连接设备,请保持设备连接" ,Toast.LENGTH_SHORT).show();
                mBluetoothDevice=null;
            }
            name.setText(""+mBluetoothDevice.getName());
            mac.setText(""+mBluetoothDevice.getAddress());
            bluetoothclass.setText(""+mBluetoothDevice.getBluetoothClass().toString());


            // 延迟30秒后再次启动蓝牙设备的刷新任务
            mHandler.postDelayed(this, 20*1000);
        }
    };

    // 创建一个GATT客户端回调对象
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        // BLE连接的状态发生变化时回调
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange status=" + status + ", newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) { // 连接成功
                Log.d(TAG, "onConnectionStateChange: 连接成功，开始发现服务");
                mBluetoothGatt.discoverServices(); // 开始查找GATT服务器提供的服务

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mBluetoothGatt =
                        mBluetoothDevice.connectGatt(MainActivity.this, false,
                                mGattCallback);// 连接断开
                Log.d(TAG, "onConnectionStateChange: 尝试回连");


            }
        }

        // 发现BLE服务端的服务列表及其特征值时回调

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered status" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 获得特征值的第一种办法：直接问硬件厂商，然后把特征值写在代码中
                BluetoothGattService service = mBluetoothGatt.getService(Device_information);
                if (service != null) {
                    Log.d(TAG, "服务Service " + service.getUuid());

                    BluetoothGattCharacteristic chara =
                            service.getCharacteristic(Software_Revision_String_Service);

                    Log.d(TAG, "特征值: " + chara.getUuid());
                    if (chara != null) {
                        int charaProp = chara.getProperties(); // 获取该特征的属性
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            Log.d(TAG, "chara:可读 ");
                        }
                        boolean b = mBluetoothGatt.setCharacteristicNotification(chara, true);
                        Log.d(TAG, "onServicesDiscovered 设置通知 " + b);
//                        if (b){
//                            List<BluetoothGattDescriptor> descriptors = chara.getDescriptors();
//                            if(descriptors != null && descriptors.size() > 0) {
//                                for(BluetoothGattDescriptor descriptor : descriptors) {
//                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                                    mBluetoothGatt.writeDescriptor(descriptor);
//                                }
//                            }
//                        }
                        gatt.readCharacteristic(chara);


                    }else {
                        Log.d(TAG, "特征值chara为空: ");
                    }

                }else {
                    Log.d(TAG, "onServicesDiscovered: 服务为空");
                }



            }else {
                Log.d(TAG, "onServicesDiscovered: 发现BLE服务端的服务列表及其特征值时回调false");
            }

        }

        // 收到BLE服务端的数据变更时回调
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic chara) {
            super.onCharacteristicChanged(gatt, chara);
            String message = new String(chara.getValue()); // 把服务端返回的数据转成字符串
            Log.d(TAG, "onCharacteristicChanged "+message+"    uuid"+chara.getUuid());


        }


        // 收到BLE服务端的数据写入时回调
        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic chara, int status) {
            Log.d(TAG, "onCharacteristicWrite status="+status);
            gatt.setCharacteristicNotification(chara,true);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicWrite: ********************");
            } else {
                Log.d(TAG, "write fail->" + status);
            }
            super.onCharacteristicWrite(gatt, chara, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS){
                byte[] data = characteristic.getValue();
                String s = hex.bytesToHex(data);
                String str = hex.hexStr2Str(s);
                Log.d(TAG, "onCharacteristicRead: 版本号:"+str);
                runOnUiThread(()->version.setText(str));

            }else{
                Toast.makeText(MainActivity.this,"版本号查询失败,尝试重新查询",Toast.LENGTH_SHORT).show();
                gatt.readCharacteristic(characteristic);
            }

        }



        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            Log.d(TAG, "onDescriptorRead: "+descriptor);
            super.onDescriptorRead(gatt, descriptor, status);
        }

    };





    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        InputDevice inputDevice = InputDevice.getDevice(event.getDeviceId());
        pid.setText("0x"+Integer.toHexString(inputDevice.getProductId()));
        vid.setText("0x"+Integer.toHexString(inputDevice.getVendorId()));
        return super.onKeyDown(keyCode, event);

    }

    @Override
    public void onBackPressed() {
        // 屏蔽返回键
    }

    @Override
    protected void onDestroy() {
        TvuiKeyEvent.getInstance().destroy();
        super.onDestroy();
    }
}