package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_39_40 : Migration(39, 40) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `VpnServerInfo` (`address` TEXT NOT NULL, `port` INTEGER NOT NULL DEFAULT 0, `clientId` TEXT NOT NULL,`alterId` TEXT NOT NULL, `protocol` TEXT NOT NULL, `camouflageType` TEXT NOT NULL, `type` TEXT NOT NULL, `securitys` TEXT NOT NULL, PRIMARY KEY(`address`))")
    }
}
