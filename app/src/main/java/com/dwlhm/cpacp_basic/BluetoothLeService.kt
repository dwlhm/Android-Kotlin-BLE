package com.dwlhm.cpacp_basic

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

class BluetoothLeService: Service() {

    private var btAdapter: BluetoothAdapter? = null
    private val binder = LocalBinder()
    private var btGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED

    fun initialize(): Boolean {
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Log.e("BT", "UNABLE TO OBTAIN A BTADAPTER")
            return false
        }
        return true
    }

    fun connect(address: String): Boolean {
        btAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                btGatt = device.connectGatt(this, true, btGattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w("BLE", "DEVICE NOT FOUND WITH PROVIDED ADDRESS")
                return false
            }
        } ?: run {
            Log.w("BLE", "BTADAPTER NOT INITIALIZED")
            return false
        }
    }

    private val btGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE","TERHUBUNG")
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)

                if (ActivityCompat.checkSelfPermission(
                        this@BluetoothLeService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "DISCONNECTED")
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_DISCOVERED)
            } else Log.w("BLE", "onServicesDiscovered received: $status")
        }
    }

    private fun broadcastUpdate(action: String) {
        Log.d("broadcastUpdate", action)
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class LocalBinder: Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService?>? {
        Log.d("BLE", "START SERVICE")
        return btGatt?.services
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        btGatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            it.readCharacteristic(characteristic)
        } ?: run {
            Log.w("BLE", "BTGATT NOT INITIALIZED")
            return
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        btGatt?.let {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            it.close()
            btGatt = null
        }
    }

    companion object {
        const val ACTION_DATA_AVAILABLE = "com.dwlhm.cpacp_basic.ACTION_DATA_AVAILABLE"
        const val ACTION_GATT_CONNECTED = "com.dwlhm.cpacp_basic.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.dwlhm.cpacp_basic.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_DISCOVERED = "com.dwlhm.cpacp_basic.ACTION_GATT_DISCOVERED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

        val UUID_GELEMBUNG: UUID = UUID.fromString("1992b16d-b009-43ff-a1b7-2fdaec84bd88")
        val UUID_OKSIGEN: UUID = UUID.fromString("f19917c1-baed-4cd3-9854-d4174014e082")
        val UUID_FLOW: UUID = UUID.fromString("d7ea3e34-ff7b-49d9-b27e-07b5efa906a8")
    }

}