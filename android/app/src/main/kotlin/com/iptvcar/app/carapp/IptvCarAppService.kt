package com.iptvcar.app.carapp

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for the Android for Cars App Library (Android Auto /
 * Android Automotive OS). This is the officially supported vehicle
 * browsing surface referenced in docs/ARCHITECTURE.md — categories,
 * lists, and item selection for Live TV, Movies, and Series, using only
 * public androidx.car.app templates.
 */
class IptvCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator =
        // Accept Android Auto / Android Automotive OS hosts signed by Google,
        // per the standard car-app-library validator (no custom allowlist
        // needed for this app).
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = IptvCarSession()
}
