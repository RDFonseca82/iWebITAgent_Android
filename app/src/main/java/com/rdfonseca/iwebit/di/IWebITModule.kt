package com.rdfonseca.iwebit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.google.gson.Gson
import com.rdfonseca.iwebit.data.local.IWebITDatabase
import com.rdfonseca.iwebit.data.local.sync.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import com.rdfonseca.iwebit.common.Constants.BASE_URL
import com.rdfonseca.iwebit.common.Constants.DOMAIN_URL
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.remote.IWebITDomainApi
import com.rdfonseca.iwebit.data.repository.SupportMsgRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.data.util.GsonParser
import com.rdfonseca.iwebit.domain.repository.SupportMsgRepository
import com.rdfonseca.iwebit.domain.use_case.GetSupportMessages
import com.rdfonseca.iwebit.domain.use_case.LogIn
import com.rdfonseca.iwebit.domain.use_case.PostSupportMessage
import com.rdfonseca.iwebit.domain.use_case.UploadScreenshot
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object IWebITModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext appContext: Context): Context = appContext

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): IWebITDatabase {
        return Room.databaseBuilder(
            appContext,
            IWebITDatabase::class.java,
            "iwebit.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: IWebITDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }

    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> = appContext.dataStore

    @Provides
    @Singleton
    fun provideGetSupportMessagesUseCase(repository: SupportMsgRepository): GetSupportMessages {
        return GetSupportMessages(repository)
    }

    @Provides
    @Singleton
    fun providePostSupportMessageUseCase(repository: SupportMsgRepository): PostSupportMessage {
        return PostSupportMessage(repository)
    }

    @Provides
    @Singleton
    fun provideLogInUseCase(repository: SupportMsgRepository): LogIn {
        return LogIn(repository)
    }

    @Provides
    @Singleton
    fun provideUploadScreenshotUseCase(repository: SupportMsgRepository): UploadScreenshot {
        return UploadScreenshot(repository)
    }

    @Provides
    @Singleton
    fun provideSupportMsgRepository(
        api: IWebITApi,
        apiDomain: IWebITDomainApi,
        @ApplicationContext context: Context,
        converters: Converters
    ): SupportMsgRepository {
        return SupportMsgRepositoryImpl(
            api,
            apiDomain,
            converters,
            context
        )
    }

    @Provides
    @Singleton
    fun provideIWebITApi(@ApplicationContext appContext: Context): IWebITApi {
        val client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.MINUTES)
            .connectTimeout(1, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val request = chain.request()
                val startedAt = System.nanoTime()
                try {
                    val response = chain.proceed(request)
                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                    logAndWrite(
                        appContext,
                        "HTTP ${request.method} ${request.url.host}${request.url.encodedPath} -> ${response.code} (${elapsedMs}ms)",
                        "IWebITModule",
                        "http"
                    )
                    response
                } catch (error: Exception) {
                    logAndWrite(
                        appContext,
                        "HTTP ${request.method} ${request.url.host}${request.url.encodedPath} falhou: ${error.javaClass.simpleName}",
                        "IWebITModule",
                        "http",
                        "ERROR"
                    )
                    throw error
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(IWebITApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIWebITDomainApi(@ApplicationContext appContext: Context): IWebITDomainApi {
        val client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.MINUTES)
            .connectTimeout(1, TimeUnit.MINUTES)
            .addInterceptor { chain ->
                val request = chain.request()
                val startedAt = System.nanoTime()
                try {
                    val response = chain.proceed(request)
                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                    logAndWrite(
                        appContext,
                        "HTTP ${request.method} ${request.url.host}${request.url.encodedPath} -> ${response.code} (${elapsedMs}ms)",
                        "IWebITModule",
                        "http"
                    )
                    response
                } catch (error: Exception) {
                    logAndWrite(
                        appContext,
                        "HTTP ${request.method} ${request.url.host}${request.url.encodedPath} falhou: ${error.javaClass.simpleName}",
                        "IWebITModule",
                        "http",
                        "ERROR"
                    )
                    throw error
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(DOMAIN_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(IWebITDomainApi::class.java)
    }

    @Provides
    @Singleton
    fun provideConverter(): Converters {
        return Converters(GsonParser(Gson()))
    }

}