plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}


android {
    namespace = "io.horizontalsystems.bankwallet"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.anwang.safewallet"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 170
        versionName = "0.49.0.20260810"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("de", "es", "en", "fa", "fr", "ko", "pt", "pt-rBR", "ru", "tr", "zh")

        vectorDrawables.useSupportLibrary = true

        resValue("string", "companyWebPageLink", "https://www.anwang.com")
        resValue("string", "appWebPageLink", "https://www.anwang.com")
        resValue("string", "analyticsLink", "https://unstoppable.money/analytics")
        resValue("string", "appGithubLink", "https://github.com/SAFE-anwang/SafeWallet-android")
        resValue("string", "appTwitterLink", "https://twitter.com/safeanwang")
        resValue("string", "appTelegramLink", "tg://resolve?domain=safeanwang4")
        resValue("string", "appRedditLink", "https://www.reddit.com/user/safe_2018")
        resValue("string", "reportEmail", "foundation@anwang.com")
        resValue("string", "releaseNotesUrl", "https://api.github.com/repos/SAFE-anwang/SafeWallet-android/releases/tags/")
        resValue("string", "walletConnectAppMetaDataName", "Safe Wallet")
        resValue("string", "walletConnectAppMetaDataUrl", "example.wallet")
        resValue("string", "walletConnectAppMetaDataIcon", "https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media")
        resValue("string", "walletConnectV1PeerMetaName", "SafeWallet")
        resValue("string", "walletConnectV1PeerMetaUrl", "https://unstoppable.money")
        resValue("string", "accountsBackupFileSalt", "Safe Wallet")

        buildConfigField("boolean", "FDROID_BUILD", "false")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        create("test") {
            storeFile = file("./test.keystore")
            storePassword = "testKeystore123"
            keyAlias = "testKeystore"
            keyPassword = "testKeystore123"
        }
    }

    flavorDimensions += "distribution"

    val uswapApiKeyAndroid = "a32d6d05ef80c878c49eb7692aa6e2b36c4c0c7777b89e2c3c4d8e512a7cea61"
    val uswapApiKeyFdroid = "6e928d1db31e481ae57d42f34a9b7d58a64d7e2380f7ea696e652bd9a0ee516e"
    val oneInchFeeAddressAndroid = "0xe42BBeE8389548fAe35C09072065b7fEc582b590"
    val oneInchFeeAddressFdroid = "0x8009267B9929196f74720F2f1496bbD7B79945F1"

    productFlavors {
        create("base") {
            dimension = "distribution"
            resValue("string", "uswapApiKey", uswapApiKeyAndroid)
            resValue("string", "oneInchPartnerFeeAddress", oneInchFeeAddressAndroid)
        }

        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "FDROID_BUILD", "true")
            resValue("string", "uswapApiKey", uswapApiKeyFdroid)
            resValue("string", "oneInchPartnerFeeAddress", oneInchFeeAddressFdroid)
        }

        create("fdroidCi") {
            dimension = "distribution"
            applicationIdSuffix = ".fdroidci"
            buildConfigField("boolean", "FDROID_BUILD", "true")
            signingConfig = signingConfigs.getByName("test")
            resValue("string", "uswapApiKey", uswapApiKeyFdroid)
            resValue("string", "oneInchPartnerFeeAddress", oneInchFeeAddressFdroid)
        }

        create("ci") {
            dimension = "distribution"
            applicationIdSuffix = ".appcenter"
            versionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: defaultConfig.versionCode
            signingConfig = signingConfigs.getByName("test")
            resValue("string", "appLinksHost", "dev.unstoppable.money")
            resValue("string", "uswapApiKey", uswapApiKeyAndroid)
            resValue("string", "oneInchPartnerFeeAddress", oneInchFeeAddressAndroid)
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("test")
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"
            resValue("string", "appLinksHost", "dev.unstoppable.money")
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAP78uAEAAAAA4CGDcoRHAaZvoc3B4Yib%2FdTYobc%3Ds9JR94JpMz4GNjucPrqteBiQ0IJAajm41locTKcX3GW5zZuzS2")
            resValue("string", "uniswapGraphUrl", "https://api.thegraph.com/subgraphs/name/uniswap/uniswap-v2")
            resValue("string", "infuraProjectId", "fc344bd5cc7e4bd3bb356a68a831d1ea")
            resValue("string", "infuraSecretKey", "g2a75Cb5qI8NKXSonkHjv1sjj2QGL9l8iMu0iGcw06+/hEKGeXPydg")
            resValue("string", "infuraProjectId3", "99e0b89d9f3d45c98cc95c0a47aeb837")
            resValue("string", "infuraSecretKey3", "f66154c66d024b5e80a7165d09d5fb6d")
            resValue("string", "etherscanKey", "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE")
            resValue("string", "etherscanKey1", "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE")
            resValue("string", "bscscanKey", "C4VUBXUJW871MFBD7EPD66VEZRGGPUTPM8")
            resValue("string", "bscscanKey2", "3YH525P1PENTH5TDZP6T5R55Q2VPC5HV1B")
            resValue("string", "bscscanKey3", "BTTGJQ8RJ139GA116II2DRRGQN42EWMY8J")
            resValue("string", "bscscanKey4", "GBCT792EPY99BR7SDBQBTNPSHCY1TGH223")
            resValue("string", "bscscanKey5", "3AP5558QS44WAJBVPGG785R4G7VZW6616Z")
            resValue("string", "bscscanKey6", "RG1CIITEEAT58J7UIAJVV3KSA2I6RRQDD2")
            resValue("string", "otherScanKey", "FU7CYEXQEUSMXJJF8MZR6BNRMP9XT8S9CP")
            resValue("string", "polygonscanKey", "TNQ44BCF1MS3S75Y6A2B6SN88I8UYFJFRM")
            resValue("string", "polygonscanKey2", "TNQ44BCF1MS3S75Y6A2B6SN88I8UYFJFRM")
            resValue("string", "polygonscanKey3", "TNQ44BCF1MS3S75Y6A2B6SN88I8UYFJFRM")
            resValue("string", "polygonscanKey4", "TNQ44BCF1MS3S75Y6A2B6SN88I8UYFJFRM")
            resValue("string", "snowtraceApiKey", "DD8VX77TQ73KSNDFGBQQ31J7K5B51CXXPH")
            resValue("string", "optimisticEtherscanApiKey", "A4E6DAX46FFFW4CGZP6IMZ64ADI3TIRBTS")
            resValue("string", "arbiscanApiKey", "Z43JN5434XVNA5D73UGPWKF26G5D9MGDPZ")
            resValue("string", "gnosisscanApiKey", "V2J8YU15ZX9S1W3GTUV2HXM11TP2TUBRW4")
            resValue("string", "ftmscanApiKey", "57YQ2GIRAZNV6M5HIJYYG3XQGGNIPVV8MF")
            resValue("string", "is_release", "false")
            resValue("string", "guidesUrl", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/develop/index.json")
            resValue("string", "eduUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            resValue("string", "faqUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/master/src/faq.json")
            resValue("string", "coinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/coins.json")
            resValue("string", "providerCoinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/master/provider.coins.json")
            resValue("string", "marketApiBaseUrl", "https://api-dev.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
            resValue("string", "openSeaApiKey", "bfbd6061a33e455c8581b594774fecb3")
            resValue("string", "walletConnectV2Key", "d98d5186f513a943fbbff4a755957fbf")
            resValue("string", "walletConnectV2Key1", "45b825481aab1907c73ff9499f199070")
            resValue("string", "solscanApiKey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVkQXQiOjE3Mzc0NDMwMjMzMTIsImVtYWlsIjoiMTgxMzgyNDgxNjlAMTYzLmNvbSIsImFjdGlvbiI6InRva2VuLWFwaSIsImFwaVZlcnNpb24iOiJ2MiIsImlhdCI6MTczNzQ0MzAyM30.FyLCMRfZJ0AiYzXFFPTmbUtYdu29u7PPCPV-txXbToM")
            resValue("string", "solanaAlchemyApiKey", "PKgWxOMarrHgyMESGjIkJ,BOlzgqJUeGYe5E7K613Fm")
            resValue("string", "solanaJupiterApiKey", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            resValue("string", "trongridApiKeys", "8f5ae2c8-8012-42a8-b0ca-ffc2741f6a29,578aa64f-a79f-4ee8-86e9-e9860e2d050a,1e92f1fc-41f8-401f-a7f6-5b719b6f1280,d1511874-1547-48df-9536-a32cc85949ac,cec10562-8f2f-44a8-a0f6-245842bc6758,33374494-8060-447e-8367-90c5efd4ed95")
            resValue("string", "udnApiKey", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            resValue("string", "blocksDecodedEthereumRpc", "https://ethereum-mainnet.core.chainstack.com/a1911ee247f4f8de22c1f4e55865f616")
            resValue("string", "oneInchApiKey", "ElyK7s22HR0JD78CEXVPnpZA8UyuUwIl")
            resValue("string", "oneInchApiKey2", "eJ6FPSDW5z2vKD6oEcjQwnzFzvQ6nXgZ")
            resValue("string", "oneInchApiKey3", "6oMuaUZbbjSSzIvHJB4YodfvQItpBrlY")
            resValue("string", "safeswapv2safe4factory", "0xB3c827077312163c53E3822defE32cAffE574B42")
            resValue("string", "safeswapv2safe4codehash", "ad0e51aa7a058efb9eb40fd6385473f0175ee7419e8d4f91a4e0294ec12b2d13")
            resValue("string", "safeswapv2safe4router", "0x6476008C612dF9F8Db166844fFE39D24aEa12271")
            resValue("string", "chainalysisBaseUrl", "https://public.chainalysis.com/api/v1/")
            resValue("string", "chainalysisApiKey", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            resValue("string", "hashDitBaseUrl", "https://service.hashdit.io/v2/hashdit/")
            resValue("string", "hashDitApiKey", "aGMkgODYiUFtTYrSRcEZsIfPHeASOlGYXClJZNWF")
            resValue("string", "uswapApiBaseUrl", "https://swap-dev.unstoppable.money/api/v2/")
        }


        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "appLinksHost", "unstoppable.money")
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB")
            resValue("string", "etherscanKey", "IEXTB9RE7MUV2UQ9X238RP146IEJB1J5HS,27S4V3GYJGMCPWQZ2T4SF9355QBQYQ3FI7,YK4KEA3TANM8KZ5J6E2Q1ZIM6YDM8TEABM,FU7CYEXQEUSMXJJF8MZR6BNRMP9XT8S9CP")
            resValue("string", "bscscanKey", "FQ2HSNNEHVG71U96P1TF3WF9RTF6AF5MRA,G6K8VZDWYSJHTCRURRITFZ2ZWV48GRGTZQ,R396MSJNCKX2YK4EIMP3EWYAW21NSVMXRN,8QW2JNMPHPUPAACFGXZ3A5PVQY6PBCJPEG")
            resValue("string", "twitterBearerToken", "AAAAAAAAAAAAAAAAAAAAAP78uAEAAAAA4CGDcoRHAaZvoc3B4Yib%2FdTYobc%3Ds9JR94JpMz4GNjucPrqteBiQ0IJAajm41locTKcX3GW5zZuzS2")
            resValue("string", "uniswapGraphUrl", "https://api.thegraph.com/subgraphs/name/uniswap/uniswap-v2")
            resValue("string", "infuraProjectId", "fc344bd5cc7e4bd3bb356a68a831d1ea")
            resValue("string", "infuraSecretKey", "g2a75Cb5qI8NKXSonkHjv1sjj2QGL9l8iMu0iGcw06+/hEKGeXPydg")
            resValue("string", "infuraProjectId3", "99e0b89d9f3d45c98cc95c0a47aeb837")
            resValue("string", "infuraSecretKey3", "f66154c66d024b5e80a7165d09d5fb6d")
            resValue("string", "etherscanKey", "FJBFQ5BF7BUQZNQBH4ZZFQ9D98UJI6Y7K1")
            resValue("string", "etherscanKey1", "GDMGEBI3CUW4GSAQ5CQU127VBRUQXW6YYV")
            resValue("string", "bscscanKey", "C4VUBXUJW871MFBD7EPD66VEZRGGPUTPM8")
            resValue("string", "bscscanKey2", "3YH525P1PENTH5TDZP6T5R55Q2VPC5HV1B")
            resValue("string", "bscscanKey3", "BTTGJQ8RJ139GA116II2DRRGQN42EWMY8J")
            resValue("string", "bscscanKey4", "GBCT792EPY99BR7SDBQBTNPSHCY1TGH223")
            resValue("string", "bscscanKey5", "3AP5558QS44WAJBVPGG785R4G7VZW6616Z")
            resValue("string", "bscscanKey6", "RG1CIITEEAT58J7UIAJVV3KSA2I6RRQDD2")
            resValue("string", "otherScanKey", "Y855XHV4XKUC9DTRM2ZQG8XAQ96EJV221Q,43DEJEEMA1P81YAU555A1TECRY5FPIWCFH")
            resValue("string", "polygonscanKey", "39IT91NPCIP4WNW75U4YJVJ68NRIF8D4TT")
            resValue("string", "polygonscanKey2", "SFQYAM71TIP2WYFTGJSZC6QCJKA5855KB3")
            resValue("string", "polygonscanKey3", "VGJEVNYR36WWFTBXNMJS5HNP32WJ18IAEX")
            resValue("string", "polygonscanKey4", "66AK4I7D2RIP3KY2YY6GSEUVCWTERWNKFI")
            resValue("string", "snowtraceApiKey", "47IXTRAAFT1E1J4RNSPZPNB5EWUIQR16FG")
            resValue("string", "optimisticEtherscanApiKey", "745EUI4781T147M9QJRNS5G3Q5NFF2SJXP")
            resValue("string", "arbiscanApiKey", "4QWW522BV13BJCZMXH1JIB2ESJ7MZTSJYI")
            resValue("string", "gnosisscanApiKey", "KEXFAQKDUENZ5U9CW3ZKYJEJ84ZIHH9QTY")
            resValue("string", "ftmscanApiKey", "JAWRPW27KEMVXMJJ9UKY63CVPH3X5V9SMP")
            resValue("string", "is_release", "true")
            resValue("string", "guidesUrl", "https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/v1.2/index.json")
            resValue("string", "eduUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/refs/tags/v1.4/src/edu.json")
            resValue("string", "faqUrl", "https://raw.githubusercontent.com/horizontalsystems/Unstoppable-Wallet-Website/v1.3/src/faq.json")
            resValue("string", "coinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/coins.json")
            resValue("string", "providerCoinsJsonUrl", "https://raw.githubusercontent.com/horizontalsystems/cryptocurrencies/v0.21/provider.coins.json")
            resValue("string", "marketApiBaseUrl", "https://api.blocksdecoded.com")
            resValue("string", "marketApiKey", "IQf1uAjkthZp1i2pYzkXFDom")
            resValue("string", "openSeaApiKey", "bfbd6061a33e455c8581b594774fecb3")
            resValue("string", "walletConnectV2Key", "d98d5186f513a943fbbff4a755957fbf")
            resValue("string", "walletConnectV2Key1", "45b825481aab1907c73ff9499f199070")
            resValue("string", "solscanApiKey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVkQXQiOjE3Mzc0NDMwMjMzMTIsImVtYWlsIjoiMTgxMzgyNDgxNjlAMTYzLmNvbSIsImFjdGlvbiI6InRva2VuLWFwaSIsImFwaVZlcnNpb24iOiJ2MiIsImlhdCI6MTczNzQ0MzAyM30.FyLCMRfZJ0AiYzXFFPTmbUtYdu29u7PPCPV-txXbToM")
            resValue("string", "solanaAlchemyApiKey", "BOlzgqJUeGYe5E7K613Fm,Vmt7ucAGIMEux_c43Qqqf,uCordWq3EOD800awDx1kb,1uAryzn6DOEVs5PIugeoR,PKgWxOMarrHgyMESGjIkJ")
            resValue("string", "solanaJupiterApiKey", "ec901a97-0375-45b1-8b7d-da1ea9934cb0")
            resValue("string", "trongridApiKeys", "8f5ae2c8-8012-42a8-b0ca-ffc2741f6a29,578aa64f-a79f-4ee8-86e9-e9860e2d050a,1e92f1fc-41f8-401f-a7f6-5b719b6f1280,d1511874-1547-48df-9536-a32cc85949ac")
            resValue("string", "udnApiKey", "r2phzgatt_zt9-hd_wyvdjrdsrimnxgokm7knyag1malzgcz")
            resValue("string", "blocksDecodedEthereumRpc", "https://ethereum-mainnet.core.chainstack.com/a1911ee247f4f8de22c1f4e55865f616")
            resValue("string", "oneInchApiKey", "ElyK7s22HR0JD78CEXVPnpZA8UyuUwIl")
            resValue("string", "oneInchApiKey2", "eJ6FPSDW5z2vKD6oEcjQwnzFzvQ6nXgZ")
            resValue("string", "oneInchApiKey3", "6oMuaUZbbjSSzIvHJB4YodfvQItpBrlY")
            resValue("string", "safeswapv2safe4factory", "0xB3c827077312163c53E3822defE32cAffE574B42")
            resValue("string", "safeswapv2safe4codehash", "ad0e51aa7a058efb9eb40fd6385473f0175ee7419e8d4f91a4e0294ec12b2d13")
            resValue("string", "safeswapv2safe4router", "0x6476008C612dF9F8Db166844fFE39D24aEa12271")
            resValue("string", "chainalysisBaseUrl", "https://public.chainalysis.com/api/v1/")
            resValue("string", "chainalysisApiKey", "928bb256db73f1cb93e1b3366a145d9fbe06e28581c8b665b82ad70bbfef1db4")
            resValue("string", "hashDitBaseUrl", "https://service.hashdit.io/v2/hashdit/")
            resValue("string", "hashDitApiKey", "aGMkgODYiUFtTYrSRcEZsIfPHeASOlGYXClJZNWF")
            resValue("string", "uswapApiBaseUrl", "https://swap-api.unstoppable.money/v2/")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            pickFirsts += listOf(
                    "META-INF/atomicfu.kotlin_module",
                    "META-INF/FastDoubleParser-LICENSE",
                    "META-INF/FastDoubleParser-NOTICE",
                    "META-INF/io.netty.versions.properties"
            )
            excludes += listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE.md",
                    "google/protobuf/*.proto",
                    "'META-INF/NOTICE.md",
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
        dex {
            useLegacyPackaging = true
        }
    }

    lint {
        disable += "LogNotTimber"
        disable += "RemoveWorkManagerInitializer"
    }

    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.bouncycastle:bcprov-jdk15to18:1.68")).using(module("org.bouncycastle:bcprov-jdk15on:1.70"))
            substitute(module("com.google.protobuf:protobuf-java:3.6.1")).using(module("com.google.protobuf:protobuf-javalite:3.21.1"))
            substitute(module("net.jcip:jcip-annotations:1.0")).using(module("com.github.stephenc.jcip:jcip-annotations:1.0-1"))

            substitute(module("com.tinder.scarlet:scarlet:0.1.12")).using(module("com.walletconnect.Scarlet:scarlet:1.0.2"))
            substitute(module("com.tinder.scarlet:websocket-okhttp:0.1.12")).using(module("com.walletconnect.Scarlet:websocket-okhttp:1.0.2"))
            substitute(module("com.tinder.scarlet:stream-adapter-rxjava2:0.1.12")).using(module("com.walletconnect.Scarlet:stream-adapter-rxjava2:1.0.2"))
            substitute(module("com.tinder.scarlet:message-adapter-gson:0.1.12")).using(module("com.walletconnect.Scarlet:message-adapter-gson:1.0.2"))
            substitute(module("com.tinder.scarlet:lifecycle-android:0.1.12")).using(module("com.walletconnect.Scarlet:lifecycle-android:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:scarlet:1.0.0")).using(module("com.walletconnect.Scarlet:scarlet:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:websocket-okhttp:1.0.0")).using(module("com.walletconnect.Scarlet:websocket-okhttp:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:stream-adapter-rxjava2:1.0.0")).using(module("com.walletconnect.Scarlet:stream-adapter-rxjava2:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:message-adapter-gson:1.0.0")).using(module("com.walletconnect.Scarlet:message-adapter-gson:1.0.2"))
            substitute(module("com.github.WalletConnect.Scarlet:lifecycle-android:1.0.0")).using(module("com.walletconnect.Scarlet:lifecycle-android:1.0.2"))
        }

        resolutionStrategy.eachDependency {
            if (requested.group == "com.squareup.okhttp3") {
                useVersion("4.10.0")
            }
            if (requested.group == "org.web3j") {
                useVersion("4.9.8")
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // AndroidX Core
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.credentials)

    // Lifecycle
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.rxjava2)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.material3)

    // Google Material
    implementation(libs.google.material)

    // Accompanist
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.appcompat.theme)
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.permissions)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.adapter.rxjava2)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Rx
    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.rx2)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)

    // Logging
    implementation(libs.timber)
    debugImplementation(libs.leakcanary)

    // Markdown
    implementation(libs.commonmark)
    implementation(libs.markwon)

    // QR
    api(libs.zxing)
    implementation(libs.qrose)

    // Web3
    implementation(libs.web3j)
    implementation(libs.unstoppable.domains)

    // Wallet kits
