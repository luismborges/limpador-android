package com.luisborges.limpadorandroid

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.luisborges.limpadorandroid.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica permissões antes de abrir a tela principal
        if (!hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        if (!hasRequiredPermissions()) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_files,
                R.id.navigation_apps,
                R.id.navigation_boost
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun hasRequiredPermissions(): Boolean {
        val storageOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return storageOk
    }
}
