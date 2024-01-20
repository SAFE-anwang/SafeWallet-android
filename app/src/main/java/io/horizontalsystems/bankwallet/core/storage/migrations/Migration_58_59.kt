package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_58_59 : Migration(58, 59) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE `ActiveAccount_new` (`level` INTEGER NOT NULL, `accountId` TEXT NOT NULL, PRIMARY KEY(`level`))")
        database.execSQL("INSERT INTO ActiveAccount_new (`accountId`, `level`) SELECT `accountId`, 0 FROM ActiveAccount LIMIT 0, 1")
        database.execSQL("DROP TABLE ActiveAccount")
        database.execSQL("ALTER TABLE ActiveAccount_new RENAME TO ActiveAccount")
    }
}
