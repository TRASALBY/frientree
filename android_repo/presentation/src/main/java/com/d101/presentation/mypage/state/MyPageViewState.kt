package com.d101.presentation.mypage.state

sealed class MyPageViewState {
    abstract val id: String
    abstract val nickname: String
    abstract val alarmStatus: AlarmStatus
    abstract val backgroundMusicStatus: BackgroundMusicStatus
    abstract val backgroundMusic: String

    data class Default(
        override val id: String = "테스트",
        override val nickname: String = "테스트",
        override val backgroundMusicStatus: BackgroundMusicStatus = BackgroundMusicStatus.ON,
        override val alarmStatus: AlarmStatus = AlarmStatus.ON,
        override val backgroundMusic: String = "테스트",
    ) : MyPageViewState()
}
