package com.dwlhm.cpacp_basic

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

const val LOCATION_PERMISSION_REQUEST_CODE = 3

class InformationActivity : AppCompatActivity() {

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

    private lateinit var warn: TextView
    private lateinit var action: Button

    private val gson: Gson = Gson()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)

        warn = findViewById(R.id.textView3)
        action = findViewById(R.id.btnInfo)

        val dataIntent = intent.getStringExtra("mode")

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            // Start Location Settings Activity, you should explain to the user why he need to enable location before.
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }

        if (dataIntent == "1") {
            findViewById<ImageView>(R.id.imgInfo).setImageResource(R.drawable.ic_bt_not_enabled)
            findViewById<TextView>(R.id.descInfo).text = resources.getText(R.string.info_bt_not_enabled_text)
            action.run {
                text = resources.getText(R.string.info_bt_not_enabled_action)
                setBackgroundResource(R.drawable.btn_red)
                setOnClickListener{ promptEnableBt() }
            }
        }

        if (dataIntent == "2") {
            findViewById<ImageView>(R.id.imgInfo).setImageResource(R.drawable.ic_device_not_connected)
            findViewById<TextView>(R.id.descInfo).text = resources.getText(R.string.info_null_device_registered_text)
            action.run {
                text = resources.getText(R.string.info_null_device_registered_action)
                setBackgroundResource(R.drawable.btn_blue)
                setOnClickListener{
                    scanDevice()
                    text = resources.getText(R.string.info_null_device_registered_action_running)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scanDevice() {
        warn.text = resources.getText(R.string.info_null_device_registered_warn_running)
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this@InformationActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
        if (!isLocationPermissionGranted) reqLocationPermission()
        else bleScanner.startScan(arrayListOf(scanFilter), scanSettings, scanCallback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopBleScan() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this@InformationActivity, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
        action.text = resources.getText(R.string.info_null_device_registered_action)
        bleScanner.stopScan(scanCallback)
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
                    ActivityCompat.requestPermissions(this@InformationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
            }.show()
        }
    }

    private val scanCallback = object : ScanCallback() {

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            warn.text = resources.getText(R.string.info_null_device_registered_warn_failed)
            stopBleScan()
        }

        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            with(result?.device) {
                val deviceList: SharedPreferences = getSharedPreferences("device_list", MODE_PRIVATE)

                val deviceMacJson: ArrayList<ModelDevice> = arrayListOf()

                if (ActivityCompat.checkSelfPermission(
                        this@InformationActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                }
                warn.text = "CPAP berhasil ditemukan dengan\nalamat: ${result?.device?.address}"

                result?.device?.name?.let {
                    result.device?.address?.let { it1 ->
                        ModelDevice(
                            it1,
                            it
                        )
                    }
                }?.let { deviceMacJson.add(it) }

                val editor = deviceList.edit()
                val json = gson.toJson(deviceMacJson)
                editor.putString("mac_list", json)
                editor.apply()

                Log.d("ScanCallback", "Found BLE device! Name: ${result?.device?.name ?: "Unnamed"}, address: $result.device.address")
                stopBleScan()
                startActivity(Intent(this@InformationActivity, MainActivity::class.java))
            }
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
                    ActivityCompat.requestPermissions(this@InformationActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN), 4)
                }
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) reqLocationPermission()
                else bleScanner.startScan(arrayListOf(scanFilter), scanSettings, scanCallback)
            }
        }
    }

    private val requestEnableBt = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != RESULT_OK) promptEnableBt()
        else startActivity(Intent(this, MainActivity::class.java))
    }

    private fun promptEnableBt() {
        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBt.launch(enableBtIntent)
        }
    }

}