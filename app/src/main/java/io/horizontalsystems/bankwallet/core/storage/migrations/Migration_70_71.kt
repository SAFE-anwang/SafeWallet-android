package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_70_71 : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `StatRecord` (`json` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")

        db.execSQL("ALTER TABLE EnabledWallet ADD `coinImage` TEXT")

        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamAddress` (`transactionHash` BLOB NOT NULL, `address` TEXT NOT NULL, `domain` TEXT, `blockchainType` TEXT, PRIMARY KEY(`transactionHash`, `address`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamScanState` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `lastTransactionHash` BLOB NOT NULL, PRIMARY KEY(`blockchainType`, `accountId`))")

        db.execSQL("CREATE TABLE IF NOT EXISTS `RecentAddress` (`accountId` TEXT NOT NULL, `blockchainType` TEXT NOT NULL, `address` TEXT NOT NULL, PRIMARY KEY(`accountId`, `blockchainType`))")

        //clean binancecoin coins and tokens from wallet
        db.execSQL("DELETE FROM EnabledWallet WHERE tokenQueryId LIKE '%binancecoin%'")
        db.execSQL("DELETE FROM EnabledWalletCache WHERE tokenQueryId LIKE '%binancecoin%'")

        db.execSQL("DROP TABLE CexAssetRaw")

        // recreate SpamScanState
        db.execSQL("DROP TABLE IF EXISTS `SpamScanState`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamScanState` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `lastSyncedTransactionId` TEXT NOT NULL, PRIMARY KEY(`blockchainType`, `accountId`))")

        db.execSQL("CREATE TABLE IF NOT EXISTS `MoneroNodeRecord` (`url` TEXT NOT NULL, `username` TEXT, `password` TEXT, PRIMARY KEY(`url`))")

        db.execSQL("UPDATE `RestoreSettingRecord` SET value = '3480000' WHERE blockchainTypeUid='monero' AND key='BirthdayHeight' AND value='-1'")

        try {
            db.execSQL(
                " INSERT INTO `RestoreSettingRecord`(`accountId`, `blockchainTypeUid`, `key`, `value`) " +
                        "SELECT `id`,  'monero', 'BirthdayHeight', '3480000' FROM AccountRecord WHERE type='mnemonic' AND origin='Created'"
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        db.execSQL("ALTER TABLE MoneroNodeRecord ADD COLUMN `trusted` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("""
            CREATE TABLE `EnabledWalletCache_new` (
                `tokenQueryId` TEXT NOT NULL, 
                `accountId` TEXT NOT NULL, 
                `balanceData` TEXT, 
                PRIMARY KEY(`tokenQueryId`, `accountId`),
                FOREIGN KEY(`accountId`) 
                REFERENCES `AccountRecord`(`id`) 
                ON UPDATE CASCADE 
                ON DELETE CASCADE
                DEFERRABLE INITIALLY DEFERRED -- Add this line
            )
        """)
        db.execSQL("DROP TABLE `EnabledWalletCache`")
        db.execSQL("ALTER TABLE `EnabledWalletCache_new` RENAME TO `EnabledWalletCache`")


        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `SwapProviderAssetRecord` (
                `providerId` TEXT NOT NULL,
                `tokenQueryId` TEXT NOT NULL,
                `data` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`providerId`, `tokenQueryId`)
            )
        """.trimIndent())

    }
}
