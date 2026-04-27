package com.diplom.autotab

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AutoTabViewModel(private val context: Context) : ViewModel() {

    private val api = TabApiService(context)

    private val _state = MutableStateFlow<AutoTabState>(AutoTabState.Idle)
    val state: StateFlow<AutoTabState> = _state

    // проверка интернета + доступности сервера
    fun checkConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = AutoTabState.CheckingConnection
            val serverOk = api.ping()
            _state.value = if (serverOk)
                AutoTabState.Ready
            else
                AutoTabState.NoConnection
        }
    }

    fun analyze(uri: Uri, tuning: String = "standard") {
        viewModelScope.launch(Dispatchers.IO) {

            _state.value = AutoTabState.Loading("Загружаем файл на сервер…")

            try {
                _state.value = AutoTabState.Loading("Отделяем гитару из микса…")
                val result = api.analyzeAudio(uri, tuning)

                _state.value = AutoTabState.Loading("Строим табулатуру…")
                // небольшая пауза чтобы пользователь увидел сообщение
                kotlinx.coroutines.delay(300)

                _state.value = AutoTabState.Success(result)

            } catch (e: Exception) {
                _state.value = AutoTabState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun reset() {
        _state.value = AutoTabState.Ready
    }

    fun resetWithoutPing() {
        _state.value = AutoTabState.Ready  // просто Ready, без checkConnection
    }
}

sealed class AutoTabState {
    object Idle : AutoTabState()
    object CheckingConnection : AutoTabState()
    object NoConnection : AutoTabState()
    object Ready : AutoTabState()
    data class Loading(val message: String) : AutoTabState()
    data class Success(val result: TabApiResult) : AutoTabState()
    data class Error(val message: String) : AutoTabState()
}