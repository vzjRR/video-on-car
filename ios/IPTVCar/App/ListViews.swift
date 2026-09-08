import SwiftUI

struct FavoritesView: View {
    @State private var favorites: [Favorite] = FavoritesStore.shared.list()

    var body: some View {
        NavigationView {
            Group {
                if favorites.isEmpty {
                    Text("No favorites yet.").padding()
                } else {
                    List(favorites, id: \.contentId) { favorite in
                        Text("\(favorite.contentType.rawValue.capitalized) · \(favorite.contentId)")
                    }
                }
            }
            .navigationTitle("Favorites")
            .onAppear { favorites = FavoritesStore.shared.list() }
        }
    }
}

struct ContinueWatchingView: View {
    @State private var items: [ResumeState] = ResumeStore.shared.allContinueWatching()

    var body: some View {
        NavigationView {
            Group {
                if items.isEmpty {
                    Text("Nothing in progress.").padding()
                } else {
                    List(items, id: \.contentId) { resume in
                        let percent = resume.durationSeconds > 0 ? Int(resume.positionSeconds / resume.durationSeconds * 100) : 0
                        VStack(alignment: .leading) {
                            Text("\(resume.contentType.rawValue.capitalized) · \(resume.contentId)")
                            Text("\(percent)% watched").font(.caption).foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("Continue Watching")
            .onAppear { items = ResumeStore.shared.allContinueWatching() }
        }
    }
}
