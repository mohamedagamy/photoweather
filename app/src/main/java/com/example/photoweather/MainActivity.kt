package com.example.photoweather

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.photoweather.databinding.ActivityMainBinding
import com.example.photoweather.ui.WeatherViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


@AndroidEntryPoint
class MainActivity : AppCompatActivity(),EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMainBinding
    val weatherViewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, RC_CAMERA_AND_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA)
                .build()
        )
    }
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }
    fun onRequestPermissionsResult(
        requestCode: Int?,
        permissions: Array<String?>?,
        grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode!!, permissions!!, grantResults!!)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    companion object{
        val RC_CAMERA_AND_STORAGE = 300
    }

}