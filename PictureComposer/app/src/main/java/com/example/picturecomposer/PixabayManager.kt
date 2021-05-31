package com.example.picturecomposer

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PixabayManager {

    private val okHttpClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()

        // Set up our OkHttpClient instance to log all network traffic to Logcat
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)

        okHttpClient = builder.build()
    }

    fun retrieveImages(query: String, apiKey: String): MutableList<RefPhoto> {

        //perform networking request
        val request = Request.Builder()
            .url("https://pixabay.com/api/?key=$apiKey&q=$query&sort_by=views\n")
            .method("GET", null)
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseString: String? = response.body?.string()

        val urls = mutableListOf<RefPhoto>()

        //parse response string
        if (!responseString.isNullOrEmpty() && response.isSuccessful) {
            val json = JSONObject(responseString)
            val hits = json.getJSONArray("hits")
            val hit0 = hits.getJSONObject(0)
            val ref0 = RefPhoto(hit0.getString("webformatURL"), (hit0.getString("pageURL")))
            urls.add(ref0)
            val hit1 = hits.getJSONObject(1)
            val ref1 = RefPhoto(hit1.getString("webformatURL"), (hit1.getString("pageURL")))
            urls.add(ref1)
            val hit2 = hits.getJSONObject(2)
            val ref2 = RefPhoto(hit2.getString("webformatURL"), (hit2.getString("pageURL")))
            urls.add(ref2)
        }

        return urls
    }

}