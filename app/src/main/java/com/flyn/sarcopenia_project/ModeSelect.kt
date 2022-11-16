package com.flyn.sarcopenia_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import com.flyn.sarcopenia_project.file.FileManagerActivity
import com.flyn.sarcopenia_project.service.ScanDeviceActivity

class ModeSelect : AppCompatActivity() {


    private val dataViewerButton: ImageButton by lazy { findViewById(R.id.cop_button) }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_select)

        dataViewerButton.setOnClickListener {
            startActivity(Intent(this, connectBle::class.java))
        }

    }
}