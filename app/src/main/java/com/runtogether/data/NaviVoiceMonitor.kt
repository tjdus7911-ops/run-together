package com.runtogether.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 내비 길 안내 음성이 나오는 중인지. 우리 안내(VoiceQueue)는 이 소리가 끝난 뒤에 읽는다. */
interface NaviVoiceMonitor {
    val guidancePlaying: StateFlow<Boolean>
    fun release()
}

/** 길 안내 소리를 알 수 없는 곳(테스트 등) */
object NoNaviVoice : NaviVoiceMonitor {
    override val guidancePlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override fun release() = Unit
}

/**
 * 안드로이드 공개 API(AudioManager 재생 정보)로 지금 재생 중인 소리의 ‘용도’만 본다. 내용이나 앱 이름은 보지 않는다.
 * 길 안내 용도(USAGE_ASSISTANCE_NAVIGATION_GUIDANCE) 재생이 있으면 내비가 말하는 중으로 본다.
 * 차량마다 내비가 쓰는 용도가 다를 수 있어, 안 보이면 기다리지 않고 순서만 지킨다.
 */
class AndroidNaviVoiceMonitor(context: Context) : NaviVoiceMonitor {
    private val audio: AudioManager? = context.applicationContext.getSystemService(AudioManager::class.java)
    private val _playing = MutableStateFlow(false)
    override val guidancePlaying: StateFlow<Boolean> = _playing.asStateFlow()

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            _playing.value = isGuidance(configs)
        }
    }

    init {
        try {
            _playing.value = isGuidance(audio?.activePlaybackConfigurations)
            audio?.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        } catch (t: Throwable) {
            Log.w(TAG, "playback monitor unavailable", t)
        }
    }

    private fun isGuidance(configs: List<AudioPlaybackConfiguration>?): Boolean =
        configs.orEmpty().any { it.audioAttributes.usage == AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE }

    override fun release() {
        try {
            audio?.unregisterAudioPlaybackCallback(callback)
        } catch (t: Throwable) {
            Log.w(TAG, "unregister failed", t)
        }
    }

    private companion object {
        const val TAG = "NaviVoice"
    }
}
