package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_64_65 : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE EnabledWallet ADD `coinImage` TEXT")
        db.execSQL("CREATE TABLE IF NOT EXISTS `StatRecord` (`json` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
    }
}
