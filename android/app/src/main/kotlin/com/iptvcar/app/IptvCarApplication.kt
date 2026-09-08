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
    }
}
