package com.example.pantrify.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val _message = MutableStateFlow("Siap")
    val message: StateFlow<String> = _message
}
