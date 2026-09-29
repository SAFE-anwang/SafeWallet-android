package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * NodeInfo 表主键由 id 改为 (id, type, chainType) 复合主键。
 *
 * 原因：超级节点与主节点的 id 都由链上从 1 开始各自编号，仅用 id 作主键时
 * 两者会互相覆盖（REPLACE），导致缓存数据缺失。
 *
 * NodeInfo 是纯本地缓存（可由 [SuperNodeCacheManager] 重新拉取），
 * 因此这里直接删除旧表重建，不迁移旧数据。
 */
object Migration_81_82 : Migration(81, 82) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `NodeInfo`")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `NodeInfo` (
                `id` INTEGER NOT NULL,
                `addr` TEXT NOT NULL,
                `creator` TEXT NOT NULL,
                `enode` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `isOfficial` INTEGER NOT NULL,
                `state` TEXT NOT NULL,
                `founders` TEXT NOT NULL,
                `incentivePlan` TEXT NOT NULL,
                `lastRewardHeight` INTEGER NOT NULL,
                `createHeight` INTEGER NOT NULL,
                `updateHeight` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `isEdit` INTEGER NOT NULL,
                `totalVoteNum` TEXT NOT NULL,
                `totalAmount` TEXT NOT NULL,
                `allVoteNum` TEXT NOT NULL,
                `availableLimit` TEXT NOT NULL,
                `type` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `chainType` INTEGER NOT NULL,
                PRIMARY KEY(`id`, `type`, `chainType`)
            )
        """.trimIndent())
    }
}
