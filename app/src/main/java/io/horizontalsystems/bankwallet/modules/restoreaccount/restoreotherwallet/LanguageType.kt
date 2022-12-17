package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreotherwallet

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import kotlinx.android.parcel.Parcelize

sealed class LanguageType: Parcelable {

    @Parcelize
    object English : LanguageType()

    @Parcelize
    object Chinese : LanguageType()

    @Parcelize
    object TraditionalChinese : LanguageType()

    @Parcelize
    object Japan : LanguageType()

    @Parcelize
    object Spanish : LanguageType()

    @Parcelize
    object Korean : LanguageType()

    @Parcelize
    object French : LanguageType()

    @Parcelize
    object Italian : LanguageType()

    val type : String
        get() = when(this) {
            is English -> "en"
            is Chinese -> "zh-cn"
            is TraditionalChinese -> "zh-tw"
            is Japan -> "ja"
            is Spanish -> "es"
            is Korean -> "ko"
            is French -> "fr"
            is Italian -> "it"
        }

    val showName : Int
        get() = when(this) {
            is English -> R.string.Restore_Import_Launage_EN
            is Chinese -> R.string.Restore_Import_Launage_ZH
            is TraditionalChinese -> R.string.Restore_Import_Launage_TW
            is Japan -> R.string.Restore_Import_Launage_JP
            is Spanish -> R.string.Restore_Import_Launage_SP
            is Korean -> R.string.Restore_Import_Launage_KO
            is French -> R.string.Restore_Import_Launage_FR
            is Italian -> R.string.Restore_Import_Launage_IT
        }

}