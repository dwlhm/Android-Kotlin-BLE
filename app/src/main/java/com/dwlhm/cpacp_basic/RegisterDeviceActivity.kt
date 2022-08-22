package com.dwlhm.cpacp_basic

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class RegisterDeviceActivity : AppCompatActivity() {

    // Enable BT Logic
    private val btAdapter: BluetoothAdapter by lazy {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btManager.adapter
    }
    private val bleScanner by lazy {
        btAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val isLocationPermissionGranted
        get() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid.fromString("6bc6940a-5e1e-4e56-8d81-4351e1048b9d"))
        .build()

    private val gson: Gson = Gson()

    private lateinit var info: TextView
    private lateinit var action: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_device)

        val textInfoRegister: TextView = findViewById(R.id.register_device_status)
        info = findViewById(R.id.txt_register_device_info)
        action = findViewById(R.id.btn_register_device_action)

        findViewById<ImageView>(R.id.btn_close_register_device).setOnClickListener{
            finish()
        }

        action.setOnClickListener {
            scanDevice()
            action.text = resources.getText(R.string.info_null_device_registered_action_running)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scanDevice() {
        info.text = resources.getText(R.string.info_null_device_registered_warn_running)
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this@RegisterDeviceActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
        if (!isLocationPermissionGranted) reqLocationPermission()
        else bleScanner.startScan(arrayListOf(scanFilter), scanSettings, scanCallback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this@RegisterDeviceActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
        action.text = resources.getText(R.string.info_null_device_registered_action)
        bleScanner.stopScan(scanCallback)
    }


    private val scanCallback = object : ScanCallback() {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            info.text = resources.getText(R.string.info_null_device_registered_warn_failed)
            stopBleScan()
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            lateinit var deviceMacJson: ArrayList<ModelDevice>
            with(result?.device) {
                val deviceList: SharedPreferences = getSharedPreferences("device_list", MODE_PRIVATE)
                val deviceMacList: String? = deviceList.getString("mac_list", "[]")
                val type: Type = object : TypeToken<ArrayList<ModelDevice>>() {}.type
                deviceMacJson = gson.fromJson(deviceMacList, type)
                Log.d("deviceMacJson Val 1", deviceMacJson.toString())
                var added = false
                val data: ModelDevice

                if (ActivityCompat.checkSelfPermission(
                        this@RegisterDeviceActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }

                deviceMacJson.forEach {
                    Log.d("MAC",it.getMac())
                    if (it.getMac() == result?.device?.address) added = false
                }

                stopBleScan()

                if (added && result != null) {
                    info.text = "CPAP berhasil ditemukan dengan\nalamat: ${result?.device?.address}"
                    deviceMacJson.add(
                        ModelDevice(
                            result.device.address,
                            result.device.address
                        )
                    )
                    val editor = deviceList.edit()
                    val json = gson.toJson(deviceMacJson)
                    editor.putString("mac_list", json)
                    editor.apply()
                    finish()
                } else info.text = "Tidak terdeteksi perangkat CPAP baru disekitar anda"

            }
        }
    }

    private fun reqLocationPermission() {

        val alert = AlertDialog.Builder(this)

        if (isLocationPermissionGranted) return

        runOnUiThread {
            alert.run {
                setTitle("Location permission required")
                setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                        "location access in order to scan for BLE devices.")
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(this@RegisterDeviceActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
            }.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this@RegisterDeviceActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
                }
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) reqLocationPermission()
                else bleScanner.startScan(arrayListOf(scanFilter), scanSettings, scanCallback)
            }
        }
    }
}