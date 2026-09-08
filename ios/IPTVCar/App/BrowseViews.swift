import SwiftUI

private enum BrowseLevel { case categories, items, seriesDetail }

struct LiveView: View {
    @State private var categories: [Category] = []
    @State private var channels: [LiveChannel] = []
    @State private var level: BrowseLevel = .categories
    @State private var errorMessage: String?
    @State private var nowPlaying: LiveChannel?

    var body: some View {
        NavigationView {
            content
                .navigationTitle("Live TV")
                .task { await loadCategories() }
        }
        .sheet(item: $nowPlaying) { channel in
            PlayerView(streamUrl: channel.streamUrl, title: channel.name)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let provider = ProviderRepository.shared.listProviders().first {
            if let errorMessage { Text(errorMessage).foregroundColor(.red).padding() }
            else if level == .categories {
                List(categories) { category in
                    Button(category.name) { Task { await loadChannels(provider: provider, category: category) } }
                }
            } else {
                List(channels) { channel in
                    Button(channel.name) { nowPlaying = channel }
                }
            }
        } else {
            Text("No IPTV provider configured yet. Go to Settings to add one.").padding()
        }
    }

    private func loadCategories() async {
        guard let provider = ProviderRepository.shared.listProviders().first else { return }
        do { categories = try await ProviderRepository.shared.loadLiveCategories(provider) }
        catch { errorMessage = "Failed to load live categories: \(error.localizedDescription)" }
    }

    private func loadChannels(provider: Provider, category: Category) async {
        do {
            channels = try await ProviderRepository.shared.loadLiveChannels(provider, categoryId: category.id)
            level = .items
        } catch { errorMessage = "Failed to load channels: \(error.localizedDescription)" }
    }
}

struct MoviesView: View {
    @State private var categories: [Category] = []
    @State private var movies: [Movie] = []
    @State private var level: BrowseLevel = .categories
    @State private var errorMessage: String?
    @State private var nowPlaying: Movie?

    var body: some View {
        NavigationView {
            content
                .navigationTitle("Movies")
                .task { await loadCategories() }
        }
        .sheet(item: $nowPlaying) { movie in
            PlayerView(streamUrl: movie.streamUrl, title: movie.title)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let provider = ProviderRepository.shared.listProviders().first {
            if let errorMessage { Text(errorMessage).foregroundColor(.red).padding() }
            else if level == .categories {
                List(categories) { category in
                    Button(category.name) { Task { await loadMovies(provider: provider, category: category) } }
                }
            } else {
                List(movies) { movie in
                    Button(movie.year != nil ? "\(movie.title) (\(movie.year!))" : movie.title) { nowPlaying = movie }
                }
            }
        } else {
            Text("No IPTV provider configured yet. Go to Settings to add one.").padding()
        }
    }

    private func loadCategories() async {
        guard let provider = ProviderRepository.shared.listProviders().first else { return }
        do { categories = try await ProviderRepository.shared.loadMovieCategories(provider) }
        catch { errorMessage = "Failed to load movie categories: \(error.localizedDescription)" }
    }

    private func loadMovies(provider: Provider, category: Category) async {
        do {
            movies = try await ProviderRepository.shared.loadMovies(provider, categoryId: category.id)
            level = .items
        } catch { errorMessage = "Failed to load movies: \(error.localizedDescription)" }
    }
}

struct SeriesView: View {
    @State private var categories: [Category] = []
    @State private var seriesList: [Series] = []
    @State private var selected: Series?
    @State private var level: BrowseLevel = .categories
    @State private var errorMessage: String?
    @State private var nowPlaying: Episode?

    var body: some View {
        NavigationView {
            content
                .navigationTitle(selected?.title ?? "Series")
                .task { await loadCategories() }
        }
        .sheet(item: $nowPlaying) { episode in
            PlayerView(streamUrl: episode.streamUrl, title: episode.title)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let provider = ProviderRepository.shared.listProviders().first {
            if let errorMessage { Text(errorMessage).foregroundColor(.red).padding() }
            else if let series = selected {
                List {
                    ForEach(series.seasons) { season in
                        Section("Season \(season.seasonNumber)") {
                            ForEach(season.episodes) { episode in
                                Button("\(episode.episodeNumber). \(episode.title)") { nowPlaying = episode }
                            }
                        }
                    }
                }
            } else if level == .categories {
                List(categories) { category in
                    Button(category.name) { Task { await loadSeries(provider: provider, category: category) } }
                }
            } else {
                List(seriesList) { series in
                    Button(series.title) { Task { await loadDetail(provider: provider, series: series) } }
                }
            }
        } else {
            Text("No IPTV provider configured yet. Go to Settings to add one.").padding()
        }
    }

    private func loadCategories() async {
        guard let provider = ProviderRepository.shared.listProviders().first else { return }
        do { categories = try await ProviderRepository.shared.loadSeriesCategories(provider) }
        catch { errorMessage = "Failed to load series categories: \(error.localizedDescription)" }
    }

    private func loadSeries(provider: Provider, category: Category) async {
        do {
            seriesList = try await ProviderRepository.shared.loadSeries(provider, categoryId: category.id)
            level = .items
        } catch { errorMessage = "Failed to load series: \(error.localizedDescription)" }
    }

    private func loadDetail(provider: Provider, series: Series) async {
        do { selected = try await ProviderRepository.shared.loadSeriesDetail(provider, series: series) }
        catch { errorMessage = "Failed to load series detail: \(error.localizedDescription)" }
    }
}
