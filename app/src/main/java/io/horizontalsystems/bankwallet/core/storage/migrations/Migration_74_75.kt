package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_74_75 : Migration(74, 75) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `LockRecordInfo` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `SRC20LockedInfo` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ProposalRecordInfo` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ProposalState` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `Redeem` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `NodeInfo` ADD COLUMN `chainType` INTEGER NOT NULL DEFAULT 0")
    }
}
