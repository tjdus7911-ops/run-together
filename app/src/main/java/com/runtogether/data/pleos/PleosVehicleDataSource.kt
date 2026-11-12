package com.runtogether.data.pleos

import ai.pleos.playground.vehicle.Vehicle
import android.content.Context
import android.util.Log
import com.runtogether.data.VehicleDataSource
import com.runtogether.domain.Gear
import com.runtogether.domain.VehicleSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

/**
 * Pleos Vehicle SDK: 기어, 연료(%), 전기차 배터리(%), 주행 가능 거리, 이번 주행의 연속 운전 시간.
 */
class PleosVehicleDataSource(private val context: Context) : VehicleDataSource {

    override val snapshots: Flow<VehicleSnapshot> = callbackFlow {
        val vehicle: Vehicle? = try {
            Vehicle(context).also { it.initialize() }
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos Vehicle SDK unavailable", t)
            null
        }
        if (vehicle == null) {
            close()
            return@callbackFlow
        }

        val lock = Any()
        var current = VehicleSnapshot(gear = Gear.UNKNOWN)
        fun update(change: (VehicleSnapshot) -> VehicleSnapshot) {
            val next = synchronized(lock) {
                current = change(current)
                current
            }
            trySend(next)
        }

        val unsubscribe = PleosBridge.subscribeVehicle(
            vehicle,
            { name -> update { it.copy(gear = gearOf(name)) } },
            { percent -> if (percent != null) update { it.copy(energyPercent = percent) } },
            { percent -> if (percent != null) update { it.copy(energyPercent = percent) } },
            { range -> if (range != null) update { it.copy(rangeKm = rangeToKm(range)) } },
            { time -> if (time != null) update { it.copy(tripDrivingMinutes = drivingTimeToMinutes(time)) } },
        )

        awaitClose {
            unsubscribe.run()
            try {
                vehicle.release()
            } catch (t: Throwable) {
                Log.w(TAG, "release failed", t)
            }
        }
    }

    private fun gearOf(name: String?): Gear = when (name) {
        "GEAR_PARK" -> Gear.PARK
        "GEAR_REVERSE" -> Gear.REVERSE
        "GEAR_NEUTRAL" -> Gear.NEUTRAL
        null, "GEAR_UNKNOWN" -> Gear.UNKNOWN
        else -> Gear.DRIVE // D 및 1~9단
    }

    /** AAOS 기준은 미터. km 로 오는 차종도 있어 크기로 구분한다. */
    private fun rangeToKm(value: Float): Double = if (value > 2_000f) value / 1_000.0 else value.toDouble()

    /** 초 단위로 오는 경우를 대비해 크기로 구분한다. */
    private fun drivingTimeToMinutes(value: Int): Int = if (value > 600) value / 60 else value

    private companion object {
        const val TAG = "PleosVehicle"
    }
}

/**
 * AAOS 표준 속성(시동·속도)과 Pleos Vehicle SDK(기어·연료·배터리·주행 가능 거리·연속 운전 시간)를 합친다.
 * 같은 값이 둘 다 있으면 Pleos SDK 값을 쓴다.
 */
class CombinedVehicleDataSource(
    private val aaos: VehicleDataSource,
    private val pleos: VehicleDataSource,
) : VehicleDataSource {
    override val snapshots: Flow<VehicleSnapshot> = combine(
        aaos.snapshots.onStart { emit(VehicleSnapshot(gear = Gear.UNKNOWN)) },
        pleos.snapshots.onStart { emit(VehicleSnapshot(gear = Gear.UNKNOWN)) },
    ) { a, p ->
        VehicleSnapshot(
            ignitionOn = a.ignitionOn || p.ignitionOn,
            gear = if (p.gear != Gear.UNKNOWN) p.gear else a.gear,
            speedKmh = a.speedKmh,
            energyPercent = p.energyPercent ?: a.energyPercent,
            rangeKm = p.rangeKm ?: a.rangeKm,
            tripDrivingMinutes = p.tripDrivingMinutes ?: a.tripDrivingMinutes,
        )
    }.distinctUntilChanged()
}
