package com.bhan796.anagramarena.avatars

import com.bhan796.anagramarena.online.AvatarDefinition

object AvatarCatalog {
    val avatars: List<AvatarDefinition> = listOf(DefaultRookieAvatar)

    fun getById(id: String): AvatarDefinition {
        return avatars.firstOrNull { it.id == id } ?: DefaultRookieAvatar
    }
}
