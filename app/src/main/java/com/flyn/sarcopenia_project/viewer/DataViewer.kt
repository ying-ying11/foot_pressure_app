package com.flyn.sarcopenia_project.viewer


import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.flyn.sarcopenia_project.MainActivity
import com.flyn.sarcopenia_project.R
import com.flyn.sarcopenia_project.connectBle
import com.flyn.sarcopenia_project.file.cache_file.EmgCacheFile
import com.flyn.sarcopenia_project.service.BleAction
import com.flyn.sarcopenia_project.service.BluetoothLeService
import com.flyn.sarcopenia_project.utils.ExtraManager
import com.flyn.sarcopenia_project.utils.FileManager
import com.github.psambit9791.jdsp.signal.peaks.FindPeak
import com.github.psambit9791.jdsp.signal.peaks.Peak
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class DataViewer: AppCompatActivity() {

    companion object {
        private const val TAG = "Data Viewer"
        private val tagName = listOf("Foot Pressure")
    }

    private val emg = DataPage(0f, 10000f, "Hallux","LT","M1","M5","Arch","HM") { "%.2f Pa".format(it) }
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
//                        Log.i("adc",adc.toList().toString())

                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0)-247.73,0)//right
                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1)-177.81,1)
                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2)-427.71,2)
                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3)-131.9,3)
                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4)+90.28,4)
                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5)-361.34,5)
//                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0)+20.96,0)//left
//                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1)-61.18,1)
//                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2)-15.05,2)
//                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3)+69.34,3)
//                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4)+55.22,4)
//                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5)+51.35,5)
//                        val ha_gram = calibrate(emgTransform(adc[0]), 0)
//                        val lt_gram = calibrate(emgTransform(adc[1]), 1)
//                        val m1_gram = calibrate(emgTransform(adc[2]), 2)
//                        val m5_gram = calibrate(emgTransform(adc[3]), 3)
//                        val arch_gram = calibrate(emgTransform(adc[4]), 4)
//                        val hm_gram = calibrate(emgTransform(adc[5]), 5)





//                        val out2: Peak = fp.detectTroughs()
//                        val troughs: IntArray = out2.getPeaks()

                        val text = getString(R.string.emg_describe, ha_gram, lt_gram, m1_gram, m5_gram, arch_gram, hm_gram)

//                        val text = getString(R.string.emg_describe,emgTransform(adc1),emgTransform(adc2),emgTransform(adc3),emgTransform(adc4),emgTransform(adc5),emgTransform(adc6))
                        emg.addData(text, ha_gram.toFloat(), lt_gram.toFloat(), m1_gram.toFloat(), m5_gram.toFloat(), arch_gram.toFloat(), hm_gram.toFloat())
                        GlobalScope.launch(Dispatchers.IO) {
                            FileManager.appendRecordData(FileManager.ADC1_FILE, EmgCacheFile(time, adc))
                        }

                        appendData(ha_gram)
//                        val fp = FindPeak(mutableListOfDouble.toDoubleArray())//mutableList
                        val fp = FindPeak(datalist.toDoubleArray())//linked list
                        val out: Peak = fp.detectPeaks()
//                        val peaks: IntArray = out.getPeaks()
//                        val height = out.findPeakHeights(peaks)
                        filteredPeaks1 = out.filterByHeight(2500.0, 20000.0)
                        filteredPeaks_t = out.filterByPeakDistance(filteredPeaks1,100)
                        height = out.findPeakHeights(filteredPeaks_t)
                        if (filteredPeaks_t.toList().isNotEmpty()){
//                            filteredPeaks2.add(height.toList().last().toInt())
//                            filteredPeaks2.add(height.toList().last().toInt())
//                            if( i > 1){
//                                filteredPeaks2.add(height.toList().last().toInt())
//
//                            }

//                            filteredPeaks2.add(i,height.toList().last().toInt())
//                            i++
                            emg.updateSamplingRate(++adcCount, height.toList().last(),(filteredPeaks_t.toList().last().toDouble()/1000))
                        }
                        else{
                            emg.updateSamplingRate(++adcCount, 0.0, 0.0)
                        }
                        if (datalist.size >6000){
                            datalist.removeFirst()
                        }
                        Log.i("peak",filteredPeaks2.toList().toString())
