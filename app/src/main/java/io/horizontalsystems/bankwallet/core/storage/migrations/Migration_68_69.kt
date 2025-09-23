package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_68_69 : Migration(68, 69) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM LockRecordInfo")
        database.execSQL("ALTER TABLE LockRecordInfo ADD `frozenAddr` TEXT")
    }
}
