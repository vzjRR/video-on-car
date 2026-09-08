package com.iptvcar.app.carapp

import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Template
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import android.content.Intent
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.core.model.ContentType

class IptvCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = RootScreen(carContext)
}

/**
 * Vehicle home screen: Live TV / Movies / Series, mirroring the phone's
 * Home screen sections (docs/ARCHITECTURE.md "vehicle browsing UI").
 */
class RootScreen(carContext: androidx.car.app.CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication
        val hasProvider = app.providerRepository.listProviders().isNotEmpty()

        val builder = ItemList.Builder()
        if (!hasProvider) {
            builder.addItem(
                Row.Builder()
                    .setTitle("No provider configured")
                    .addText("Open the phone app's Settings to add an IPTV provider first.")
                    .build()
            )
        } else {
            builder
                .addItem(rowFor("Live TV") { screenManager.push(CategoryListScreen(carContext, ContentType.LIVE)) })
                .addItem(rowFor("Movies") { screenManager.push(CategoryListScreen(carContext, ContentType.MOVIE)) })
                .addItem(rowFor("Series") { screenManager.push(CategoryListScreen(carContext, ContentType.SERIES)) })
        }

        return ListTemplate.Builder()
            .setTitle("IPTV Car")
            .setSingleList(builder.build())
            .build()
    }

    private fun rowFor(title: String, onClick: () -> Unit): Row =
        Row.Builder().setTitle(title).setOnClickListener(onClick).build()
}
