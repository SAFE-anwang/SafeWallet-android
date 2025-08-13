package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_66_67 : Migration(66, 67) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE CustomToken RENAME TO TempCustomToken")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `CustomToken` (`symbol` TEXT NOT NULL, " +
                    "`creator` TEXT NOT NULL, `type` INTEGER NOT NULL, `address` TEXT NOT NULL, " +
                    "`decimals` TEXT NOT NULL, `name` TEXT NOT NULL, `version` TEXT, " +
                    "`chainId` TEXT NOT NULL, `logoURI` TEXT NOT NULL, PRIMARY KEY(`address`))"
        )
        try {
            database.execSQL("INSERT INTO CustomToken (`symbol`, `creator`, `type`, `address`, `decimals`, `name`, `version`, `chainId`, `logoURI`) SELECT `symbol`, `creator`, `type`, `address`, `decimals`, `name`, `version`, `chainId`, `logoURI` FROM TempCustomToken")
        } catch (e: Exception) {

        }
        database.execSQL("DROP TABLE TempCustomToken")
    }
}
