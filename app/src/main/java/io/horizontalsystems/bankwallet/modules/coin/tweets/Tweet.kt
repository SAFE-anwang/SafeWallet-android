package io.horizontalsystems.bankwallet.modules.coin.tweets

import java.util.*

data class Tweet(
    val id: String,
    val user: TwitterUser,
    val text: String,
    val date: Date,
    val attachments: List<Attachment>,
    val referencedTweet: ReferencedTweetXxx?,
    val publicMetrics: TweetsPageResponse.RawTweet.PublicMetrics? = null,
    val conversationId: String? = null,
    val inReplyToUserId: String? = null,
    val lang: String? = null,
    val source: String? = null,
) {
    sealed class Attachment {
        class Photo(val url: String) : Attachment()
        class Video(
            val previewImageUrl: String,
            val videoUrl: String? = null,
        ) : Attachment()
        class Poll(
            val options: List<Option>,
            val durationMinutes: Int? = null,
            val endDatetime: String? = null,
            val votingStatus: String? = null,
        ) : Attachment() {
            data class Option(val position: Int, val label: String, val votes: Int)
        }
    }

    enum class ReferenceType {
        Quoted, Retweeted, Replied
    }

    data class ReferencedTweetXxx(val referenceType: ReferenceType, val tweet: Tweet)
}
