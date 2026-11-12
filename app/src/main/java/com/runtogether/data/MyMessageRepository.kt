package com.runtogether.data

import com.runtogether.domain.QuickMessage
import com.runtogether.domain.QuickMessagePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ‘내 메시지’ 목록.
 * 휴대폰 앱에서 만들고 고친 내용이 계정 서버에 저장되고, 차량 앱은 받아서 보여 주기만 한다.
 * (차량에서는 만들기 · 고치기 · 지우기 · 순서 바꾸기를 하지 않는다)
 */
interface MyMessageRepository {
    /** 순서대로, 최대 6개 */
    val messages: StateFlow<List<QuickMessage>>

    /** 계정 서버에서 다시 받아 온다 (차량 앱을 켤 때 · 정차할 때 · 서버 알림을 받을 때) */
    suspend fun refresh()
}

/** 시연용: 휴대폰에서 만들어 둔 것으로 가정한 목록을 그대로 돌려준다. */
class DemoMyMessageRepository(initial: List<QuickMessage>) : MyMessageRepository {
    private val _messages = MutableStateFlow(QuickMessagePolicy.forCar(initial))
    override val messages: StateFlow<List<QuickMessage>> = _messages.asStateFlow()

    override suspend fun refresh() = Unit
}
