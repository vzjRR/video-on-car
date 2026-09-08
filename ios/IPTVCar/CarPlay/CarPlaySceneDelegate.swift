import CarPlay
import AVFoundation

/// CarPlay entry point. Uses only the officially documented CarPlay
/// **Audio**-category templates (CPListTemplate) for browsing — see
/// docs/RESEARCH.md §2 and §5 for why: the CarPlay *video app* entitlement
/// exists in the API surface but is inert on every real vehicle today
/// (no automaker has enabled "video in car" over MFi yet), so a video
/// app entitlement would not render anything different on the Camry. The
/// Audio category, by contrast, is broadly obtainable and fully
/// functional today for exactly this browse-then-play structure:
///
///   IPTV
///   ├── Live TV   -> categories -> channels
///   ├── Movies    -> categories -> movies
///   └── Series    -> categories -> series -> seasons -> episodes
///
/// Selecting a leaf item starts playback through the shared
/// ExternalPlaybackRouter (Core/Playback/ExternalPlaybackRouter.swift),
/// which is configured for external/AirPlay playback so the actual video
/// can reach the Camry screen via whatever the Carlinkit/VehiConn adapter
/// exposes (see docs/RPLAYTV_BEHAVIOR.md — pending hardware confirmation).
class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate {

    var interfaceController: CPInterfaceController?
    private let router = ExternalPlaybackRouter()

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didConnect interfaceController: CPInterfaceController
    ) {
        self.interfaceController = interfaceController
        interfaceController.setRootTemplate(rootTemplate(), animated: true, completion: nil)
    }

    func templateApplicationScene(
        _ templateApplicationScene: CPTemplateApplicationScene,
        didDisconnectInterfaceController interfaceController: CPInterfaceController
    ) {
        self.interfaceController = nil
    }

    private func rootTemplate() -> CPListTemplate {
        guard ProviderRepository.shared.listProviders().first != nil else {
            let item = CPListItem(text: "No provider configured", detailText: "Add one in the iPhone app's Settings tab first.")
            return CPListTemplate(title: "IPTV Car", sections: [CPListSection(items: [item])])
        }

        let liveItem = CPListItem(text: "Live TV", detailText: nil)
        liveItem.handler = { [weak self] _, completion in
            self?.pushCategoryList(contentType: .live, title: "Live TV")
            completion()
        }

        let moviesItem = CPListItem(text: "Movies", detailText: nil)
        moviesItem.handler = { [weak self] _, completion in
            self?.pushCategoryList(contentType: .movie, title: "Movies")
            completion()
        }

        let seriesItem = CPListItem(text: "Series", detailText: nil)
        seriesItem.handler = { [weak self] _, completion in
            self?.pushCategoryList(contentType: .series, title: "Series")
            completion()
        }

        return CPListTemplate(title: "IPTV Car", sections: [CPListSection(items: [liveItem, moviesItem, seriesItem])])
    }

    private func pushCategoryList(contentType: ContentType, title: String) {
        guard let provider = ProviderRepository.shared.listProviders().first else { return }
        Task {
            do {
                let categories: [Category]
                switch contentType {
                case .live: categories = try await ProviderRepository.shared.loadLiveCategories(provider)
                case .movie: categories = try await ProviderRepository.shared.loadMovieCategories(provider)
                case .series: categories = try await ProviderRepository.shared.loadSeriesCategories(provider)
                }

                let items = categories.map { category -> CPListItem in
                    let item = CPListItem(text: category.name, detailText: nil)
                    item.handler = { [weak self] _, completion in
                        self?.pushContentList(provider: provider, contentType: contentType, category: category)
                        completion()
                    }
                    return item
                }

                let template = CPListTemplate(title: title, sections: [CPListSection(items: items)])
                interfaceController?.pushTemplate(template, animated: true, completion: nil)
            } catch {
                presentError("Failed to load \(title.lowercased()) categories", error)
            }
        }
    }

    private func pushContentList(provider: Provider, contentType: ContentType, category: Category) {
        Task {
            do {
                let items: [CPListItem]
                switch contentType {
                case .live:
                    let channels = try await ProviderRepository.shared.loadLiveChannels(provider, categoryId: category.id)
                    items = channels.map { channel in
                        let item = CPListItem(text: channel.name, detailText: nil)
                        item.handler = { [weak self] _, completion in
                            self?.play(streamUrl: channel.streamUrl, title: channel.name)
                            completion()
                        }
                        return item
                    }
                case .movie:
                    let movies = try await ProviderRepository.shared.loadMovies(provider, categoryId: category.id)
                    items = movies.map { movie in
                        let item = CPListItem(text: movie.title, detailText: movie.year.map(String.init))
                        item.handler = { [weak self] _, completion in
                            self?.play(streamUrl: movie.streamUrl, title: movie.title)
                            completion()
                        }
                        return item
                    }
                case .series:
                    let seriesList = try await ProviderRepository.shared.loadSeries(provider, categoryId: category.id)
                    items = seriesList.map { series in
                        let item = CPListItem(text: series.title, detailText: nil)
                        item.handler = { [weak self] _, completion in
                            self?.pushEpisodeList(provider: provider, series: series)
                            completion()
                        }
                        return item
                    }
                }

                let template = CPListTemplate(title: category.name, sections: [CPListSection(items: items)])
                interfaceController?.pushTemplate(template, animated: true, completion: nil)
            } catch {
                presentError("Failed to load \(category.name)", error)
            }
        }
    }

    private func pushEpisodeList(provider: Provider, series: Series) {
        Task {
            do {
                let detail = try await ProviderRepository.shared.loadSeriesDetail(provider, series: series)
                let sections = detail.seasons.map { season -> CPListSection in
                    let items = season.episodes.map { episode -> CPListItem in
                        let item = CPListItem(text: "\(episode.episodeNumber). \(episode.title)", detailText: nil)
                        item.handler = { [weak self] _, completion in
                            self?.play(streamUrl: episode.streamUrl, title: episode.title)
                            completion()
                        }
                        return item
                    }
                    return CPListSection(items: items, header: "Season \(season.seasonNumber)", sectionIndexTitle: nil)
                }
                let template = CPListTemplate(title: series.title, sections: sections)
                interfaceController?.pushTemplate(template, animated: true, completion: nil)
            } catch {
                presentError("Failed to load \(series.title)", error)
            }
        }
    }

    private func play(streamUrl: String, title: String) {
        router.play(streamUrl: streamUrl)
        let nowPlaying = CPNowPlayingTemplate.shared
        interfaceController?.pushTemplate(nowPlaying, animated: true, completion: nil)
    }

    private func presentError(_ message: String, _ error: Error) {
        let alert = CPAlertTemplate(titleVariants: ["\(message): \(error.localizedDescription)"], actions: [
            CPAlertAction(title: "OK", style: .default) { [weak self] _ in
                self?.interfaceController?.dismissTemplate(animated: true, completion: nil)
            }
        ])
        interfaceController?.presentTemplate(alert, animated: true, completion: nil)
    }
}
