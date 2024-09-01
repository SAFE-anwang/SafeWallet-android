package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_62_63 : Migration(62, 63) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("Update `AccountRecord` SET isBackedUp = '1', isFileBackedUp = '1' WHERE type = 'evm_private_key'")
    }
}
