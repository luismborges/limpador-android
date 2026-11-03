package com.luisborges.limpadorandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.luisborges.limpadorandroid.databinding.ActivityPermissionBinding

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantPermissions.setOnClickListener {
            requestStoragePermissions()
        }

        binding.btnUsageAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        if (hasAllPermissions()) {
            goToMain()
        }
    }

    private fun updateUI() {
        val hasStorage = hasStoragePermission()
        val hasUsage = hasUsageAccessPermission()

        binding.tvStorageStatus.text = if (hasStorage) "✅ Concedida" else "❌ Necessária"
        binding.tvUsageStatus.text = if (hasUsage) "✅ Concedida" else "❌ Necessária"

        binding.btnGrantPermissions.isEnabled = !hasStorage
        binding.btnUsageAccess.isEnabled = !hasUsage
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun hasAllPermissions() = hasStoragePermission() && hasUsageAccessPermission()

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
