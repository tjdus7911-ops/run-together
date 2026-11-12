package com.runtogether.data

import com.runtogether.domain.GroupMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 모임 메시지 통로. 기본 상황 공유 · 내 메시지 · 음성 메시지가 모두 이 통로로 오간다.
 * 실제 서비스에서는 동기화 서버(WebSocket)가 모임 단위로, 현재 모임 참여자에게만 전달한다.
 */
interface GroupChannel {
    /** 다른 차에서 온 메시지 (내가 보낸 것은 오지 않는다) */
    val incoming: Flow<GroupMessage>

    /** 현재 모임에 보낸다 */
    fun send(message: GroupMessage)
}

/** 시연용: 보낸 것은 기록만 하고, 다른 회원의 메시지는 시나리오가 흉내 낸다. */
class DemoGroupChannel : GroupChannel {
    private val _incoming = MutableSharedFlow<GroupMessage>(extraBufferCapacity = 16)
    override val incoming: Flow<GroupMessage> = _incoming.asSharedFlow()

    private val _sent = MutableStateFlow<List<GroupMessage>>(emptyList())
    val sent: StateFlow<List<GroupMessage>> = _sent.asStateFlow()

    override fun send(message: GroupMessage) {
        _sent.value = (_sent.value + message).takeLast(50)
    }

    /** 다른 회원이 보낸 것처럼 넣는다 (시연 시나리오) */
    fun simulateIncoming(message: GroupMessage) {
        _incoming.tryEmit(message)
    }
}
