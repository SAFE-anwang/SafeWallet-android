package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_72_73 : Migration(72, 73) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop old SpamAddress table
        db.execSQL("DROP TABLE IF EXISTS `SpamAddress`")

        // Create new ScannedTransaction table with spamScore
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ScannedTransaction` (
                `transactionHash` BLOB NOT NULL,
                `spamScore` INTEGER NOT NULL,
                `blockchainType` TEXT NOT NULL,
                `address` TEXT COLLATE NOCASE,
                PRIMARY KEY(`transactionHash`)
            )
        """.trimIndent())

        // Add index on address and spamScore for faster spam lookups
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ScannedTransaction_address_spamScore` ON `ScannedTransaction` (`address`, `spamScore`)")

        // Clear SpamScanState to trigger rescan of all transactions
        db.execSQL("DELETE FROM SpamScanState")
    }
}
