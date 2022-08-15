package com.dwlhm.cpacp_basic

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class ReadingActivity : AppCompatActivity() {

    private var bleService: BluetoothLeService? = null
    private var devMac: String? = null
    private lateinit var isConnected: TextView
    private lateinit var gelembungVal: TextView
    private lateinit var oksigenVal: TextView
    private lateinit var flowVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)

        val devName = intent.getStringExtra("device_name")
        devMac = intent.getStringExtra("device_mac")

        findViewById<TextView>(R.id.device_info_name_reading).text = devName
        findViewById<TextView>(R.id.device_info_mac_reading).text = devMac
        isConnected = findViewById(R.id.device_info_connection_reading)
        gelembungVal = findViewById(R.id.gelembung_val)
        oksigenVal = findViewById(R.id.oksigen_val)
        flowVal = findViewById(R.id.flow_val)

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.d("BroadcastReceiver", p1?.action.toString())
            when (p1?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    isConnected.text = "CONNECTED"
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    isConnected.text = "DISCONNECTED"
                }
                BluetoothLeService.ACTION_GATT_DISCOVERED -> {
                    Log.d("ACTION_GATT_DISCOVERED", "START")
//                    displayGattServices(bleService?.getSupportedGattServices())
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    oksigenVal.text = p1.getStringExtra("BLE_OKSIGEN")
                    flowVal.text = p1.getStringExtra("BLE_FLOW")
                    gelembungVal.text = p1.getStringExtra("BLE_GELEMBUNG")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bleService != null) {
            val result = devMac?.let { bleService!!.connect(it) }
            Log.d("BLE", "CONNECTION RESULT $result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            bleService = (p1 as BluetoothLeService.LocalBinder).getService()
            bleService?.let { bluetooth ->
                if(!bluetooth.initialize()) {
                    Log.d("BLE", "UNABLE TO INITIALIZE")
                    finish()
                }
                devMac?.let {
                    Log.d("BLE", "CONNECTED 2")
                    bluetooth.connect(it)
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            bleService = null
        }

    }

}