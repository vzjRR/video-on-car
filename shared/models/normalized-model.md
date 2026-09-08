# Normalized IPTV Model

Both `/ios` and `/android` implement these exact fields. `?` = optional.

## Provider
- `id: string` (local UUID, not the server URL)
- `displayName: string`
- `kind: "xtream" | "m3u"`
- `baseUrl: string` (Xtream server URL, or the M3U URL/file reference — never shown raw in UI after setup)
- `createdAt: timestamp`

Credentials (`username`, `password`, raw M3U URL if it embeds auth) are
stored **separately** in secure storage, keyed by `Provider.id`, and are
never part of this model object once loaded into memory for UI use.

## Category
- `id: string`
- `providerId: string`
- `name: string`
- `contentType: "live" | "movie" | "series"`
- `parentId: string?` (for nested categories, rare)

## LiveChannel
- `id: string`
- `providerId: string`
- `categoryId: string`
- `name: string`
- `logoUrl: string?`
- `streamUrl: string`
- `epgChannelId: string?` (`tvg-id` for M3U, `epg_channel_id` for Xtream)
- `catchupAvailable: bool`
- `catchupDays: int?`

## Movie
- `id: string`
- `providerId: string`
- `categoryId: string`
- `title: string`
- `year: int?`
- `posterUrl: string?`
- `durationSeconds: int?`
- `description: string?`
- `genre: string?`
- `rating: double?`
- `streamUrl: string`
- `containerExtension: string?` (Xtream: `.mp4`, `.mkv`, etc.)

## Series
- `id: string`
- `providerId: string`
- `categoryId: string`
- `title: string`
- `posterUrl: string?`
- `description: string?`
- `genre: string?`
- `rating: double?`
- `seasons: [Season]` (loaded lazily)

## Season
- `id: string`
- `seriesId: string`
- `seasonNumber: int`
- `episodes: [Episode]` (loaded lazily)

## Episode
- `id: string`
- `seasonId: string`
- `episodeNumber: int`
- `title: string`
- `posterUrl: string?`
- `durationSeconds: int?`
- `description: string?`
- `streamUrl: string`
- `containerExtension: string?`

## EPGEvent
- `id: string`
- `channelId: string`
- `title: string`
- `description: string?`
- `startTime: timestamp`
- `endTime: timestamp`

## ResumeState (local only, never sent to provider)
- `key: (providerId, contentType, contentId)`
- `positionMs: long`
- `durationMs: long`
- `updatedAt: timestamp`

## Favorite (local only)
- `providerId: string`
- `contentType: "live" | "movie" | "series"`
- `contentId: string`
- `addedAt: timestamp`
