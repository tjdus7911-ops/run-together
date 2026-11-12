package com.runtogether.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.runtogether.domain.Gear
import com.runtogether.domain.VehicleSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

/** 내 차의 상태(시동·기어·속도·연료/배터리)를 흘려보낸다. */
interface VehicleDataSource {
    val snapshots: Flow<VehicleSnapshot>
}

object VehicleDataSources {
    /** 차량(AAOS)에서만 실제 차량 데이터를 쓴다. 일반 기기에서는 시연 값으로 동작한다. */
    fun create(context: Context): VehicleDataSource? =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            AaosVehicleDataSource(context.applicationContext)
        } else {
            null
        }
}

/**
 * AAOS 차량 속성(CarPropertyManager)으로 차량 데이터를 읽는다.
 * 차종마다 지원하는 데이터가 다르므로 속성마다 지원 여부를 먼저 확인하고, 없는 값은 비워 둔다.
 */
class AaosVehicleDataSource(private val context: Context) : VehicleDataSource {

    override val snapshots: Flow<VehicleSnapshot> = callbackFlow {
        val car: Car? = try {
            Car.createCar(context)
        } catch (e: Throwable) {
            Log.w(TAG, "Car service unavailable", e)
            null
        }
        val manager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        if (car == null || manager == null) {
            car?.disconnect()
            close()
            return@callbackFlow
        }

        val fuelCapacity = readFloat(manager, VehiclePropertyIds.INFO_FUEL_CAPACITY)
        val batteryCapacity = readFloat(manager, VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY)
        var current = VehicleSnapshot()

        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                current = applyValue(current, value, fuelCapacity, batteryCapacity)
                trySend(current)
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.w(TAG, "property error $propertyId/$areaId")
            }
        }

        val subscriptions = listOf(
            VehiclePropertyIds.IGNITION_STATE to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.GEAR_SELECTION to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.PERF_VEHICLE_SPEED to CarPropertyManager.SENSOR_RATE_NORMAL,
            VehiclePropertyIds.FUEL_LEVEL to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.EV_BATTERY_LEVEL to CarPropertyManager.SENSOR_RATE_ONCHANGE,
            VehiclePropertyIds.RANGE_REMAINING to CarPropertyManager.SENSOR_RATE_ONCHANGE,
        )
        for ((propertyId, rate) in subscriptions) {
            try {
                if (manager.getCarPropertyConfig(propertyId) == null) continue // 이 차는 지원하지 않음
                @Suppress("DEPRECATION")
                manager.registerCallback(callback, propertyId, rate)
            } catch (e: SecurityException) {
                Log.w(TAG, "no permission for property $propertyId")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "unsupported property $propertyId")
            }
        }

        awaitClose {
            try {
                manager.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.w(TAG, "unregister failed", e)
            }
            car.disconnect()
        }
    }

    private fun readFloat(manager: CarPropertyManager, propertyId: Int): Float? = try {
        if (manager.getCarPropertyConfig(propertyId) == null) null else manager.getFloatProperty(propertyId, 0)
    } catch (e: Exception) {
        null
    }

    private fun applyValue(
        s: VehicleSnapshot,
        v: CarPropertyValue<*>,
        fuelCapacity: Float?,
        batteryCapacity: Float?,
    ): VehicleSnapshot {
        val value = v.value ?: return s
        return when (v.propertyId) {
            VehiclePropertyIds.IGNITION_STATE ->
                s.copy(ignitionOn = ((value as? Int) ?: 0) >= IGNITION_STATE_ON)
            VehiclePropertyIds.GEAR_SELECTION ->
                s.copy(gear = gearOf((value as? Int) ?: 0))
            VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                s.copy(speedKmh = (((value as? Float) ?: 0f) * 3.6f).toDouble())
            VehiclePropertyIds.FUEL_LEVEL ->
                if (fuelCapacity != null && fuelCapacity > 0f) {
                    s.copy(energyPercent = (((value as? Float) ?: 0f) / fuelCapacity * 100f).roundToInt())
                } else s
            VehiclePropertyIds.EV_BATTERY_LEVEL ->
                if (batteryCapacity != null && batteryCapacity > 0f) {
                    s.copy(energyPercent = (((value as? Float) ?: 0f) / batteryCapacity * 100f).roundToInt())
                } else s
            VehiclePropertyIds.RANGE_REMAINING ->
                s.copy(rangeKm = (((value as? Float) ?: 0f) / 1000f).toDouble())
            else -> s
        }
    }

    private fun gearOf(raw: Int): Gear = when {
        raw == GEAR_PARK -> Gear.PARK
        raw == GEAR_REVERSE -> Gear.REVERSE
        raw == GEAR_NEUTRAL -> Gear.NEUTRAL
        raw >= GEAR_DRIVE -> Gear.DRIVE // D 및 수동 1~9단
        else -> Gear.UNKNOWN
    }

    private companion object {
        const val TAG = "AaosVehicle"
        // android.car.VehicleIgnitionState / VehicleGear 값
        const val IGNITION_STATE_ON = 4
        const val GEAR_NEUTRAL = 1
        const val GEAR_REVERSE = 2
        const val GEAR_PARK = 4
        const val GEAR_DRIVE = 8
    }
}
