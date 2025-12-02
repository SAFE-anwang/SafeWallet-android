package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import java.math.BigInteger

class BigIntegerConverters {

    @TypeConverter
    fun fromBigInteger(value: BigInteger?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toBigInteger(value: String?): BigInteger? {
        return value?.let { BigInteger(it) }
    }
}