//                        Log.i("count",adcCount.toString())
//                        Log.i("ha",ha_gram.toString())
//                        adc_double = doubleArrayOf(0.0)
//                        adc_double.add
//                        Log.i("array",mutableListOfDouble.toString())
//                        Log.i("lenght",mutableListOfDouble.size.toString())
//                        Log.i("count",adcCount.toString())

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
//    private val analysisButton: Button by lazy {findViewById(R.id.analysis_foot_button)}
//    private lateinit var analysisButton: Button
    private lateinit var address: String
    private lateinit var service: BluetoothLeService
    private lateinit var height : DoubleArray
    private lateinit var filteredPeaks1 : IntArray
    private lateinit var filteredPeaks_t : IntArray

    private var filteredPeaks2 : MutableList<Int> = mutableListOf()
    private var step_duration : MutableList<Double> = mutableListOf()
    private var adcCount = 0
    private var i=0
    private var x=0

    //    private var emgRightCount = 0
//    private var accCount = 0
//    private var gyrCount = 0

    private fun emgTransform(value: Short): Double = value.toDouble() / 4095f * 3.6f
    private fun emgTransform(value: Float): Float = value / 4095f * 3.6f
////
//    private val weight = arrayOf(0.0019, 0.0015, 0.0018, 0.0022, 0.0019, 0.0021) // left
//    private val bias = arrayOf(-0.0284, 0.0487, 0.0306, -0.1587, -0.1181, -0.1096)
    private val weight = arrayOf(0.0014, 0.0008, 0.0016, 0.001, 0.0011,0.0013) //right
    private val bias = arrayOf(0.411, 0.1106, 0.6439, 0.153, -0.0914, 0.4504)
//    private var mutableListOfDouble: MutableList<Double> = mutableListOf()
    private var datalist: LinkedList<Double> = LinkedList()
//    private var storageOldData: MutableList<Double> = mutableListOf()

//    private val weight = arrayOf(1, 1, 1, 1, 1,1) // calibrate
//    private val bias = arrayOf(0, 0, 0, 0, 0, 0)

    private val area = arrayOf(0.05, 0.15, 0.1, 0.1, 0.3, 0.3)
    private fun calibrate(adc: Double, index: Int): Double =
        ((adc + bias[index]) / weight[index]).toDouble()
//    private fun accTransform(value: Short): Float = value.toFloat() / 32767f * 2f
//    private fun accTransform(value: Float): Float = value / 32767f * 2f
//
//    private fun gyrTransform(value: Short): Float = value.toFloat() / 32767f * 250f
//    private fun gyrTransform(value: Float): Float = value / 32767f * 250f
    private fun appendData(data: Double){

        datalist.add(data)


    }

    private fun calculatePa(data: Double ,index: Int): Double = ((data*0.001*9.80665)/(153*0.0001*area[index]))


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

        val analysisButton: Button = findViewById<Button>(R.id.anaylysis_foot_button)

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

        analysisButton.setOnClickListener {
            MaterialAlertDialogBuilder(this).apply {
                var velocity = (10 / ((filteredPeaks2.toMutableList().last().toDouble()-filteredPeaks2.toMutableList().first().toDouble())/1000))
                var average_peak_value = height.average()
                for (i in 0 until  filteredPeaks2.toMutableList().size-1){
                    step_duration.add(i,filteredPeaks2.get(i+1).toDouble()-filteredPeaks2.get(i).toDouble() )
                }
//                step_duration.add(0,(filteredPeaks1.get(1).toDouble()-filteredPeaks1.get(0).toDouble()))
                var step_duration_average = step_duration.average()
                emg.updataAnalysisData(velocity,step_duration_average,average_peak_value)
                Log.i("velocity", velocity.toString())

            }
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