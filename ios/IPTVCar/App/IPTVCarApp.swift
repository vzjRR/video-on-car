import SwiftUI

@main
struct IPTVCarApp: App {
    var body: some Scene {
        WindowGroup {
            RootTabView()
        }
    }
}

struct RootTabView: View {
    var body: some View {
        TabView {
            LiveView().tabItem { Label("Live", systemImage: "tv") }
            MoviesView().tabItem { Label("Movies", systemImage: "film") }
            SeriesView().tabItem { Label("Series", systemImage: "rectangle.stack") }
            FavoritesView().tabItem { Label("Favorites", systemImage: "star") }
            ContinueWatchingView().tabItem { Label("Continue", systemImage: "clock.arrow.circlepath") }
            SettingsView().tabItem { Label("Settings", systemImage: "gear") }
        }
    }
}
