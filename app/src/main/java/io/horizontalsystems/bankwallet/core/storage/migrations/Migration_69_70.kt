package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_69_70 : Migration(69, 70) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `SRC20LockedInfo` (`id` TEXT NOT NULL, " +
                    "`amount` TEXT NOT NULL, `address` TEXT NOT NULL, `lockDay` INTEGER NOT NULL, " +
                    "`startHeight` INTEGER NOT NULL, `unlockHeight` INTEGER NOT NULL, " +
                    "`contract` TEXT NOT NULL,  PRIMARY KEY(`id`, `contract`))"
        )

    }
}
