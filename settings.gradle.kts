pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SafeWallet"

include(":app")

include(":core")
include(":ui")
include(":components:icons")
include(":components:chartview")
include(":subscriptions-core")
if (file("subscriptions-google-play").exists()) {
    include(":subscriptions-google-play")
}
include(":subscriptions-dev")
include(":subscriptions-fdroid")

include(":components:views")
//include ':components:snackbar'
include(":components:seekbar")
include(":components:chartview")

includeFlat("bitcoin-kit-android")
include(":bitcoin-kit-android:bitcoincore")     // Bitcoin Core
include(":bitcoin-kit-android:bitcoinkit")      // Bitcoin kit
include(":bitcoin-kit-android:dashkit")         // Dash kit
include(":bitcoin-kit-android:bitcoincashkit")  // Bitcoin Cash kit
include(":bitcoin-kit-android:litecoinkit")     // Litecoin kit
include(":bitcoin-kit-android:dogecoinkit")     // Litecoin kit
include(":bitcoin-kit-android:hodler")          // Hodler
include(":bitcoin-kit-android:tools")           // Checkpoint syncer
include(":bitcoin-kit-android:safekit")
include(":bitcoin-kit-android:ecashkit")

includeFlat("market-kit-android")
include(":market-kit-android:marketkit")

includeFlat("ethereum-kit-android")
include(":ethereum-kit-android:ethereumkit")
include(":ethereum-kit-android:erc20kit")
include(":ethereum-kit-android:uniswapkit")
include(":ethereum-kit-android:oneinchkit")
include(":ethereum-kit-android:nftkit")
include(":ethereum-kit-android:wsafekit")

includeFlat("solana-kit-android")
include(":solana-kit-android:solanakit")

include(":vpn")

//include("telegram:TMessagesProj")
