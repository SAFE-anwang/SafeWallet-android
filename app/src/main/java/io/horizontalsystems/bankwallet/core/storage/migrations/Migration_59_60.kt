package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_59_60 : Migration(59, 60) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `ProposalState` (`proposalId` INTEGER NOT NULL, `address` TEXT NOT NULL, `state` INTEGER NOT NULL, PRIMARY KEY(`address`, `proposalId`))")
    }
}
