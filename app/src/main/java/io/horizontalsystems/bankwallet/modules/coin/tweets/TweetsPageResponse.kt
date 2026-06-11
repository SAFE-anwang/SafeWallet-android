package io.horizontalsystems.bankwallet.modules.coin.tweets

import com.google.gson.annotations.SerializedName
import java.util.*

data class TweetsPageResponse(
    val data: List<RawTweet>?,
    val includes: Includes?,
    val meta: Meta?,
) {
    data class Includes(
        @SerializedName("media")
        val media: List<Media>?,
        @SerializedName("polls")
        val polls: List<Poll>?,
        @SerializedName("users")
        val users: List<TwitterUser>?,
        @SerializedName("tweets")
        val referencedTweets: List<RawTweet>?,
        @SerializedName("places")
        val places: List<Place>?
    )

    data class Meta(
        @SerializedName("result_count")
        val resultCount: Int,
        @SerializedName("next_token")
        val nextToken: String?,
        @SerializedName("previous_token")
        val previousToken: String?,
        @SerializedName("newest_id")
        val newestId: String?,
        @SerializedName("oldest_id")
        val oldestId: String?,
    )

    fun tweets(user: TwitterUser): List<Tweet> {
        return data?.map { rawTweet ->
            buildTweet(rawTweet, user)
        } ?: emptyList()
    }

    /**
     * Builds tweets from search results where each tweet may have a different author.
     * Resolves the author for each tweet from the includes.users list.
     */
    fun tweetsFromSearch(): List<Tweet> {
        val usersById = includes?.users?.associateBy { it.id } ?: emptyMap()
        return data?.mapNotNull { rawTweet ->
            val authorId = rawTweet.authorId ?: return@mapNotNull null
            val user = usersById[authorId] ?: return@mapNotNull null
            buildTweet(rawTweet, user)
        } ?: emptyList()
    }

    private fun buildTweet(rawTweet: RawTweet, user: TwitterUser): Tweet {
        val attachments = mutableListOf<Tweet.Attachment>()
        rawTweet.attachments?.mediaKeys?.let { mediaKeys ->
            for (mediaKey in mediaKeys) {
                val media = includes?.media?.find { singleMedia -> singleMedia.key == mediaKey }
                if (media != null) {
                    when (media.type) {
                        "photo" -> {
                            media.url?.let {
                                attachments.add(Tweet.Attachment.Photo(it))
                            }
                        }
                        "video", "animated_gif" -> {
                            media.previewImageUrl?.let { previewUrl ->
                                val videoUrl = media.variants
                                    ?.filter { it.bitRate != null }
                                    ?.maxByOrNull { it.bitRate!! }
                                    ?.url
                                attachments.add(Tweet.Attachment.Video(previewUrl, videoUrl))
                            }
                        }
                    }
                }
            }
        }
        rawTweet.attachments?.pollIds?.let { pollIds ->
            for (pollId in pollIds) {
                val poll = includes?.polls?.find { it.id == pollId }
                if (poll != null) {
                    val options = poll.options.map { Tweet.Attachment.Poll.Option(it.position, it.label, it.votes) }
                    attachments.add(Tweet.Attachment.Poll(options, poll.durationMinutes, poll.endDatetime, poll.votingStatus))
                }
            }
        }

        // Prefer note_tweet text for long-form tweets (X Premium)
        val tweetText = rawTweet.noteTweet?.text?.takeIf { it.isNotBlank() } ?: rawTweet.text

        var referencedTweet: Tweet.ReferencedTweetXxx? = null
        rawTweet.referencedTweets?.firstOrNull()?.let { tweetReference ->
            includes?.referencedTweets?.find { tweet -> tweet.id == tweetReference.id }?.let { rawReferencedTweet ->
                includes?.users?.find { u -> u.id == rawReferencedTweet.authorId }?.let { referencedTweetAuthor ->
                    val refTweetText = rawReferencedTweet.noteTweet?.text?.takeIf { it.isNotBlank() } ?: rawReferencedTweet.text
                    val tweet = Tweet(
                        rawReferencedTweet.id,
                        referencedTweetAuthor,
                        refTweetText,
                        rawReferencedTweet.date,
                        listOf(),
                        null,
                        rawReferencedTweet.publicMetrics,
                        rawReferencedTweet.conversationId,
                        rawReferencedTweet.inReplyToUserId,
                        rawReferencedTweet.lang,
                        rawReferencedTweet.source,
                    )

                    referencedTweet = when (tweetReference.type) {
                        "quoted" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Quoted, tweet)
                        "retweeted" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Retweeted, tweet)
                        "replied_to" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Replied, tweet)
                        else -> null
                    }
                }
            }
        }

        return Tweet(
            rawTweet.id,
            user,
            tweetText,
            rawTweet.date,
            attachments,
            referencedTweet,
            rawTweet.publicMetrics,
            rawTweet.conversationId,
            rawTweet.inReplyToUserId,
            rawTweet.lang,
            rawTweet.source,
        )
    }

    data class RawTweet(
        val id: String,
        @SerializedName("created_at")
        val date: Date,
        @SerializedName("author_id")
        val authorId: String?,
        val text: String,
        val attachments: Attachments?,
        @SerializedName("referenced_tweets")
        val referencedTweets: List<ReferencedTweet>?,
        @SerializedName("public_metrics")
        val publicMetrics: PublicMetrics?,
        @SerializedName("conversation_id")
        val conversationId: String?,
        @SerializedName("in_reply_to_user_id")
        val inReplyToUserId: String?,
        val lang: String?,
        val source: String?,
        val entities: Entities?,
        @SerializedName("edit_history_tweet_ids")
        val editHistoryTweetIds: List<String>?,
        @SerializedName("note_tweet")
        val noteTweet: NoteTweet?,
        @SerializedName("possibly_sensitive")
        val possiblySensitive: Boolean?,
        @SerializedName("context_annotations")
        val contextAnnotations: List<ContextAnnotation>?,
        @SerializedName("reply_settings")
        val replySettings: String?,
    ) {
        data class Attachments(
            @SerializedName("media_keys")
            val mediaKeys: List<String>?,
            @SerializedName("poll_ids")
            val pollIds: List<String>?,
        )

        data class PublicMetrics(
            @SerializedName("retweet_count")
            val retweetCount: Int,
            @SerializedName("reply_count")
            val replyCount: Int,
            @SerializedName("like_count")
            val likeCount: Int,
            @SerializedName("quote_count")
            val quoteCount: Int,
            @SerializedName("bookmark_count")
            val bookmarkCount: Int,
            @SerializedName("impression_count")
            val impressionCount: Int,
        )

        data class Entities(
            val mentions: List<Mention>?,
            val urls: List<UrlEntity>?,
            val hashtags: List<Hashtag>?,
            val cashtags: List<Cashtag>?,
            val annotations: List<Annotation>?,
        ) {
            data class Mention(
                val start: Int,
                val end: Int,
                val username: String,
                val id: String,
            )

            data class UrlEntity(
                val start: Int,
                val end: Int,
                val url: String,
                @SerializedName("expanded_url")
                val expandedUrl: String?,
                @SerializedName("display_url")
                val displayUrl: String?,
                val title: String?,
                val description: String?,
                @SerializedName("unwound_url")
                val unwoundUrl: String?,
                val images: List<UrlImage>?,
                val status: Int?,
            )

            data class UrlImage(
                val url: String,
                val width: Int,
                val height: Int,
            )

            data class Hashtag(
                val start: Int,
                val end: Int,
                val tag: String,
            )

            data class Cashtag(
                val start: Int,
                val end: Int,
                val tag: String,
            )

            data class Annotation(
                val start: Int,
                val end: Int,
                val probability: Double,
                val type: String,
                @SerializedName("normalized_text")
                val normalizedText: String,
            )
        }

        data class NoteTweet(
            val text: String,
        )

        data class ContextAnnotation(
            val domain: Domain,
            val entity: Entity,
        ) {
            data class Domain(
                val id: String,
                val name: String,
                val description: String?,
            )

            data class Entity(
                val id: String,
                val name: String,
                val description: String?,
            )
        }
    }

    data class Media(
        @SerializedName("media_key")
        val key: String,
        val type: String,
        val url: String?,
        @SerializedName("preview_image_url")
        val previewImageUrl: String?,
        val variants: List<Variant>?,
        val width: Int?,
        val height: Int?,
        @SerializedName("alt_text")
        val altText: String?,
        @SerializedName("duration_ms")
        val durationMs: Int?,
        @SerializedName("public_metrics")
        val publicMetrics: MediaPublicMetrics?,
    ) {
        data class Variant(
            @SerializedName("bit_rate")
            val bitRate: Int?,
            @SerializedName("content_type")
            val contentType: String?,
            val url: String?,
        )

        data class MediaPublicMetrics(
            @SerializedName("view_count")
            val viewCount: Int?,
        )
    }

    data class Poll(
        val id: String,
        val options: List<PollOption>,
        @SerializedName("duration_minutes")
        val durationMinutes: Int?,
        @SerializedName("end_datetime")
        val endDatetime: String?,
        @SerializedName("voting_status")
        val votingStatus: String?,
    )

    data class PollOption(
        val position: Int,
        val label: String,
        val votes: Int,
    )

    data class ReferencedTweet(
        val type: String,
        val id: String,
    )

    data class Place(
        val id: String,
        @SerializedName("full_name")
        val fullName: String?,
        val name: String?,
        @SerializedName("country_code")
        val countryCode: String?,
        @SerializedName("place_type")
        val placeType: String?,
    )
}
