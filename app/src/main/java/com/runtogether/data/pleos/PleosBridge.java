package com.runtogether.data.pleos;

import android.util.Log;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.constant.control.VehicleGear;
import ai.pleos.playground.vehicle.listener.DrivingTimeListener;
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener;
import ai.pleos.playground.vehicle.listener.FuelPercentListener;
import ai.pleos.playground.vehicle.listener.GearSelectionListener;
import ai.pleos.playground.vehicle.listener.RangeRemainingListener;

/**
 * Pleos Connect SDK 의 리스너 구현과 getter 호출을 자바에서 감싼다.
 * SDK 는 Kotlin 으로 작성돼 있어, 자바 쪽에서 붙이면 SDK 버전이 바뀌어도
 * (널 가능 여부, 프로퍼티/함수 표기 같은) 세부에 덜 민감하다.
 */
public final class PleosBridge {
    private static final String TAG = "PleosBridge";

    private PleosBridge() {
    }

    public interface TextSink {
        void accept(String value);
    }

    public interface IntSink {
        void accept(Integer value);
    }

    public interface FloatSink {
        void accept(Float value);
    }

    public interface SpeechSink {
        void onText(String text, boolean isFinal);

        void onFinished();
    }

    // ---------------- Vehicle SDK ----------------

    /**
     * 기어·연료·배터리·주행 가능 거리·연속 운전 시간을 구독한다.
     * 차종마다 지원하는 데이터가 다르므로 하나가 실패해도 나머지는 계속한다.
     *
     * @return 실행하면 구독을 모두 푸는 Runnable
     */
    public static Runnable subscribeVehicle(
            Vehicle vehicle,
            TextSink gear,
            IntSink fuelPercent,
            IntSink evStateOfCharge,
            FloatSink rangeRemaining,
            IntSink tripDrivingTime) {
        final GearSelectionListener gearListener = new GearSelectionListener() {
            @Override
            public void onSelected(VehicleGear value) {
                gear.accept(value == null ? "GEAR_UNKNOWN" : value.name());
            }
        };
        final FuelPercentListener fuelListener = new FuelPercentListener() {
            @Override
            public void onFuelPercentUpdated(Integer value) {
                fuelPercent.accept(value);
            }
        };
        final EvBatteryStateOfChargeListener socListener = new EvBatteryStateOfChargeListener() {
            @Override
            public void onEvBatteryStateOfChargeUpdated(Integer value) {
                evStateOfCharge.accept(value);
            }
        };
        final RangeRemainingListener rangeListener = new RangeRemainingListener() {
            @Override
            public void onRangeRemainingChanged(Float value) {
                rangeRemaining.accept(value);
            }
        };
        final DrivingTimeListener timeListener = new DrivingTimeListener() {
            @Override
            public void onDrivingTimeUpdated(Integer value) {
                tripDrivingTime.accept(value);
            }
        };

        safely("gear", () -> vehicle.getGear().registerGearSelection(gearListener));
        safely("fuel", () -> vehicle.getFuel().registerFuelPercent(fuelListener));
        safely("ev", () -> vehicle.getEvBattery().registerEvBatteryStateOfCharge(socListener));
        safely("range", () -> vehicle.getOdometer().registerRangeRemainingListener(rangeListener));
        safely("time", () -> vehicle.getOdometer().registerTripCurrentDrivingTime(timeListener));

        return () -> {
            safely("gear off", () -> vehicle.getGear().unregisterGearSelection(gearListener));
            safely("fuel off", () -> vehicle.getFuel().unregisterFuelPercent(fuelListener));
            safely("ev off", () -> vehicle.getEvBattery().unregisterEvBatteryStateOfCharge(socListener));
            safely("range off", () -> vehicle.getOdometer().unregisterRangeRemainingListener(rangeListener));
            safely("time off", () -> vehicle.getOdometer().unregisterTripCurrentDrivingTime(timeListener));
        };
    }

    // ---------------- TextToSpeech SDK ----------------

    public static ai.pleos.playground.tts.listener.OnInitializeListener ttsInit(Runnable ok, Runnable fail) {
        return new ai.pleos.playground.tts.listener.OnInitializeListener() {
            @Override
            public void onSuccess() {
                ok.run();
            }

            @Override
            public void onFailure(ai.pleos.playground.tts.constant.ErrorType error) {
                Log.w(TAG, "TTS init failed: " + error);
                fail.run();
            }
        };
    }

    public static ai.pleos.playground.tts.listener.EventListener ttsEvents(Runnable finished, Runnable failed) {
        return new ai.pleos.playground.tts.listener.EventListener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onReady() {
            }

            @Override
            public void onDone() {
                finished.run();
            }

            @Override
            public void onError(ai.pleos.playground.tts.constant.ErrorType error) {
                Log.w(TAG, "TTS error: " + error);
                failed.run();
            }

            @Override
            public void onStop() {
                finished.run();
            }

            @Override
            public void onUpdatedRms(double rms) {
            }
        };
    }

    // ---------------- SpeechToText SDK ----------------

    public static ai.pleos.playground.stt.listener.OnInitializeListener sttInit(Runnable ok, Runnable fail) {
        return new ai.pleos.playground.stt.listener.OnInitializeListener() {
            @Override
            public void onSuccess() {
                ok.run();
            }

            @Override
            public void onFailure(ai.pleos.playground.stt.constant.ErrorType error) {
                Log.w(TAG, "STT init failed: " + error);
                fail.run();
            }
        };
    }

    public static ai.pleos.playground.stt.listener.ResultListener sttResults(SpeechSink sink) {
        return new ai.pleos.playground.stt.listener.ResultListener() {
            @Override
            public void onUpdated(String text, boolean isFinal) {
                sink.onText(text, isFinal);
            }

            @Override
            public void onUpdatedEpdData(long start, long end) {
            }

            @Override
            public void onStartedRecognition() {
            }

            @Override
            public void onEndedRecognition() {
                sink.onFinished();
            }

            @Override
            public void onError(ai.pleos.playground.stt.constant.ErrorType error) {
                Log.w(TAG, "STT error: " + error);
                sink.onFinished();
            }

            @Override
            public void onReady() {
            }

            @Override
            public void onUpdatedRms(double rms) {
            }
        };
    }

    private static void safely(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            Log.w(TAG, "Pleos vehicle " + what + " unavailable", t);
        }
    }
}
