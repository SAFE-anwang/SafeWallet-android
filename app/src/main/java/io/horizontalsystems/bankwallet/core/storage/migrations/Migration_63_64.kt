package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_63_64 : Migration(63, 64) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE AccountRecord ADD `isAnBaoWallet` INTEGER NOT NULL DEFAULT 0")
    }
}
