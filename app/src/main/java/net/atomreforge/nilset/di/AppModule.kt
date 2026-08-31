package net.atomreforge.nilset.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.BuildConfig
import net.atomreforge.nilset.core.command.CommandRegistry
import net.atomreforge.nilset.core.command.NilSetCommandCenter
import net.atomreforge.nilset.core.command.commands.ClearConsoleCommand
import net.atomreforge.nilset.core.command.commands.ClearDataCommand
import net.atomreforge.nilset.core.command.commands.NoLoginCommand
import net.atomreforge.nilset.core.command.commands.StatusCommand
import net.atomreforge.nilset.data.config.AppConfig
import net.atomreforge.nilset.data.config.ConfigLoader
import net.atomreforge.nilset.data.config.DurationParser
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.interceptor.AuthInterceptor
import net.atomreforge.nilset.data.repository.RemoteSessionRepository
import net.atomreforge.nilset.data.repository.SessionRepository
import net.atomreforge.nilset.data.session.SessionDataStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nilset_session"
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(DurationParser.parse(config.api.timeouts.connect).toMillis(), TimeUnit.MILLISECONDS)
        .readTimeout(DurationParser.parse(config.api.timeouts.read).toMillis(), TimeUnit.MILLISECONDS)
        .apply {
            if (config.log.isHttpLoggingEnabled) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
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
    fun provideSessionDataStore(@ApplicationContext context: Context): SessionDataStore =
        SessionDataStore(context.sessionDataStore)

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
}
