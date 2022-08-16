package com.dwlhm.cpacp_basic

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class MainActivity : AppCompatActivity(), RecyclerViewClickListener {

    private val requestConnectBT = 2

    private lateinit var deviceAdapter: DeviceAdapter
    private val gson: Gson = Gson()

    private lateinit var deviceMacJson: ArrayList<ModelDevice>

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                requestConnectBT)
        }

        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (!bluetoothAdapter?.isEnabled!!) {
            val intent = Intent(this, InformationActivity::class.java).run {
                putExtra("mode", "1")
            }
            startActivity(intent)
            this.finish()
        }

    }

    override fun onRestart() {
        super.onRestart()

        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (!bluetoothAdapter?.isEnabled!!) {
            val intent = Intent(this, InformationActivity::class.java).run {
                putExtra("mode", "1")
            }
            startActivity(intent)
            this.finish()
        }

    }

    override fun onResume() {
        super.onResume()

        val deviceList: SharedPreferences = getSharedPreferences("device_list", MODE_PRIVATE)
        val deviceMacList: String? = deviceList.getString("mac_list", "[]")
        val type: Type = object : TypeToken<ArrayList<ModelDevice>>() {}.type

        if (deviceMacList == "[]") {
            val intent = Intent(this, InformationActivity::class.java).run {
                putExtra("mode", "2")
            }
            startActivity(intent)
            this.finish()
        }

        deviceMacJson = gson.fromJson(deviceMacList, type)

        // Set layout
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_register_device).setOnClickListener{
            val intent = Intent(this, RegisterDeviceActivity::class.java)
            startActivity(intent)
        }

        val listDevice: RecyclerView = findViewById(R.id.list_device_card)
        deviceAdapter = DeviceAdapter(deviceMacJson)

        deviceAdapter.listener = this@MainActivity

        val layoutManager = LinearLayoutManager(applicationContext)
        listDevice.layoutManager = layoutManager
        listDevice.itemAnimator = DefaultItemAnimator()
        listDevice.adapter = deviceAdapter

    }

    override fun onItemClicked(view: View, device: ModelDevice) {
        Toast.makeText(this, "Device ${device.getMac()} dipilih!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this@MainActivity, ReadingActivity::class.java).putExtra("device_name", device.getDate()).putExtra("device_mac", device.getMac()))
    }

}