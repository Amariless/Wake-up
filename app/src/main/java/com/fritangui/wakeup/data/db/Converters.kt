package com.fritangui.wakeup.data.db

import androidx.room.TypeConverter
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class Converters {

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? =
        value?.let { json.decodeFromString<List<Long>>(it) }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.let { json.decodeFromString<List<String>>(it) }

    @TypeConverter
    fun fromDismissChallenge(value: DismissChallengeType): String = value.name

    @TypeConverter
    fun toDismissChallenge(value: String): DismissChallengeType =
        runCatching { DismissChallengeType.valueOf(value) }.getOrDefault(DismissChallengeType.NONE)

    @TypeConverter
    fun fromBlockSurface(value: BlockSurface): String = value.name

    @TypeConverter
    fun toBlockSurface(value: String): BlockSurface =
        runCatching { BlockSurface.valueOf(value) }.getOrDefault(BlockSurface.GENERIC_APP_TIME_LIMIT)
}
