package edu.vt.cs.cs5254.dreamcatcher.database

import androidx.room.TypeConverter
import edu.vt.cs.cs5254.dreamcatcher.DreamEntryKind
import java.util.*

class DreamTypeConverters {
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }
    @TypeConverter
    fun toUUID(uuid: String?): UUID? {
        return UUID.fromString(uuid)
    }
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }
    @TypeConverter
    fun toDreamEntryKind(dreamEntryKind: String): DreamEntryKind? {
        return enumValueOf<DreamEntryKind>(dreamEntryKind)
    }
    @TypeConverter
    fun fromDreamEntryKind(dreamEntryKind: DreamEntryKind): String? {
        return dreamEntryKind.name
    }
}