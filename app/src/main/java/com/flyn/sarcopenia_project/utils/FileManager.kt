package com.flyn.sarcopenia_project.utils

import android.util.Log
import com.flyn.sarcopenia_project.file.cache_file.CacheFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock

object FileManager {

    const val ADC1_FILE = "ADC.csv"

//    const val IMU_ACC_FILE_NAME = "imu_acc.csv"
//    const val IMU_GYR_FILE_NAME = "imu_gyr.csv"

    private const val TAG = "FILE_MANAGER"

    private val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'.csv'", Locale("zh", "tw"))
    private val lock = ReentrantLock()

    internal lateinit var APP_DIR: File
    internal lateinit var CACHE_DIR: File

    fun appendRecordData(fileName: String, data: CacheFile) {
        lock.lock()
        val dir = File(APP_DIR, "temp_record")
        if (!dir.exists()) dir.mkdir()
        FileOutputStream(File(dir, fileName), true).use { out ->
            out.write(data.toCsv().toByteArray())
        }
        lock.unlock()
    }

    fun removeTempRecord() {
        lock.lock()
        File(APP_DIR, "temp_record").deleteRecursively()
        lock.unlock()
    }

    fun writeRecordFile(adcCount: Int) {
        lock.lock()
        val dir = File(APP_DIR, "foot_pressure_record")
        if (!dir.exists()) dir.mkdir()
        val filePath = dataFormat.format(Date()).replace(":", "-")
        FileOutputStream(File(dir, filePath)).use { out ->
            out.write("ADC, $adcCount\n".toByteArray())
            copyCacheFile(out, ADC1_FILE)
        }
        lock.unlock()
    }

    private fun copyCacheFile(out: FileOutputStream, fileName: String) {
        val dir = File(APP_DIR, "temp_record")
        File(dir, fileName).let { file ->
            if (!file.exists()) return
            FileInputStream(file).use { input ->
                val buffer = ByteArray(1024)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    Log.d(TAG, "File write - $fileName, length: $len")
                    out.write(buffer, 0 , len)
                }
            }
        }
    }

}