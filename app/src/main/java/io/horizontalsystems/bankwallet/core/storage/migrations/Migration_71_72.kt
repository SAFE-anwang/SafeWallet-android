package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_71_72 : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `NodeInfo` (`id` INTEGER PRIMARY KEY NOT NULL, `addr` TEXT NOT NULL, " +
                "`creator` TEXT NOT NULL, `enode` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                "`isOfficial` INTEGER NOT NULL, `state` TEXT NOT NULL, `founders` TEXT NOT NULL, " +
                "`incentivePlan` TEXT NOT NULL, `lastRewardHeight` INTEGER NOT NULL, `createHeight` INTEGER NOT NULL, " +
                "`updateHeight` INTEGER NOT NULL, `name` TEXT NOT NULL DEFAULT '', `isEdit` INTEGER NOT NULL DEFAULT 0, " +
                "`totalVoteNum` TEXT NOT NULL DEFAULT '0', `totalAmount` TEXT NOT NULL DEFAULT '0', " +
                "`allVoteNum` TEXT NOT NULL DEFAULT '0', `availableLimit` TEXT NOT NULL DEFAULT '0', `type` INTEGER NOT NULL DEFAULT 0)")

    }
}
