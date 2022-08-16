package com.dwlhm.cpacp_basic

import android.Manifest
import android.app.Service
import android.bluetooth.*
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

        var chars: List<BluetoothGattCharacteristic> = ArrayList()

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

                val uuid = UUID.fromString("6bc6940a-5e1e-4e56-8d81-4351e1048b9d")
                chars = gatt?.getService(uuid)?.characteristics as List<BluetoothGattCharacteristic>

                Log.d("CHARS", chars.size.toString())

                requestCharacteristics(gatt)

//                broadcastUpdate(ACTION_GATT_DISCOVERED)
//                val uuid = UUID.fromString("6bc6940a-5e1e-4e56-8d81-4351e1048b9d")
//                val uuidDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//                val gelembungChar = gatt?.getService(uuid)
//                    ?.getCharacteristic(UUID_GELEMBUNG)
//                val oksigenChar = gatt?.getService(uuid)
//                    ?.getCharacteristic(UUID_OKSIGEN)
//                val flowChar = gatt?.getService(uuid)
//                    ?.getCharacteristic(UUID_FLOW)
//                if (ActivityCompat.checkSelfPermission(
//                        this@BluetoothLeService,
//                        Manifest.permission.BLUETOOTH_CONNECT
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {}
//                gatt?.setCharacteristicNotification(gelembungChar, true)
//                var descriptor = gelembungChar?.getDescriptor(uuidDescriptor)
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt?.writeDescriptor(descriptor)
//                gatt?.readCharacteristic(gelembungChar)
//
//
//                gatt?.setCharacteristicNotification(oksigenChar, true)
//                descriptor = oksigenChar?.getDescriptor(uuidDescriptor)
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt?.writeDescriptor(descriptor)
//                gatt?.readCharacteristic(oksigenChar)
//
//
//                gatt?.setCharacteristicNotification(flowChar, true)
//                descriptor = flowChar?.getDescriptor(uuidDescriptor)
//                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt?.writeDescriptor(descriptor)
//                gatt?.readCharacteristic(flowChar)

            } else Log.w("BLE", "onServicesDiscovered received: $status")
        }

        private fun requestCharacteristics(gatt: BluetoothGatt) {
            if (ActivityCompat.checkSelfPermission(
                    this@BluetoothLeService,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {}

            val uuidDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            gatt?.setCharacteristicNotification(chars[chars.size-1], true)
            var descriptor = chars[chars.size-1]?.getDescriptor(uuidDescriptor)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(descriptor)
            gatt.readCharacteristic(chars[chars.size-1])
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            chars.drop(chars.size-1)

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)

            if (chars.isNotEmpty()) {
                if (gatt != null) {
                    requestCharacteristics(gatt)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic?) {
        val intent = Intent(action)

        val data = characteristic!!.value

        Log.d("characteristic", characteristic.uuid.toString())

        when (characteristic?.uuid) {
            UUID_GELEMBUNG -> {
                Log.d("GELEMBUNG_BLE", String(data))
                intent.putExtra("GELEMBUNG", String(data))
            }
            UUID_OKSIGEN -> {
                Log.d("OKSIGEN_BLE", String(data))
                intent.putExtra("OKSIGEN", String(data))
            }
            UUID_FLOW -> {
                Log.d("FLOW_BLE", String(data))
                intent.putExtra("FLOW", String(data))
            }
            else -> {
                Log.d("YG_LAIN_BLE", String(data))
            }
        }
        sendBroadcast(intent)
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