package com.d101.presentation.main.state

sealed class LeafState {
    abstract val leafSendTitle: String
    abstract val restrictedSend: String
    data class NoSendLeafState(
        override val leafSendTitle: String = "다른 사람들에게 힘이 되는 이파리를 날려보세요!",
        override val restrictedSend: String = "이파리는 하루에 한 번 보낼 수 있어요!\n내일 다시 보내러 와주세요 :)",
    ) : LeafState()

    data class ZeroViewLeafState(
        override val leafSendTitle: String = "당신의 이파리가 날아다니는 중이에요!",
        override val restrictedSend: String = "이파리는 하루에 한 번 보낼 수 있어요!\n내일 다시 보내러 와주세요 :)",
    ) : LeafState()

    data class SomeViewLeafState(
        override val leafSendTitle: String = "",
        override val restrictedSend: String = "이파리는 하루에 한 번 보낼 수 있어요!\n내일 다시 보내러 와주세요 :)",
    ) : LeafState()

    data class AlreadySendState(
        override val leafSendTitle: String,
        override val restrictedSend: String,
    ) : LeafState()
}
