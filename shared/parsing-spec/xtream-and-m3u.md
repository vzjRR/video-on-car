# Provider Parsing Spec

Both native codebases implement parsing exactly as specified here so
fixtures in `/tests/fixtures` produce identical normalized output.

## Xtream Codes

Base call pattern: `{baseUrl}/player_api.php?username=U&password=P&action=A[&...]`

| Action | Purpose | Maps to |
|---|---|---|
| (none, just user/pass) | Auth + account/server info | validate credentials, read `user_info.status`, `server_info` |
| `get_live_categories` | Live categories | `Category(contentType: live)` |
| `get_live_streams[&category_id=]` | Live channels | `LiveChannel` |
| `get_vod_categories` | Movie categories | `Category(contentType: movie)` |
| `get_vod_streams[&category_id=]` | Movies | `Movie` |
| `get_vod_info&vod_id=` | Movie detail (plot, duration, rating) | enrich `Movie` |
| `get_series_categories` | Series categories | `Category(contentType: series)` |
| `get_series[&category_id=]` | Series list | `Series` (seasons/episodes not yet loaded) |
| `get_series_info&series_id=` | Seasons + episodes | `Season`, `Episode` |
| `get_short_epg&stream_id=&limit=` | EPG for a channel | `EPGEvent` |

Stream URL construction:
- Live: `{baseUrl}/live/{username}/{password}/{stream_id}.{ext}` (`ext`
  from `output` format, default `m3u8` for HLS or `ts` for MPEG-TS).
- VOD: `{baseUrl}/movie/{username}/{password}/{stream_id}.{container_extension}`
- Episode: `{baseUrl}/series/{username}/{password}/{episode_id}.{container_extension}`

Rules:
- Never assume every field is present; `null`/missing → treat as unknown,
  do not crash.
- `container_extension` missing → default to `mp4`.
- Auth failure (`user_info.auth == 0` or HTTP 401/403) → surface as
  **Authentication failure** (Phase 14 category #2), distinct from a
  network error.
- Any JSON field expected to be a number but arriving as a numeric string
  (Xtream is inconsistent about this across servers) must be coerced
  leniently, never fail the whole parse for one field.

## M3U / M3U Plus

Parse line by line. An entry is:
```
#EXTINF:-1 tvg-id="..." tvg-name="..." tvg-logo="..." group-title="...",Display Name
https://server/path/to/stream
```

Rules:
- Do not assume attribute order or that all attributes are present.
- `group-title` → `Category.name` (create category on first sighting,
  content type inferred per the heuristic below).
- `tvg-id` → `LiveChannel.epgChannelId` / used to join EPG XMLTV if
  provided separately.
- `tvg-logo` → `posterUrl`/`logoUrl`.
- Catch-up: `catchup="..."`, `catchup-days="N"`, `catchup-source="..."`
  attributes when present → `catchupAvailable=true`, `catchupDays=N`.
- Content-type classification heuristic (M3U has no formal type field):
  1. `group-title` containing case-insensitive "movie" or "vod" → Movie.
  2. `group-title` containing "series" and the display name matching
     `S\d+E\d+` (season/episode pattern) → Episode, grouped into a
     synthetic Series by the part of the name before the `S\d+E\d+` token.
  3. URL path containing `/movie/` → Movie; `/series/` → Episode.
  4. Otherwise → Live.
  This is a best-effort heuristic; unclassifiable entries default to Live
  rather than being dropped, since dropping silently loses content the
  user paid for.
- Malformed lines (missing URL after `#EXTINF`, unparsable attributes) are
  skipped with a logged (non-credential) parse warning, not a hard failure
  of the whole playlist.
- Support both a local file path and a remote URL as the M3U source; a
  remote M3U URL may itself embed a username/password query — treat that
  URL as a credential and store it exactly like an Xtream password (never
  logged, never displayed raw after initial setup).

## Fixtures

See `/tests/fixtures/xtream/*.json` and `/tests/fixtures/m3u/*.m3u` for
sample inputs and `/tests/fixtures/expected/*.json` for the exact
normalized output both platforms must produce.
