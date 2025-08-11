package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_65_66 : Migration(65, 66) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `CustomToken` (`symbol` TEXT NOT NULL, " +
                "`creator` TEXT NOT NULL, `type` INTEGER NOT NULL, `address` TEXT NOT NULL, " +
                "`decimals` TEXT NOT NULL, `name` TEXT NOT NULL, `version` TEXT, " +
                "`chainId` TEXT NOT NULL, `logoURI` TEXT NOT NULL, PRIMARY KEY(`address`))")
    }
}
