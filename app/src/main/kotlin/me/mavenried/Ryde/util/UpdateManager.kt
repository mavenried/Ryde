package me.mavenried.Ryde.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UpdateManager {

    enum class CheckState { IDLE, CHECKING, UP_TO_DATE, ERROR }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _checkState = MutableStateFlow(CheckState.IDLE)
    val checkState: StateFlow<CheckState> = _checkState.asStateFlow()

    fun checkAsync() {
        if (_checkState.value == CheckState.CHECKING) return
        _checkState.value = CheckState.CHECKING
        scope.launch {
            UpdateChecker.checkForUpdate()
                .onSuccess { info ->
                    _updateInfo.value = info
                    _checkState.value = if (info != null) CheckState.IDLE else CheckState.UP_TO_DATE
                }
                .onFailure {
                    _checkState.value = CheckState.ERROR
                }
        }
    }

    fun dismiss() {
        _updateInfo.value = null
        _checkState.value = CheckState.IDLE
    }
}
