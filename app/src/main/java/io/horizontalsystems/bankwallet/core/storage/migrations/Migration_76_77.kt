package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_76_77 : Migration(76, 77) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `SwapRecord_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL,
                `providerId` TEXT NOT NULL,
                `providerName` TEXT NOT NULL,
                `tokenInUid` TEXT NOT NULL,
                `tokenInCoinCode` TEXT NOT NULL,
                `tokenInCoinUid` TEXT NOT NULL,
                `tokenInBadge` TEXT,
                `tokenInBlockchainTypeUid` TEXT NOT NULL,
                `tokenOutUid` TEXT NOT NULL,
                `tokenOutCoinCode` TEXT NOT NULL,
                `tokenOutCoinUid` TEXT NOT NULL,
                `tokenOutBadge` TEXT,
                `tokenOutBlockchainTypeUid` TEXT NOT NULL,
                `amountIn` TEXT NOT NULL,
                `amountOut` TEXT,
                `amountOutMin` TEXT,
                `recipientAddress` TEXT,
                `sourceAddress` TEXT,
                `transactionHash` TEXT,
                `outboundTransactionHash` TEXT,
                `providerSwapId` TEXT,
                `fromAsset` TEXT,
                `toAsset` TEXT,
                `depositAddress` TEXT,
                `status` TEXT NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `SwapRecord_new` (
                `id`, `accountId`, `timestamp`, `providerId`, `providerName`,
                `tokenInUid`, `tokenInCoinCode`, `tokenInCoinUid`, `tokenInBadge`, `tokenInBlockchainTypeUid`,
                `tokenOutUid`, `tokenOutCoinCode`, `tokenOutCoinUid`, `tokenOutBadge`, `tokenOutBlockchainTypeUid`,
                `amountIn`, `amountOut`, `amountOutMin`,
                `recipientAddress`, `sourceAddress`, `transactionHash`, `outboundTransactionHash`,
                `providerSwapId`, `fromAsset`, `toAsset`, `depositAddress`, `status`
            )
            SELECT
                `id`, `accountId`, `timestamp`, `providerId`, `providerName`,
                `tokenInUid`, `tokenInCoinCode`, `tokenInCoinUid`, `tokenInBadge`, `tokenInBlockchainTypeUid`,
                `tokenOutUid`, `tokenOutCoinCode`, `tokenOutCoinUid`, `tokenOutBadge`, `tokenOutBlockchainTypeUid`,
                `amountIn`, `amountOut`, `amountOutMin`,
                NULL, `sourceAddress`, `transactionHash`, `outboundTransactionHash`,
                `providerSwapId`, `fromAsset`, `toAsset`, `depositAddress`, `status`
            FROM `SwapRecord`
        """.trimIndent()
        )

        db.execSQL("DROP TABLE `SwapRecord`")
        db.execSQL("ALTER TABLE `SwapRecord_new` RENAME TO `SwapRecord`")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `SwapProviderChainRecord` (
                `providerId` TEXT NOT NULL,
                `chainId` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`providerId`, `chainId`)
            )
        """.trimIndent())
    }
}
