package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App

object Migration_57_58 : Migration(57, 58) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `EnabledWallet` SET tokenQueryId = 'polygon-pos|eip20:0xb7dd19490951339fe65e341df6ec5f7f93ff2779' WHERE tokenQueryId = 'polygon-pos|eip20:0xb7Dd19490951339fE65E341Df6eC5f7f93FF2779'")
    }
}
