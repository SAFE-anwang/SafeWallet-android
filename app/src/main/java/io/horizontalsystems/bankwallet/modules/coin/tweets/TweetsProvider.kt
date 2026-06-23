package io.horizontalsystems.bankwallet.modules.coin.tweets

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

class TweetsProvider(private val bearerToken: String?) {
    interface TwitterAPI {
        @GET("users/by")
        fun getUsers(
            @Query("usernames") usernames: String,
            @Query("user.fields") userFields: String,
            @Header("Authorization") authHeader: String,
        ): Single<UsersResponse>

        @GET("users/by/username/{username}")
        fun getUserByUsername(
            @Path("username") username: String,
            @Query("user.fields") userFields: String,
            @Header("Authorization") authHeader: String,
        ): Single<UserSingleResponse>

        @GET("users/{userId}/tweets")
        fun getTweets(
            @Path("userId") userId: String,
            @Query("expansions") expansions: String,
            @Query("media.fields") mediaFields: String,
            @Query("tweet.fields") tweetFields: String,
            @Query("user.fields") userFields: String,
            @Query("max_results") maxResults: Int,
            @Header("Authorization") authHeader: String,
        ): Single<TweetsPageResponse>

        @GET("tweets/search/recent")
        fun searchTweets(
            @Query("query") query: String,
            @Query("expansions") expansions: String,
            @Query("media.fields") mediaFields: String,
            @Query("tweet.fields") tweetFields: String,
            @Query("user.fields") userFields: String,
            @Query("max_results") maxResults: Int,
            @Header("Authorization") authHeader: String,
        ): Single<TweetsPageResponse>
    }

    data class UsersResponse(val data: List<TwitterUser>)
    data class UserSingleResponse(val data: TwitterUser)

    class UserNotFound : Exception()

    private val baseUrl = "https://api.x.com/2/"
    private val service = APIClient.retrofit(baseUrl, 60, true).create(TwitterAPI::class.java)

    companion object {
        private const val USER_FIELDS =
            "created_at,description,entities,id,location,name,pinned_tweet_id,profile_image_url,protected,public_metrics,url,username,verified,verified_type"
        private const val TWEET_FIELDS =
            "attachments,author_id,context_annotations,conversation_id,created_at,edit_history_tweet_ids,entities,geo,id,in_reply_to_user_id,lang,note_tweet,possibly_sensitive,public_metrics,referenced_tweets,reply_settings,source,text,withheld"
        private const val MEDIA_FIELDS =
            "alt_text,duration_ms,height,media_key,preview_image_url,public_metrics,type,url,variants,width"
        private const val EXPANSIONS =
            "attachments.poll_ids,attachments.media_keys,author_id,edit_history_tweet_ids,entities.mentions.username,geo.place_id,in_reply_to_user_id,referenced_tweets.id,referenced_tweets.id.author_id"
    }

    fun userRequestSingle(username: String): Single<TwitterUser> {
        return service.getUsers(username, USER_FIELDS, "Bearer $bearerToken")
            .map { it.data }
            .flatMap {
                when {
                    it.isNotEmpty() -> Single.just(it.first())
                    else -> Single.error(UserNotFound())
                }
            }
    }

    fun userByUsernameSingle(username: String): Single<TwitterUser> {
        return service.getUserByUsername(username, USER_FIELDS, "Bearer $bearerToken")
            .map { it.data }
    }

    fun tweetsSingle(user: TwitterUser): Single<List<Tweet>> {
        return service
            .getTweets(
                userId = user.id,
                expansions = EXPANSIONS,
                mediaFields = MEDIA_FIELDS,
                tweetFields = TWEET_FIELDS,
                userFields = USER_FIELDS,
                maxResults = 50,
                authHeader = "Bearer $bearerToken"
            )
            .map {
                it.tweets(user)
            }
    }

    fun searchTweetsSingle(
        query: String,
        maxResults: Int = 50,
    ): Single<List<Tweet>> {
        return service
            .searchTweets(
                query = query,
                expansions = EXPANSIONS,
                mediaFields = MEDIA_FIELDS,
                tweetFields = TWEET_FIELDS,
                userFields = USER_FIELDS,
                maxResults = maxResults,
                authHeader = "Bearer $bearerToken"
            )
            .map { response ->
                response.tweetsFromSearch()
            }
    }

}

