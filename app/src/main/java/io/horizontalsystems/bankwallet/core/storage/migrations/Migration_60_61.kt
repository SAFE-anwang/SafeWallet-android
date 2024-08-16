package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_60_61 : Migration(60, 61) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `Redeem` (`address` TEXT NOT NULL, `existAvailable` INTEGER NOT NULL, `existLocked` INTEGER NOT NULL, `existMasterNode` INTEGER NOT NULL, `success` INTEGER NOT NULL, PRIMARY KEY(`address`))")
    }
}
