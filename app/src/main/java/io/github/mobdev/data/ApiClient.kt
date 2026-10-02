package io.github.mobdev.data

import com.google.gson.GsonBuilder
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://faerytea.name/"

    // Resolves faerytea.name directly to its server IP 213.189.221.170 if Android emulator DNS fails
    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                if (hostname.equals("faerytea.name", ignoreCase = true)) {
                    listOf(
                        InetAddress.getByAddress(
                            "faerytea.name",
                            byteArrayOf(213.toByte(), 189.toByte(), 221.toByte(), 170.toByte()),
                        ),
                    )
                } else {
                    throw e
                }
            }
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(MessageData::class.java, MessageDataAdapter())
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .dns(customDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                AuthEvents.notifyUnauthorized()
            }
            response
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: ChatApi = retrofit.create(ChatApi::class.java)
}
