package com.iptvcar.app

import android.app.Application
import com.iptvcar.app.data.FavoritesRepository
import com.iptvcar.app.data.ProviderRepository
import com.iptvcar.app.data.ResumeRepository

class IptvCarApplication : Application() {

    lateinit var providerRepository: ProviderRepository
        private set
    lateinit var resumeRepository: ResumeRepository
        private set
    lateinit var favoritesRepository: FavoritesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        providerRepository = ProviderRepository(this)
        resumeRepository = ResumeRepository(this)
        favoritesRepository = FavoritesRepository(this)
        seedProviderIfConfigured()
    }

    /**
     * Optional first-launch convenience: if this build was compiled with
     * android/app/seed.properties (gitignored, never committed — see
     * app/build.gradle.kts), and no provider has been added yet, add it
     * automatically so the app opens ready to use instead of requiring
     * manual entry in Settings.
     */
    private fun seedProviderIfConfigured() {
        if (BuildConfig.SEED_XTREAM_URL.isBlank()) return
        if (providerRepository.listProviders().isNotEmpty()) return
        providerRepository.addXtreamProvider(
            displayName = BuildConfig.SEED_DISPLAY_NAME.ifBlank { "My IPTV" },
            baseUrl = BuildConfig.SEED_XTREAM_URL,
            username = BuildConfig.SEED_XTREAM_USER,
            password = BuildConfig.SEED_XTREAM_PASS,
        )
    }
}