//    implementation 'com.github.horizontalsystems:bitcoin-kit-android:098bed9'

    implementation(project(":bitcoin-kit-android:bitcoincore"))
    implementation(project(":bitcoin-kit-android:bitcoinkit"))
    implementation(project(":bitcoin-kit-android:dashkit"))
    implementation(project(":bitcoin-kit-android:bitcoincashkit"))
    implementation(project(":bitcoin-kit-android:litecoinkit"))
    implementation(project(":bitcoin-kit-android:safekit"))
    implementation(project(":bitcoin-kit-android:ecashkit"))
    implementation(project(":bitcoin-kit-android:dogecoinkit"))

    implementation(project(":ethereum-kit-android:ethereumkit"))
    implementation(project(":ethereum-kit-android:erc20kit"))
    implementation(project(":ethereum-kit-android:uniswapkit"))
    implementation(project(":ethereum-kit-android:oneinchkit"))
    implementation(project(":ethereum-kit-android:nftkit"))
    implementation(project(":ethereum-kit-android:wsafekit"))
    implementation(project(":solana-kit-android:solanakit"))


    implementation(libs.kit.ton)
    implementation(libs.kit.monero)
    implementation(libs.kit.zano)
    implementation(libs.kit.stellar)
    implementation(libs.kit.fee.rate)
    implementation(libs.kit.tron)
    implementation(libs.zcash.android.sdk)

    // BouncyCastle
    implementation(libs.bouncycastle)


    implementation(project(":market-kit-android:marketkit"))

    // Binance
    implementation(libs.binance.connector) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }

    // Desugar
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Exclude old version from wherever it's coming
    configurations.configureEach {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
        exclude(group = "org.bouncycastle", module = "bcutil-jdk18on")
    }

    // Tor
    implementation(libs.tor.android)
    implementation(libs.jtorctl)

    // Utils
    implementation(libs.circleindicator)
    implementation(libs.twitter.text)
    api(libs.android.shell)
    api(libs.portmapper)

    // UI modules

    implementation(project(":core"))
    implementation(project(":components:views"))
