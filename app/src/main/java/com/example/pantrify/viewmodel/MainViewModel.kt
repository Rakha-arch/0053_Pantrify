package com.example.pantrify.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrify.model.*
import com.example.pantrify.network.RetrofitClient
import com.example.pantrify.network.ServerConfig
import com.example.pantrify.network.SessionStore
import com.example.pantrify.notification.ExpiryNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime

data class UiState(
    val loading: Boolean = false,
    val user: User? = null,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val inventory: List<Inventory> = emptyList(),
    val error: String? = null,
    val info: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app
    private val api get() = RetrofitClient.apiService
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** Menerjemahkan error HTTP/IO menjadi pesan berbahasa Indonesia. */
    private fun errorMessage(e: Exception): String {
        if (e is HttpException) {
            val raw = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val detail = runCatching {
                val json = JSONObject(raw ?: "")
                when (val value = json.opt("detail")) {
                    is String -> value
                    is JSONArray -> value.optJSONObject(0)?.optString("msg").orEmpty()
                    else -> ""
                }
            }.getOrNull().orEmpty()
            return when {
                detail.isNotBlank() -> detail.replace("Value error, ", "")
                e.code() == 401 -> "Sesi berakhir. Silakan masuk kembali."
                e.code() == 403 -> "Akses ditolak."
                e.code() == 422 -> "Data tidak valid. Periksa kembali isian formulir."
                else -> "Permintaan gagal (HTTP ${e.code()})"
            }
        }
        if (e is IOException) {
            return "Tidak dapat terhubung ke server (${ServerConfig.baseUrl}). Pastikan backend berjalan dan alamatnya benar."
        }
        return e.message ?: "Terjadi kesalahan"
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, info = null)
            try {
                withContext(Dispatchers.IO) {
                    ServerConfig.resolve()
                    block()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = errorMessage(e))
            } finally {
                _state.value = _state.value.copy(loading = false)
            }
        }
    }

    private suspend fun loadAll() {
        val categories = api.categories().data.orEmpty()
        val products = api.products().data.orEmpty()
        val inventory = api.inventory().data.orEmpty()
        _state.value = _state.value.copy(
            categories = categories,
            products = products,
            inventory = inventory
        )
    }

    fun login(email: String, password: String) = run {
        val response = api.login(LoginRequest(email, password))
        val data = response.data ?: error(response.message ?: "Login gagal")
        SessionStore.accessToken = data.tokens?.access_token
        SessionStore.refreshToken = data.tokens?.refresh_token
        _state.value = _state.value.copy(user = data.user, info = "Login berhasil")
        loadAll()
    }

    fun register(name: String, email: String, password: String) = run {
        val response = api.register(RegisterRequest(name, email, password))
        if (response.data == null) error(response.message ?: "Registrasi gagal")
        SessionStore.accessToken = null
        SessionStore.refreshToken = null
        _state.value = UiState(info = "Registrasi berhasil. Silakan masuk dengan email dan password tersebut.")
    }

    fun logout() = run {
        SessionStore.refreshToken?.let { api.logout(RefreshRequest(it)) }
        SessionStore.accessToken = null
        SessionStore.refreshToken = null
        _state.value = UiState(info = "Logout berhasil")
    }

    fun refresh() = run { loadAll() }

    fun searchProducts(query: String) = run {
        _state.value = _state.value.copy(products = api.products(query).data.orEmpty())
    }

    /** Membuat produk sekaligus batch stok awal. */
    fun addProductWithStock(
        categoryId: Int?,
        name: String,
        unit: String,
        minimum: Double,
        qty: Double,
        expiry: String
    ) = run {
        val created = api.createProduct(ProductRequest(categoryId, name, unit, minimum)).data
            ?: error("Produk gagal dibuat")
        val productId = created.id ?: error("ID produk tidak tersedia")
        val now = LocalDateTime.now().withNano(0).toString()
        val batch = api.createInventory(InventoryRequest(productId, qty, now, expiry))
        batch.data?.id?.let { ExpiryNotification.schedule(application, it, name, expiry) }
        loadAll()
        _state.value = _state.value.copy(info = "Produk \"$name\" ditambahkan")
    }

    fun updateProduct(id: Int, categoryId: Int?, name: String, unit: String, minimum: Double) = run {
        api.updateProduct(id, ProductRequest(categoryId, name, unit, minimum))
        loadAll()
        _state.value = _state.value.copy(info = "Produk diperbarui")
    }

    fun addBatch(productId: Int, qty: Double, expiry: String) = run {
        val now = LocalDateTime.now().withNano(0).toString()
        val response = api.createInventory(InventoryRequest(productId, qty, now, expiry))
        response.data?.id?.let { ExpiryNotification.schedule(application, it, "Produk #$productId", expiry) }
        loadAll()
        _state.value = _state.value.copy(info = "Stok ditambahkan")
    }

    fun consume(productId: Int, qty: Double) = run {
        api.consume(productId, qty)
        loadAll()
        _state.value = _state.value.copy(info = "Stok dipakai ${trim(qty)}")
    }

    fun deleteProduct(id: Int) = run {
        api.deleteProduct(id)
        loadAll()
        _state.value = _state.value.copy(info = "Produk dihapus")
    }

    fun addCategory(name: String, description: String) = run {
        api.createCategory(CategoryRequest(name, description.ifBlank { null }))
        loadAll()
        _state.value = _state.value.copy(info = "Kategori ditambahkan")
    }

    fun updateCategory(id: Int, name: String, description: String) = run {
        api.updateCategory(id, CategoryRequest(name, description.ifBlank { null }))
        loadAll()
        _state.value = _state.value.copy(info = "Kategori diperbarui")
    }

    fun deleteCategory(id: Int) = run {
        api.deleteCategory(id)
        loadAll()
        _state.value = _state.value.copy(info = "Kategori dihapus")
    }

    fun updateProfile(name: String, password: String?) = run {
        val id = _state.value.user?.id ?: error("Sesi tidak ditemukan")
        val updated = api.updateUser(id, UserUpdate(name, null, password)).data
        _state.value = _state.value.copy(
            user = updated ?: _state.value.user?.copy(name = name),
            info = "Profil diperbarui"
        )
    }

    fun deleteAccount() = run {
        val id = _state.value.user?.id ?: error("Sesi tidak ditemukan")
        api.deleteUser(id)
        SessionStore.accessToken = null
        SessionStore.refreshToken = null
        _state.value = UiState(info = "Akun berhasil dihapus")
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, info = null)
    }

    private fun trim(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
