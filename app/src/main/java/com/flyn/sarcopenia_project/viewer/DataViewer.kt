package com.flyn.sarcopenia_project.viewer


import android.app.Service
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
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

    private val emg = DataPage(0f, 25000f, "Hallux","LT","M1","M5","Arch","HM") { "%.2f Pa".format(it) }
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


//                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0)+260,0)// male right
//                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1)+730,1)
//                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2)+460,2)
//                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3)+430,3)
//                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4)+565,4)
//                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5)+145,5)
//
//                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0)+95,0)// male left
//                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1)+390,1)
//                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2)+85,2)
//                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3)+550,3)
//                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4)+350,4)
//                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5)+100,5)

//                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0),0)-1500// female right
//                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1),1)-600
//                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2),2)-1200
//                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3),3)-1200
//                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4),4)-400
//                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5),5)+300
//
//                        val ha_gram = calculatePa(calibrate(emgTransform(adc[0]), 0),0)// female left
//                        val lt_gram = calculatePa(calibrate(emgTransform(adc[1]), 1),1)
//                        val m1_gram = calculatePa(calibrate(emgTransform(adc[2]), 2),2)
//                        val m5_gram = calculatePa(calibrate(emgTransform(adc[3]), 3),3)
//                        val arch_gram = calculatePa(calibrate(emgTransform(adc[4]), 4),4)
//                        val hm_gram = calculatePa(calibrate(emgTransform(adc[5]), 5),5)

                        val ha_gram = calibrate(emgTransform(adc[0]), 0) //calibrate
                        val lt_gram = calibrate(emgTransform(adc[1]), 1)
                        val m1_gram = calibrate(emgTransform(adc[2]), 2)
                        val m5_gram = calibrate(emgTransform(adc[3]), 3)
                        val arch_gram = calibrate(emgTransform(adc[4]), 4)
                        val hm_gram = calibrate(emgTransform(adc[5]), 5)
                        Log.i("test", adc.toList().toString())



//                        val out2: Peak = fp.detectTroughs()
//                        val troughs: IntArray = out2.getPeaks()

                        val text = getString(R.string.emg_describe, ha_gram, lt_gram, m1_gram, m5_gram, arch_gram, hm_gram)

//                        val text = getString(R.string.emg_describe,emgTransform(adc1),emgTransform(adc2),emgTransform(adc3),emgTransform(adc4),emgTransform(adc5),emgTransform(adc6))
                        emg.addData(text, ha_gram.toFloat(), lt_gram.toFloat(), m1_gram.toFloat(), m5_gram.toFloat(), arch_gram.toFloat(), hm_gram.toFloat())

                        datalist.add(ha_gram)
//                        val fp = FindPeak(mutableListOfDouble.toDoubleArray())//mutableList

                        val fp = FindPeak(datalist.toDoubleArray())//linked list
                        val out: Peak = fp.detectPeaks()
                        var isFatigue = false
//                        val peaks: IntArray = out.getPeaks()
//                        val height = out.findPeakHeights(peaks)
                        filteredPeaks1 = out.filterByHeight(1000.0, 20000.0)
                        filteredPeaks_t = out.filterByPeakDistance(filteredPeaks1,50)
//                        fpArray.add(filteredPeaks_t.last())
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
                        if (datalist.size >= 1000){
                            Log.i("height", height.toList().toString())
                            if (time - time_start >= 10000){  //10000、15000、200000 By experimental protocol testing App whether detecting fatigue or not
                                if(height.isNotEmpty()){
                                    heightAverage = height.average()
                                    PressureList.add(heightAverage)
                                    if(PressureList.size >= 2){
                                        if(PressureList.get(PressureList.size-1)<PressureList.first && PressureList.get(PressureList.size-1)<PressureList.get(PressureList.size-2)){
                                            i++
                                            if(i>=3){
                                                isFatigue = true
                                                Toast.makeText(this@DataViewer,"Fatigue Detected",Toast.LENGTH_LONG).show()
                                                vibrator = getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
                                                vibrationEffect = VibrationEffect.createOneShot(1000, 150)
                                                vibrator.vibrate(vibrationEffect)
//                                                vibrator.vibrate(1000)
                                                i = 0
                                            }
                                        }
                                        else{
                                            i = 0
                                        }
                                    }
                                }
                                time_start = time
                            }
                            datalist.removeFirst()
//                            datalist.clear()
                        }
