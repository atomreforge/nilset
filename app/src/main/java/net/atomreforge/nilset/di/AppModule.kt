package net.atomreforge.nilset.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.BuildConfig
import net.atomreforge.nilset.core.command.CommandRegistry
import net.atomreforge.nilset.core.command.NilSetCommandCenter
import net.atomreforge.nilset.core.command.commands.ClearConsoleCommand
import net.atomreforge.nilset.core.command.commands.ClearDataCommand
import net.atomreforge.nilset.core.command.commands.NoLoginCommand
import net.atomreforge.nilset.core.command.commands.StatusCommand
import net.atomreforge.nilset.core.logging.AppLogger
import net.atomreforge.nilset.core.logging.LogLevel
import net.atomreforge.nilset.data.config.AppConfig
import net.atomreforge.nilset.data.config.ConfigLoader
import net.atomreforge.nilset.data.config.DurationParser
import net.atomreforge.nilset.data.repository.CalendarRepository
import net.atomreforge.nilset.data.repository.RemoteCalendarRepository
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.interceptor.AuthInterceptor
import net.atomreforge.nilset.data.remote.interceptor.TokenAuthenticator
import net.atomreforge.nilset.data.repository.RemoteSessionRepository
import net.atomreforge.nilset.data.repository.SessionScope
import net.atomreforge.nilset.data.repository.SessionRepository
import net.atomreforge.nilset.data.repository.SessionTokenRefresher
import net.atomreforge.nilset.data.session.PreferencesSessionDataStore
import net.atomreforge.nilset.data.session.SessionDataStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nilset_session",
)

private class HttpLogBridge(private val appLogger: AppLogger) : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        val safeMessage = redactSensitiveValues(message)
        val level = when {
            safeMessage.contains("HTTP FAILED", ignoreCase = true) -> LogLevel.ERROR
            safeMessage.contains(Regex("^<--\\s+5\\d{2}\\b")) -> LogLevel.ERROR
            safeMessage.contains(Regex("^<--\\s+4\\d{2}\\b")) -> LogLevel.WARNING
            else -> LogLevel.INFO
        }
        appLogger.log(level, "HTTP", safeMessage)
    }

    private fun redactSensitiveValues(message: String): String = message
        .replace(Regex("(?i)\"password\"\\s*:\\s*\"[^\"]*\""), "\"password\":\"[REDACTED]\"")
        .replace(Regex("(?i)\"access_token\"\\s*:\\s*\"[^\"]*\""), "\"access_token\":\"[REDACTED]\"")
        .replace(Regex("(?i)\"refresh_token\"\\s*:\\s*\"[^\"]*\""), "\"refresh_token\":\"[REDACTED]\"")
        .replace(Regex("(?i)\"register_code\"\\s*:\\s*\"[^\"]*\""), "\"register_code\":\"[REDACTED]\"")
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @SessionScope
    fun provideSessionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionDataStore

    @Provides
    @Singleton
    fun provideAppConfig(@ApplicationContext context: Context): AppConfig =
        ConfigLoader.mustLoad(context)

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: AppConfig,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        appLogger: AppLogger,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(DurationParser.parse(config.api.timeouts.connect).toMillis(), TimeUnit.MILLISECONDS)
        .readTimeout(DurationParser.parse(config.api.timeouts.read).toMillis(), TimeUnit.MILLISECONDS)
        .apply {
            if (config.log.isHttpLoggingEnabled) {
                addInterceptor(HttpLoggingInterceptor(HttpLogBridge(appLogger)).apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                })
            }
        }
        .apply {
            if (config.auth.autoRefresh) {
                authenticator(tokenAuthenticator)
            }
        }
        .addInterceptor(authInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        config: AppConfig,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.api.baseUrl.trimEnd('/') + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideDaizyNightApi(retrofit: Retrofit): DaizyNightApi =
        retrofit.create(DaizyNightApi::class.java)

    @Provides
    @Singleton
    fun provideCommandRegistry(): CommandRegistry = CommandRegistry(
        commands = listOf(
            NoLoginCommand(),
            ClearDataCommand(),
            ClearConsoleCommand(),
            StatusCommand(),
        ),
        isDebug = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideCommandCenter(registry: CommandRegistry): NilSetCommandCenter =
        NilSetCommandCenter(registry)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: RemoteSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSessionTokenRefresher(impl: RemoteSessionRepository): SessionTokenRefresher

    @Binds
    @Singleton
    abstract fun bindSessionDataStore(impl: PreferencesSessionDataStore): SessionDataStore

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: RemoteCalendarRepository): CalendarRepository
}
