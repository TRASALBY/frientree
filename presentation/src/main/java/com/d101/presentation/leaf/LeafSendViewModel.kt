package com.d101.presentation.leaf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d101.domain.model.LeafViews
import com.d101.domain.model.Result
import com.d101.domain.model.status.ErrorStatus
import com.d101.domain.model.status.LeafErrorStatus
import com.d101.domain.usecase.main.SendMyLeafUseCase
import com.d101.domain.usecase.usermanagement.ManageUserStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import utils.MutableEventFlow
import utils.asEventFlow
import javax.inject.Inject

@HiltViewModel
class LeafSendViewModel @Inject constructor(
    private val sendLeafUseCase: SendMyLeafUseCase,
    private val manageUserStatusUseCase: ManageUserStatusUseCase,
) : ViewModel() {

    val inputText = MutableStateFlow("")
    var checkedChipId: Int = 0
    private val _uiState = MutableStateFlow<LeafSendViewState>(
        LeafSendViewState.NoSendLeafSendViewState(),
    )
    val uiState: StateFlow<LeafSendViewState> = _uiState.asStateFlow()

    private val _leafEventFlow = MutableEventFlow<LeafSendViewEvent>()
    val leafEventFlow = _leafEventFlow.asEventFlow()

    init {
        viewModelScope.launch {
            manageUserStatusUseCase.getUserStatus().collect {
                setLeafViewState(it.userLeafStatus)
                if (it.userLeafStatus == 0) {
                    _uiState.update { currentState ->
                        LeafSendViewState.AlreadySendState(
                            leafSendTitle = currentState.leafSendTitle,
                        )
                    }
                }
            }
        }
        emitEvent(LeafSendViewEvent.FirstPage)
    }

    private suspend fun setLeafViewState(leftLeavesCount: Int) {
        when (val result = sendLeafUseCase.getMyLeafViews()) {
            is Result.Success -> {
                when (result.data) {
                    LeafViews.ZERO_VIEW.count -> {
                        _uiState.update {
                            LeafSendViewState.ZeroViewLeafSendViewState(
                                leftLeavesCount = leftLeavesCount,
                            )
                        }
                    }

                    LeafViews.NO_SEND.count -> {
                        _uiState.update {
                            LeafSendViewState.NoSendLeafSendViewState(
                                leftLeavesCount = leftLeavesCount,
                            )
                        }
                    }

                    else -> {
                        _uiState.update {
                            LeafSendViewState.SomeViewLeafSateSendView(
                                "당신의 이파리가 일주일간 ${result.data}명에게 힘이 되었어요!",
                                leftLeavesCount = leftLeavesCount,
                            )
                        }
                    }
                }
            }

            is Result.Failure -> {
                when (val errorStatus = result.errorStatus) {
                    LeafErrorStatus.NoSendLeaf() -> {
                        _uiState.update {
                            LeafSendViewState.NoSendLeafSendViewState(
                                leftLeavesCount = leftLeavesCount,
                            )
                        }
                    }

                    ErrorStatus.ServerMaintenance() -> emitEvent(
                        LeafSendViewEvent.OnServerMaintaining(errorStatus.message),
                    )

                    is ErrorStatus.NetworkError -> emitEvent(
                        LeafSendViewEvent.ShowErrorToast(errorStatus.message),
                    )

                    else -> {}
                }
            }
        }
    }

    private fun emitEvent(event: LeafSendViewEvent) {
        viewModelScope.launch {
            _leafEventFlow.emit(event)
        }
    }

    fun onSendLeaf() {
        emitEvent(LeafSendViewEvent.SendLeaf)
    }

    private val _blowing = MutableEventFlow<Int>(0)
    val blowing = _blowing.asEventFlow()
    fun onBlowing() {
        viewModelScope.launch(Dispatchers.Default) {
            for (i in 1..30) {
                delay(50)
                _blowing.emit(1)
            }
        }
    }

    fun onReadyToSend() {
        viewModelScope.launch {
            when (val result = sendLeafUseCase.sendLeaf(checkedChipId, inputText.value)) {
                is Result.Success -> {
                    manageUserStatusUseCase.updateUserStatus()
                    emitEvent(LeafSendViewEvent.ReadyToSend)
                }

                is Result.Failure -> {
                    when (val errorStatus = result.errorStatus) {
                        is ErrorStatus.ServerMaintenance -> {
                            emitEvent(LeafSendViewEvent.OnServerMaintaining(errorStatus.message))
                        }

                        is ErrorStatus.NetworkError -> emitEvent(
                            LeafSendViewEvent.ShowErrorToast(errorStatus.message),
                        )

                        else -> {}
                    }
                }
            }
        }
    }

    fun onNeedPermission(message: String) {
        emitEvent(LeafSendViewEvent.ShowErrorToast(message))
    }
}