//                        Log.i("peak",PressureList.toList().toString())
//                        Log.i("count",datalist.size.toString())
//                        Log.i("ha",ha_gram.toString())
//                        adc_double = doubleArrayOf(0.0)
//                        adc_double.add
//                        Log.i("array",mutableListOfDouble.toString())
//                        Log.i("lenght",mutableListOfDouble.size.toString())
//                        Log.i("count",adcCount.toString())

                        GlobalScope.launch(Dispatchers.IO) {
                            FileManager.appendRecordData(FileManager.ADC1_FILE, EmgCacheFile(time, adc, heightAverage, isFatigue))
                        }

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
    lateinit var vibrator: Vibrator
    
    lateinit var vibrationEffect: VibrationEffect
    private lateinit var address: String
    private lateinit var service: BluetoothLeService
    private lateinit var height : DoubleArray
    private var heightAverage : Double = 0.0
    private lateinit var filteredPeaks1 : IntArray
    private lateinit var filteredPeaks_t : IntArray
    private lateinit var fpArray : LinkedList<Int>
    private var time_start: Long = 0
    private var analysis_peakvalue : MutableList<Double> = mutableListOf()
    private var step_duration : MutableList<Double> = mutableListOf()
    private var adcCount = 0
    private var i=0
    private var x=0

    //    private var emgRightCount = 0
//    private var accCount = 0
//    private var gyrCount = 0

    private fun emgTransform(value: Short): Double = (value.toDouble() / 4095f) * 3.6f
    private fun emgTransform(value: Float): Float = value / 4095f * 3.6f
////
//    private val weight = arrayOf(0.0015, 0.0017, 0.0017, 0.0016, 0.0015, 0.0015) //  male left
//    private val bias = arrayOf(-0.1376, -0.6738, -0.1653, -0.8853, -0.5407, -0.3266)
//    private val weight = arrayOf(0.0016, 0.0015, 0.0015, 0.0016, 0.0015,0.0018) // male right
//    private val bias = arrayOf(-0.4404, -1.1027, -0.7006, -0.693, -0.8453, -0.3081)

//    private val weight = arrayOf(0.0004, 0.0003, 0.0004, 0.0004, 0.0004, 0.0002) //  female left
//    private val bias = arrayOf(0.0214, 0.0206, 0.0523, 0.0085, 0.0171, 0.0351)
//    private val weight = arrayOf(0.0004, 0.0003, 0.0003, 0.0003, 0.0003,0.0004) // female right
//    private val bias = arrayOf(0.0578, 0.0367, 0.0538, 0.053, 0.0634, -0.0621)

    private val weight = arrayOf(0.0016, 0.0017, 0.0012, 0.0013, 0.0015, 0.0015) //  new female left
    private val bias = arrayOf(-0.1774, 0.0176, -0.0905, 0.0562, -0.0002, 0.0273)
//    private val weight = arrayOf(0.0017, 0.0016, 0.0012, 0.0017, 0.0018,0.0017) // new female right
//    private val bias = arrayOf(0.0492, -0.0048, 0.0744, 0.1325, -0.0666, 0.0711)

//    private val weight = arrayOf(1, 1, 1, 1, 1,1) // calibrate
//    private val bias = arrayOf(0, 0, 0, 0, 0, 0)

//    private var mutableListOfDouble: MutableList<Double> = mutableListOf()
    private var datalist: LinkedList<Double> = LinkedList()
    private var PressureList: LinkedList<Double> = LinkedList()
    private var PressureAverageList: LinkedList<Double> = LinkedList()
//    private var storageOldData: MutableList<Double> = mutableListOf()



    private val area = arrayOf(0.05, 0.15, 0.1, 0.1, 0.3, 0.3)
    private fun calibrate(adc: Double, index: Int): Double =
        ((adc + bias[index]) / weight[index]).toDouble()
//    private fun accTransform(value: Short): Float = value.toFloat() / 32767f * 2f
//    private fun accTransform(value: Float): Float = value / 32767f * 2f
//
//    private fun gyrTransform(value: Short): Float = value.toFloat() / 32767f * 250f
//    private fun gyrTransform(value: Float): Float = value / 32767f * 250f
    private fun appendData(list: LinkedList<Double>,data: Double){

        list.add(data)


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
                if(filteredPeaks_t.isNotEmpty()) {
                    var velocity = (10 / ((filteredPeaks_t.toMutableList().last()
                        .toDouble() - filteredPeaks_t.toMutableList().first().toDouble()) / 1000))
                    var average_peak_value = height.average()
                    for (i in 0 until filteredPeaks_t.toMutableList().size - 1) {
                        step_duration.add(i,
                            filteredPeaks_t.get(i + 1).toDouble() - filteredPeaks_t.get(i)
                                .toDouble()
                        )
                    }
//                step_duration.add(0,(filteredPeaks1.get(1).toDouble()-filteredPeaks1.get(0).toDouble()))
                    var step_duration_average = step_duration.average()
                    emg.updataAnalysisData(velocity, step_duration_average, average_peak_value)
                    Log.i("velocity", velocity.toString())
                }
                else{
                    Toast.makeText(this@DataViewer,"no peak data",Toast.LENGTH_SHORT).show()
                }
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