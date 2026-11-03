package com.olius.app.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun load(): UserProfileEntity?

    @Query("DELETE FROM user_profile")
    fun clear()

    @Upsert
    fun upsert(profile: UserProfileEntity)

    /** Uma conta por vez: troca a linha inteira. */
    @Transaction
    fun replace(profile: UserProfileEntity) {
        clear()
        upsert(profile)
    }
}
