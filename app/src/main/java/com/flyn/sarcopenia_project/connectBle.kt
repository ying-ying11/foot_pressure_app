package com.flyn.sarcopenia_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.flyn.sarcopenia_project.file.FileManagerActivity
import com.flyn.sarcopenia_project.service.ScanDeviceActivity

class connectBle : AppCompatActivity() {


    private val dataViewerButton: Button by lazy { findViewById(R.id.main_data_viewer) }
    private val fileManagerButton: Button by lazy { findViewById(R.id.main_file_manager) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect_ble)

        dataViewerButton.setOnClickListener {
            startActivity(Intent(this, ScanDeviceActivity::class.java))
        }

        fileManagerButton.setOnClickListener {
            startActivity(Intent(this, FileManagerActivity::class.java))
        }

    }
}