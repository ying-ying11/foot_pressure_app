package com.flyn.sarcopenia_project.viewer

import android.content.*
import android.content.Context.BIND_AUTO_CREATE
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.flyn.sarcopenia_project.MainActivity
import com.flyn.sarcopenia_project.R
import com.flyn.sarcopenia_project.connectBle
import com.flyn.sarcopenia_project.file.cache_file.EmgCacheFile
import com.flyn.sarcopenia_project.file.cache_file.ImuCacheFile
import com.flyn.sarcopenia_project.service.BleAction
import com.flyn.sarcopenia_project.service.BluetoothLeService
import com.flyn.sarcopenia_project.service.UUIDList
import com.flyn.sarcopenia_project.utils.ExtraManager
import com.flyn.sarcopenia_project.utils.FileManager
import com.flyn.sarcopenia_project.utils.toShortArray
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.cancel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.min


class DataViewer: AppCompatActivity() {

    companion object {
        private const val TAG = "Data Viewer"
        private val tagName = listOf("Foot Pressure")
    }

    private val emg = DataPage(0f, 2500f, "Hallux","LT","M1","M5","Arch","HM") { "%.2f g".format(it) }
//    private val emg = DataPage(0f, 4095f, "Hallux","LT","M1","M5","Arch","HM") { "%.2f v".format(emgTransform(it)) }
//
//    private val acc = DataPage(-32768f, 32768f, "x", "y", "z") { "%.2f g".format(accTransform(it)) }
//
//    private val gyr = DataPage(-32768f, 32768f, "x", "y", "z") { "%.2f rad/s".format(gyrTransform(it)) }

    private val pageAdapter = object: FragmentStateAdapter(supportFragmentManager, lifecycle) {

        val fragments = arrayOf(emg)

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]

    }

    private val dataReceiver = object: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BleAction.GATT_CONNECTED.name -> {
                    service.enableNotification(true)
                }
                BleAction.GATT_DISCONNECTED.name -> {
                    // reconnect
                    GlobalScope.launch(Dispatchers.Default) {
                        while (!service.connect(address)) {
                            delay(100)
                        }
                    }
                }
                BleAction.ADC1_DATA_AVAILABLE.name -> {
                    intent.getByteArrayExtra(BluetoothLeService.DATA)?.let {
                        val data = ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN)
                        val adc = shortArrayOf(data.short, data.short, data.short, data.short, data.short, data.short)

                        val ha_gram = calibrate(emgTransform(adc[0]), 0)
                        val lt_gram = calibrate(emgTransform(adc[1]), 1)
                        val m1_gram = calibrate(emgTransform(adc[2]), 2)
                        val m5_gram = calibrate(emgTransform(adc[3]), 3)
                        val arch_gram = calibrate(emgTransform(adc[4]), 4)
                        val hm_gram = calibrate(emgTransform(adc[5]), 5)

                        val text = getString(R.string.emg_describe, ha_gram, lt_gram, m1_gram, m5_gram, arch_gram, hm_gram)

//                        val text = getString(R.string.emg_describe,emgTransform(adc1),emgTransform(adc2),emgTransform(adc3),emgTransform(adc4),emgTransform(adc5),emgTransform(adc6))
                        emg.addData(text, ha_gram, lt_gram, m1_gram, m5_gram, arch_gram, hm_gram)
                        GlobalScope.launch(Dispatchers.IO) {
                            FileManager.appendRecordData(FileManager.ADC1_FILE, EmgCacheFile(time, adc))
                        }
                        emg.updateSamplingRate(++adcCount)
                    }
                }
            }
        }

    }

    private val serviceCallback = object: ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as BluetoothLeService.BleServiceBinder).getService()
            service.connect(address)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service.enableNotification(false)
        }

    }

    val time: Long
        get() = Date().time - startTime

    private val startTime = Date().time
    private val tabSelector: TabLayout by lazy { findViewById(R.id.data_viewer_tab) }
    private val pager: ViewPager2 by lazy { findViewById(R.id.data_viewer)}
    private val saveButton: FloatingActionButton by lazy { findViewById(R.id.data_viewer_save_button) }

    private lateinit var address: String
    private lateinit var service: BluetoothLeService

    private var adcCount = 0

    //    private var emgRightCount = 0
//    private var accCount = 0
//    private var gyrCount = 0

    private fun emgTransform(value: Short): Float = value.toFloat() / 4095f * 3.6f
    private fun emgTransform(value: Float): Float = value / 4095f * 3.6f

//    private val weight = arrayOf(0.0014, 0.0008, 0.0009, 0.0015, 0.0015, 0.0012)
//    private val bias = arrayOf(0.1293, 0.0638, -0.0501, 0.0084, -0.14, 0.2478)
    private val weight = arrayOf(0.0014, 0.0008, 0.0016, 0.001, 0.0011,0.0013)
    private val bias = arrayOf(0.411, 0.1106, 0.6439, 0.153, -0.0914, 0.4504)

    private fun calibrate(adc: Float, index: Int): Float =
        ((adc + bias[index]) / weight[index]).toFloat()
//    private fun accTransform(value: Short): Float = value.toFloat() / 32767f * 2f
//    private fun accTransform(value: Float): Float = value / 32767f * 2f
//
//    private fun gyrTransform(value: Short): Float = value.toFloat() / 32767f * 250f
//    private fun gyrTransform(value: Float): Float = value / 32767f * 250f


    private fun saveFile() {
        GlobalScope.launch(Dispatchers.IO){
            FileManager.writeRecordFile(adcCount)
            this@DataViewer.runOnUiThread {
                Toast.makeText(
                    this@DataViewer.applicationContext,
                    R.string.sava_completed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_viewer)

        intent.getStringExtra(ExtraManager.DEVICE_ADDRESS)?.let {
            address = it
        }?:run {
            startActivity(Intent(this, MainActivity::class.java))
        }

        pager.adapter = pageAdapter
        TabLayoutMediator(tabSelector, pager) { tab, position ->
            tab.text = tagName[position]
        }.attach()

        saveButton.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                setMessage(R.string.check_saving)
                setPositiveButton(R.string.save) { dialog, which ->
                    saveFile()
                    startActivity(Intent(this@DataViewer, connectBle::class.java))
                    finish()
                }
                setNegativeButton(R.string.cancel) { dialog, which ->
                    startActivity(Intent(this@DataViewer, connectBle::class.java))
                    finish()
                }
            }.show()
        }

        IntentFilter().run {
            addAction(BleAction.GATT_CONNECTED.name)
            addAction(BleAction.GATT_DISCONNECTED.name)
            addAction(BleAction.ADC1_DATA_AVAILABLE.name)
            registerReceiver(dataReceiver, this)
        }

        bindService(
            Intent(this, BluetoothLeService::class.java), serviceCallback,
            BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(dataReceiver)
        unbindService(serviceCallback)
        GlobalScope.launch(Dispatchers.IO) {
            FileManager.removeTempRecord()
        }
    }

}