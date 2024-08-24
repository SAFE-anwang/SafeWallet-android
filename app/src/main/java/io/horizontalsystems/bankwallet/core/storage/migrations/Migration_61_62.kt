package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_61_62 : Migration(61, 62) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("Update `AccountRecord` SET isBackedUp = '1' WHERE type = 'evm_private_key'")
    }
}