//    implementation project(":components:snackbar")
    implementation(project(":components:icons"))
    implementation(project(":components:chartview"))
    implementation(project(":components:seekbar"))

    implementation(project(":subscriptions-core"))
    implementation(project(":dapp-core"))

    // UI Tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.espresso.core)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.powermock.api.mockito2)
    testImplementation(libs.powermock.module.junit4)
    testImplementation(libs.spek.dsl.jvm)
    testRuntimeOnly(libs.spek.runner.junit5)
    testRuntimeOnly(libs.kotlin.reflect)

    // VPN
//    implementation(project(":vpn"))

//    implementation(project(":telegram:TMessagesProj"))


    implementation("androidx.multidex:multidex:2.0.1")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.0")
    implementation("org.bitcoinj:bitcoinj-core:0.14.3")
    testImplementation("junit:junit:4.12")
    testImplementation("org.mockito:mockito-core:1.+")
//    implementation 'com.google.protobuf:protobuf-java:3.5.1'

    implementation("com.lambdaworks:scrypt:1.4.0")

    implementation("com.github.xuexiangjys:XUpdate:2.1.5")
//    implementation 'com.zhy:okhttputils:2.6.2'

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("commons-codec:commons-codec:1.15")

    api(files("libs/mmkv-static-1.2.7.aar"))
}

