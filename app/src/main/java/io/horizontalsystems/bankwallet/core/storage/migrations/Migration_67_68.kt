package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_67_68 : Migration(67, 68) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `LockRecordInfo` (`id` INTEGER NOT NULL, " +
                    "`value` TEXT NOT NULL, `address` TEXT, `address2` TEXT, " +
                    "`unlockHeight` INTEGER, `releaseHeight` INTEGER, " +
                    "`contact` TEXT NOT NULL, `creator` TEXT NOT NULL, `type` INTEGER NOT NULL,  `withEnable` INTEGER NOT NULL,  PRIMARY KEY(`id`, `contact`))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `ProposalRecordInfo` (`id` INTEGER NOT NULL, " +
                    "`creator` TEXT NOT NULL, `title` TEXT NOT NULL, `payAmount` TEXT NOT NULL, " +
                    "`payTimes` INTEGER NOT NULL, `startPayTime` INTEGER NOT NULL, `endPayTime` INTEGER NOT NULL, " +
                    "`description` TEXT NOT NULL, `state` INTEGER NOT NULL, `createHeight` INTEGER, `updateHeight` INTEGER,  `newProposal` INTEGER NOT NULL,  PRIMARY KEY(`id`))"
        )


    }
}
