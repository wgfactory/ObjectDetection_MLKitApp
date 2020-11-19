package com.wgf.objectdetectionapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {

    var mFirebaseRemoteConfig:FirebaseRemoteConfig? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        var splash_background = mFirebaseRemoteConfig!!.getString(getString(R.string.rc_background))
        //19.2.0
        var configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mFirebaseRemoteConfig!!.setConfigSettings(configSettings)
        mFirebaseRemoteConfig!!.setDefaults(R.xml.remote_config_defaults)

        /* 20.0.1
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setFetchTimeoutInSeconds(0)
                .setMinimumFetchIntervalInSeconds(0)
                .build()*/

        /* 20.0.1
        mFirebaseRemoteConfig!!.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig!!.setDefaultsAsync(R.xml.remote_config_defaults)*/

        // 배경설정
        splash_linear_layout.setBackgroundColor(Color.parseColor(splash_background));

        mFirebaseRemoteConfig!!.fetch()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        mFirebaseRemoteConfig!!.activate()
                    } else {
                        Toast.makeText(this, "Fetch Failed", Toast.LENGTH_SHORT).show()
                    }
                    displayWelcomeMessage()
                }
    }

    fun displayWelcomeMessage() {
        var caps = mFirebaseRemoteConfig!!.getBoolean(getString(R.string.rc_caps))
        var splash_message = mFirebaseRemoteConfig!!.getString(getString(R.string.rc_message))

        if(caps) {
            val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
                    .setMessage(splash_message)
                    .setPositiveButton("확인") {dialog, which ->
                        finish()
                    }
            alertDialog.create().show()

        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}