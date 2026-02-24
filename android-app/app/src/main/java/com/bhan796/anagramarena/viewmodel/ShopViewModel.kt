package com.bhan796.anagramarena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bhan796.anagramarena.online.CosmeticItem
import com.bhan796.anagramarena.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopUiState(
    val runes: Int = 0,
    val pendingChests: Int = 0,
    val inventory: List<CosmeticItem> = emptyList(),
    val equippedItemId: String? = null,
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val chestResult: CosmeticItem? = null,
    val error: String? = null
)

class ShopViewModel(private val shopRepo: ShopRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShopUiState())
    val state = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = shopRepo.fetchInventory()
            _state.update { current ->
                result.getOrNull()?.let { inventory ->
                    current.copy(
                        runes = inventory.runes,
                        pendingChests = inventory.pendingChests,
                        inventory = inventory.items,
                        equippedItemId = inventory.equippedCosmetic,
                        isLoading = false
                    )
                } ?: current.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun purchaseChest() {
        viewModelScope.launch {
            _state.update { it.copy(isPurchasing = true, error = null) }
            val result = shopRepo.purchaseChest()
            _state.update { current ->
                result.getOrNull()?.let {
                    current.copy(
                        runes = it.remainingRunes,
                        pendingChests = it.pendingChests,
                        isPurchasing = false
                    )
                } ?: current.copy(isPurchasing = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun openChest() {
        viewModelScope.launch {
            val result = shopRepo.openChest()
            _state.update { current ->
                result.getOrNull()?.let {
                    current.copy(
                        chestResult = it,
                        pendingChests = (current.pendingChests - 1).coerceAtLeast(0),
                        inventory = if (current.inventory.any { item -> item.id == it.id }) current.inventory else current.inventory + it
                    )
                } ?: current.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun equipItem(itemId: String?) {
        viewModelScope.launch {
            val result = shopRepo.equipItem(itemId)
            if (result.isSuccess) {
                _state.update { it.copy(equippedItemId = itemId) }
            } else {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun clearChestResult() {
        _state.update { it.copy(chestResult = null) }
    }

    companion object {
        fun factory(shopRepo: ShopRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShopViewModel(shopRepo) as T
                }
            }
        }
    }
}