// Flavor-specific dependencies must be added after evaluation
afterEvaluate {
    dependencies {
        "baseDebugImplementation"(project(":subscriptions-dev"))
        findProject(":subscriptions-google-play")?.let {
            "baseReleaseImplementation"(it)
        }

        "fdroidImplementation"(project(":subscriptions-fdroid"))
        "fdroidCiImplementation"(project(":subscriptions-fdroid"))
        "ciImplementation"(project(":subscriptions-dev"))

        findProject(":dapp-wallet-connect")?.let {
            "baseDebugImplementation"(it)
            "baseReleaseImplementation"(it)
            "ciImplementation"(it)
        }

        "baseDebugImplementation"(libs.androidx.credentials.play.services.auth)
        "baseReleaseImplementation"(libs.androidx.credentials.play.services.auth)
        "ciImplementation"(libs.androidx.credentials.play.services.auth)
    }
}

configurations.all {
    resolutionStrategy {
        cacheChangingModulesFor(0, TimeUnit.SECONDS)
        // PowerMock forces junit:4.12 which conflicts with androidTest deps requiring 4.13.2
        force("junit:junit:4.13.2")

        force("org.bitcoinj:bitcoinj-core:0.15.10")

        // Force Ktor version for TonKit
        force("io.ktor:ktor-utils:2.3.7")
        force("io.ktor:ktor-io:2.3.7")
        force("io.ktor:ktor-client-core:2.3.7")

        force("junit:junit:4.13.2")
        force("commons-codec:commons-codec:1.15")
    }

    exclude(group = "com.google.protobuf", module = "protobuf-java")
}
