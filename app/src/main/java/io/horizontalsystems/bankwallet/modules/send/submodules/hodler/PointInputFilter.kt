package io.horizontalsystems.bankwallet.modules.send.submodules.hodler

import android.text.InputFilter
import android.text.Spanned

class PointInputFilter: InputFilter {

    private val DECIMAL_DIGITS = 8

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        if ("" == source.toString()) {
            return null
        }
        val value = dest.toString()
        val splitArray = value.split("\\.").toTypedArray()
        if (splitArray.size > 1) {
            val dotValue = splitArray[1]
            val diff = dotValue.length + 1 - DECIMAL_DIGITS
            if (diff > 0) {
                return source!!.subSequence(start, end - diff)
            }
        }
        return null
    }

}