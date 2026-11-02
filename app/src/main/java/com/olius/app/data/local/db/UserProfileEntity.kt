package com.olius.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.olius.app.domain.entity.ProfileType
import com.olius.app.domain.entity.UserProfile

/**
 * Tabela `user_profile` do SQLite do app: o último GET /users/me da conta
 * logada (uma linha só). É cache: a Core continua sendo a dona dos dados.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    /** [ProfileType.name] (ESTABLISHMENT ou CITIZEN). */
    val profileType: String,
    val phone: String?,
    val isCollector: Boolean,
    val qrToken: String?
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name,
        email = email,
        profileType = runCatching { ProfileType.valueOf(profileType) }.getOrDefault(ProfileType.ESTABLISHMENT),
        phone = phone,
        isCollector = isCollector,
        qrToken = qrToken
    )

    companion object {
        fun from(profile: UserProfile) = UserProfileEntity(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            profileType = profile.profileType.name,
            phone = profile.phone,
            isCollector = profile.isCollector,
            qrToken = profile.qrToken
        )
    }
}
