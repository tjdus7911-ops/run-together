package com.runtogether

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.runtogether.ui.AppRoot
import com.runtogether.ui.theme.RunTogetherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: GroupDriveViewModel by viewModels()

    private val vehiclePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onVehiclePermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RunTogetherTheme {
                AppRoot(viewModel)
            }
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            val missing = VEHICLE_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) viewModel.onVehiclePermissionResult()
            else vehiclePermissions.launch(missing.toTypedArray())
        }
    }

    private companion object {
        val VEHICLE_PERMISSIONS = listOf(
            "android.car.permission.CAR_SPEED",
            "android.car.permission.CAR_ENERGY",
            "android.car.permission.CAR_POWERTRAIN",
        )
    }
}
