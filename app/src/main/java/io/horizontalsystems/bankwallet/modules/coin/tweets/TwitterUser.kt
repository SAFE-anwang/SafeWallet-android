package io.horizontalsystems.bankwallet.modules.coin.tweets

import com.google.gson.annotations.SerializedName
import java.util.*

data class TwitterUser(
    val id: String,
    val name: String,
    val username: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String,
    val description: String? = null,
    val verified: Boolean? = null,
    @SerializedName("verified_type")
    val verifiedType: String? = null,
    @SerializedName("public_metrics")
    val publicMetrics: PublicMetrics? = null,
    val url: String? = null,
    @SerializedName("created_at")
    val createdAt: Date? = null,
    @SerializedName("pinned_tweet_id")
    val pinnedTweetId: String? = null,
    val protected: Boolean? = null,
    val location: String? = null,
    val entities: UserEntities? = null,
) {
    data class PublicMetrics(
        @SerializedName("followers_count")
        val followersCount: Int,
        @SerializedName("following_count")
        val followingCount: Int,
        @SerializedName("tweet_count")
        val tweetCount: Int,
        @SerializedName("listed_count")
        val listedCount: Int,
        @SerializedName("like_count")
        val likeCount: Int,
        @SerializedName("media_count")
        val mediaCount: Int?,
    )

    data class UserEntities(
        val url: UrlEntity?,
        val description: DescriptionEntity?,
    ) {
        data class UrlEntity(
            val urls: List<UrlInfo>?,
        )

        data class DescriptionEntity(
            val urls: List<UrlInfo>?,
            val hashtags: List<UserHashtag>?,
            val mentions: List<UserMention>?,
            val cashtags: List<UserCashtag>?,
        )

        data class UrlInfo(
            val start: Int,
            val end: Int,
            val url: String,
            @SerializedName("expanded_url")
            val expandedUrl: String?,
            @SerializedName("display_url")
            val displayUrl: String?,
        )

        data class UserHashtag(
            val start: Int,
            val end: Int,
            val tag: String,
        )

        data class UserMention(
            val start: Int,
            val end: Int,
            val username: String,
        )

        data class UserCashtag(
            val start: Int,
            val end: Int,
            val tag: String,
        )
    }
}
