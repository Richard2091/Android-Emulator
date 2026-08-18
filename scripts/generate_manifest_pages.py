#!/usr/bin/env python3
"""Generate Retro Hall static catalog files."""

from __future__ import annotations

import argparse
import hashlib
import json
import html
import mimetypes
import shutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import quote, urlparse
from urllib.request import Request, urlopen


SORT_ALIASES = {
    "id": "id",
    "name": "name",
    "title": "name",
    "hotness": "hotness",
    "hot": "hotness",
    "popularity": "hotness",
    "popular": "hotness",
    "releasedate": "releaseDate",
    "date": "releaseDate",
    "newest": "releaseDate",
    "published": "releaseDate",
}


def parse_sort(value: str) -> str:
    key = value.lower()
    if key not in SORT_ALIASES:
        raise argparse.ArgumentTypeError(f"unknown sort key: {value}")
    return SORT_ALIASES[key]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--games-dir", default="games")
    parser.add_argument("--output-dir", default="public")
    parser.add_argument("--base-url", default="")
    parser.add_argument(
        "--sort",
        default="id",
        type=parse_sort,
        help="生成时的默认排序：id / name / hotness / releaseDate（别名：title、hot、popularity、date、newest），默认 id",
    )
    args = parser.parse_args()

    root = Path.cwd()
    games_dir = (root / args.games_dir).resolve()
    output_dir = (root / args.output_dir).resolve()
    base_url = args.base_url.rstrip("/")
    sort = args.sort

    output_dir.mkdir(parents=True, exist_ok=True)
    games = load_games(games_dir=games_dir, output_dir=output_dir, base_url=base_url, sort=sort)

    manifest = {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "games": games,
    }
    search_index = {
        "schemaVersion": 1,
        "generatedAt": manifest["generatedAt"],
        "games": [to_search_item(game) for game in games],
    }

    write_json(output_dir / "manifest.v1.json", manifest)
    write_json(output_dir / "search-index.v1.json", search_index)
    write_index_html(
        output_dir / "index.html",
        manifest=manifest,
        search_index=search_index,
        base_url=base_url,
        sort=sort,
    )

    if games_dir.exists():
        target_games_dir = (output_dir / "games").resolve()
        if target_games_dir == games_dir or target_games_dir.is_relative_to(games_dir):
            raise ValueError("output games directory must not be the source games directory or a child of it")
        if target_games_dir.exists():
            shutil.rmtree(target_games_dir)
        shutil.copytree(games_dir, target_games_dir, ignore=shutil.ignore_patterns("game.json"))


def load_games(games_dir: Path, output_dir: Path, base_url: str, sort: str = "id") -> list[dict[str, Any]]:
    if not games_dir.exists():
        return []

    image_source_cache: dict[str, str] = {}
    image_digest_cache: dict[str, str] = {}
    games: list[dict[str, Any]] = []
    for game_json in sorted(games_dir.glob("*/game.json")):
        raw = json.loads(game_json.read_text(encoding="utf-8-sig"))
        game_dir = game_json.parent
        slug = game_dir.name
        games.append(
            normalize_game(
                raw=raw,
                slug=slug,
                game_dir=game_dir,
                output_dir=output_dir,
                base_url=base_url,
                image_source_cache=image_source_cache,
                image_digest_cache=image_digest_cache,
            ),
        )
    games.sort(key=lambda item: sort_key(item, sort))
    return games


def sort_key(game: dict[str, Any], sort: str) -> tuple[int, Any]:
    if sort == "name":
        name = title_text(game.get("displayTitle") or game.get("title") or game.get("id")).casefold()
        return 0, name
    if sort == "hotness":
        hot = parse_float(game.get("hotness"))
        if hot is None:
            return 1, 0.0
        return 0, -hot
    if sort == "releaseDate":
        stamp = parse_date_stamp(game.get("releaseDate"))
        if stamp is None:
            return 1, 0.0
        return 0, -stamp
    return 0, str(game.get("id"))


def parse_float(value: Any) -> float | None:
    try:
        if value is None or str(value).strip() == "":
            return None
        return float(value)
    except (TypeError, ValueError):
        return None


def parse_date_stamp(value: Any) -> float | None:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    try:
        dt = datetime.fromisoformat(text.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.timestamp()
    except ValueError:
        pass
    return parse_float(text)


def normalize_game(
    raw: dict[str, Any],
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
) -> dict[str, Any]:
    game_id = str(raw.get("id") or slug)
    title = raw.get("title") or raw.get("displayTitle") or game_id
    description = title_text(raw.get("description", ""))
    assets = raw.get("assets") or {}
    rom_items = raw.get("roms")
    if rom_items is None:
        rom_value = raw.get("rom") or raw.get("romPath") or raw.get("url") or ""
        rom_items = [{"path": rom_value}] if rom_value else []

    normalized_roms = [
        normalize_rom(rom=rom, slug=slug, game_dir=game_dir, base_url=base_url)
        for rom in rom_items
        if isinstance(rom, dict)
    ]
    cover_url = resolve_cover_url(
        raw=raw,
        title=title,
        slug=slug,
        game_dir=game_dir,
        output_dir=output_dir,
        base_url=base_url,
        roms=normalized_roms,
        assets=assets,
        image_source_cache=image_source_cache,
        image_digest_cache=image_digest_cache,
    )
    screenshot_urls = resolve_image_group(
        raw=raw,
        slug=slug,
        title=title,
        roms=normalized_roms,
        game_dir=game_dir,
        output_dir=output_dir,
        base_url=base_url,
        image_source_cache=image_source_cache,
        image_digest_cache=image_digest_cache,
        group="screenshots",
        fallback_kinds=("Named_Snaps", "Named_Boxarts", "Named_Logos"),
        fallback_sources=(
            raw.get("screenshots"),
            raw.get("screenshotUrls"),
            assets.get("screenshots"),
            assets.get("screenshotUrls"),
            assets.get("screenshotSources"),
        ),
    )
    logo_urls = resolve_image_group(
        raw=raw,
        slug=slug,
        title=title,
        roms=normalized_roms,
        game_dir=game_dir,
        output_dir=output_dir,
        base_url=base_url,
        image_source_cache=image_source_cache,
        image_digest_cache=image_digest_cache,
        group="logos",
        fallback_kinds=("Named_Logos", "Named_Titles"),
        fallback_sources=(
            raw.get("logos"),
            raw.get("logoUrls"),
            assets.get("logos"),
            assets.get("logoUrls"),
            assets.get("logoSources"),
        ),
    )
    screenshot_urls = [url for url in screenshot_urls if url and url != cover_url]
    logo_seen = set(screenshot_urls)
    if cover_url:
        logo_seen.add(cover_url)
    logo_urls = [url for url in logo_urls if url and url not in logo_seen]

    item: dict[str, Any] = {
        "id": game_id,
        "title": title,
        "displayTitle": raw.get("displayTitle") or title_text(title),
        "platform": raw.get("platform") or "FC/NES",
        "category": raw.get("category") or "在线游戏库",
        "description": description,
        "hotness": parse_float(raw.get("hotness") or raw.get("popularity")),
        "releaseDate": title_text(raw.get("releaseDate") or raw.get("publishedAt") or ""),
        "assets": {
            "coverUrl": cover_url,
            "screenshots": screenshot_urls,
            "logoUrls": logo_urls,
        },
        "roms": normalized_roms,
    }
    return item


def normalize_rom(rom: dict[str, Any], slug: str, game_dir: Path, base_url: str) -> dict[str, Any]:
    source = str(rom.get("url") or rom.get("path") or "")
    resolved = resolve_url(source, slug=slug, base_url=base_url) if source else ""
    hashes = dict(rom.get("hashes") or {})
    local_path = game_dir / source if source and not is_url(source) else None
    if local_path is not None and local_path.exists() and local_path.is_file():
        hashes = {**file_hashes(local_path), **hashes}

    return {
        "name": rom.get("name") or Path(source).name or f"{slug}.nes",
        "url": resolved,
        "path": source,
        "size": rom.get("size") or (local_path.stat().st_size if local_path is not None and local_path.exists() else None),
        "hashes": {key: value for key, value in hashes.items() if value},
    }


def resolve_url(value: str, slug: str, base_url: str) -> str:
    if is_url(value) or not base_url:
        return value
    clean = value.replace("\\", "/").lstrip("/")
    if not clean.startswith("games/"):
        clean = f"games/{slug}/{clean}"
    return f"{base_url}/{clean}"


def is_url(value: str) -> bool:
    return value.startswith("http://") or value.startswith("https://")


def resolve_cover_url(
    raw: dict[str, Any],
    title: str,
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    roms: list[dict[str, Any]],
    assets: dict[str, Any],
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
) -> str:
    sources = collect_sources(
        raw.get("cover"),
        assets.get("cover"),
        assets.get("coverUrl"),
        assets.get("coverSources"),
        assets.get("coverUrls"),
    )
    playlist = thumbnail_playlist(str(raw.get("platform") or "FC/NES"))
    sources.extend(
        thumbnail_sources(
            playlist=playlist,
            kinds=("Named_Boxarts", "Named_Logos"),
            raw=raw,
            title=title,
            slug=slug,
            roms=roms,
        )
    )
    return first_materialized_image(
        sources=sources,
        slug=slug,
        game_dir=game_dir,
        output_dir=output_dir,
        base_url=base_url,
        image_source_cache=image_source_cache,
        image_digest_cache=image_digest_cache,
        folder="covers",
    )


def resolve_image_group(
    raw: dict[str, Any],
    slug: str,
    title: str,
    roms: list[dict[str, Any]],
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
    group: str,
    fallback_kinds: tuple[str, ...],
    fallback_sources: tuple[Any, ...],
) -> list[str]:
    sources = collect_sources(*fallback_sources)
    playlist = thumbnail_playlist(str(raw.get("platform") or "FC/NES"))
    sources.extend(
        thumbnail_sources(
            playlist=playlist,
            kinds=fallback_kinds,
            raw=raw,
            title=title,
            slug=slug,
            roms=roms,
        )
    )
    return materialize_image_group(
        sources=sources,
        slug=slug,
        game_dir=game_dir,
        output_dir=output_dir,
        base_url=base_url,
        image_source_cache=image_source_cache,
        image_digest_cache=image_digest_cache,
        folder=group,
    )


def thumbnail_sources(
    playlist: str,
    kinds: tuple[str, ...],
    raw: dict[str, Any],
    title: str,
    slug: str,
    roms: list[dict[str, Any]],
) -> list[str]:
    if not playlist:
        return []
    candidates = [
        first_non_blank(
            roms[0].get("name") if roms else "",
            Path(str(raw.get("rom") or raw.get("romPath") or "")).stem,
        ),
        first_non_blank(raw.get("displayTitle"), title),
        slug,
    ]
    urls: list[str] = []
    seen: set[str] = set()
    for candidate in candidates:
        if not candidate:
            continue
        for kind in kinds:
            source = thumbnail_url(playlist, kind, candidate)
            if source not in seen:
                seen.add(source)
                urls.append(source)
    return urls


def collect_sources(*values: Any) -> list[str]:
    sources: list[str] = []
    seen: set[str] = set()
    for value in values:
        for source in flatten_sources(value):
            source = title_text(source).strip()
            if not source or source.lower() == "null" or source in seen:
                continue
            seen.add(source)
            sources.append(source)
    return sources


def flatten_sources(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        return [value]
    if isinstance(value, dict):
        items: list[str] = []
        for key in ("url", "path", "source"):
            if key in value:
                items.extend(flatten_sources(value.get(key)))
        return items
    if isinstance(value, (list, tuple, set)):
        items: list[str] = []
        for item in value:
            items.extend(flatten_sources(item))
        return items
    return [str(value)]


def materialize_image_group(
    sources: list[str],
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
    folder: str,
) -> list[str]:
    urls: list[str] = []
    for source in sources:
        materialized = materialize_image(
            source=source,
            slug=slug,
            game_dir=game_dir,
            output_dir=output_dir,
            base_url=base_url,
            image_source_cache=image_source_cache,
            image_digest_cache=image_digest_cache,
            folder=folder,
        )
        if materialized and materialized not in urls:
            urls.append(materialized)
    return urls


def first_materialized_image(
    sources: list[str],
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
    folder: str,
) -> str:
    for source in sources:
        materialized = materialize_image(
            source=source,
            slug=slug,
            game_dir=game_dir,
            output_dir=output_dir,
            base_url=base_url,
            image_source_cache=image_source_cache,
            image_digest_cache=image_digest_cache,
            folder=folder,
        )
        if materialized:
            return materialized
    return ""


def first_non_blank(*values: Any) -> str:
    for value in values:
        text = title_text(value).strip()
        if text:
            return text
    return ""


def thumbnail_playlist(platform: str) -> str:
    key = platform.strip().lower()
    aliases = {
        "fc": "Nintendo - Nintendo Entertainment System",
        "fc/nes": "Nintendo - Nintendo Entertainment System",
        "famicom": "Nintendo - Nintendo Entertainment System",
        "nes": "Nintendo - Nintendo Entertainment System",
        "nintendo entertainment system": "Nintendo - Nintendo Entertainment System",
        "nintendo - nintendo entertainment system": "Nintendo - Nintendo Entertainment System",
    }
    return aliases.get(key, platform.strip())


def thumbnail_url(playlist: str, kind: str, game_name: str) -> str:
    playlist_part = quote(playlist, safe="")
    kind_part = quote(kind, safe="")
    game_part = quote(game_name, safe="")
    return f"https://thumbnails.libretro.com/{playlist_part}/{kind_part}/{game_part}.png"


def materialize_image(
    source: str,
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    image_source_cache: dict[str, str],
    image_digest_cache: dict[str, str],
    folder: str,
) -> str:
    if not source:
        return ""
    cached = image_source_cache.get(source)
    if cached:
        cached_path = output_dir / cached
        if cached_path.exists() and cached_path.stat().st_size > 0:
            return cover_asset_url(cached, base_url)
        image_source_cache.pop(source, None)

    target_dir = output_dir / folder
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / image_file_name(source, slug, game_dir)
    if source.startswith("http://") or source.startswith("https://"):
        if not target_path.exists() or target_path.stat().st_size == 0:
            if not download_url(source, target_path):
                return ""
    else:
        local_path = resolve_local_path(source, game_dir)
        if local_path is None or not local_path.exists() or not local_path.is_file():
            return ""
        if not target_path.exists() or target_path.stat().st_size == 0:
            shutil.copyfile(local_path, target_path)

    if target_path.exists() and target_path.stat().st_size > 0:
        relative_path = target_path.relative_to(output_dir).as_posix()
        digest = image_digest(target_path)
        existing = image_digest_cache.get(digest)
        if existing and existing != relative_path:
            try:
                target_path.unlink()
            except FileNotFoundError:
                pass
            image_source_cache[source] = existing
            return cover_asset_url(existing, base_url)
        image_digest_cache[digest] = relative_path
        image_source_cache[source] = relative_path
        return cover_asset_url(relative_path, base_url)
    return ""


def image_file_name(source: str, slug: str, game_dir: Path) -> str:
    suffix = Path(urlparse(source).path).suffix
    if not suffix:
        local_path = resolve_local_path(source, game_dir)
        if local_path is not None:
            suffix = local_path.suffix
    if not suffix:
        suffix = ".png"
    digest = hashlib.sha1(source.encode("utf-8")).hexdigest()[:16]
    return f"{slug}-{digest}{suffix}"


def image_digest(path: Path) -> str:
    return hashlib.sha1(path.read_bytes()).hexdigest()


def resolve_local_path(source: str, game_dir: Path) -> Path | None:
    if not source:
        return None
    path = Path(source)
    if path.is_absolute():
        return path
    return (game_dir / path).resolve()


def download_url(source: str, target_path: Path) -> bool:
    try:
        request = Request(source, headers={"User-Agent": "RetroHall"})
        with urlopen(request, timeout=30) as response:
            content_type = response.headers.get_content_type()
            data = response.read()
    except Exception:
        return False

    if not data:
        return False

    if target_path.suffix == "":
        guessed = mimetypes.guess_extension(content_type) or ".png"
        target_path = target_path.with_suffix(guessed)

    target_path.parent.mkdir(parents=True, exist_ok=True)
    target_path.write_bytes(data)
    return True


def cover_asset_url(relative_path: str, base_url: str) -> str:
    if not relative_path:
        return ""
    if base_url:
        return f"{base_url}/{relative_path.lstrip('/')}"
    return relative_path


def file_hashes(path: Path) -> dict[str, str]:
    data = path.read_bytes()
    body = data[16:] if path.suffix.lower() == ".nes" and len(data) > 16 and data[:4] == b"NES\x1a" else data
    return {
        "md5": hashlib.md5(body).hexdigest(),
        "sha1": hashlib.sha1(body).hexdigest(),
        "sha256": hashlib.sha256(body).hexdigest(),
        "crc32": f"{__import__('zlib').crc32(body) & 0xFFFFFFFF:08X}",
    }


def title_text(value: Any) -> str:
    if isinstance(value, dict):
        return str(value.get("zh") or value.get("en") or next(iter(value.values()), ""))
    return str(value)


def to_search_item(game: dict[str, Any]) -> dict[str, Any]:
    title = game.get("title")
    description = game.get("description")
    return {
        "id": game["id"],
        "title": title,
        "displayTitle": game.get("displayTitle") or title_text(title),
        "platform": game.get("platform", ""),
        "category": game.get("category", ""),
        "description": description,
        "keywords": [
            text
            for text in [
                title_text(title),
                game.get("platform", ""),
                game.get("category", ""),
                title_text(description),
            ]
            if text
        ],
    }


def write_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2, sort_keys=False) + "\n",
        encoding="utf-8",
    )


def write_index_html(
    path: Path,
    manifest: dict[str, Any],
    search_index: dict[str, Any],
    base_url: str,
    sort: str = "id",
) -> None:
    games = list(manifest.get("games", []))
    search_lookup = {
        str(item.get("id")): item
        for item in search_index.get("games", [])
        if isinstance(item, dict) and item.get("id") is not None
    }
    generated_at = title_text(manifest.get("generatedAt", ""))
    game_count = len(games)
    base_href = f"{base_url}/" if base_url else ""
    sort_label = {
        "id": "编号",
        "name": "名称",
        "hotness": "热度",
        "releaseDate": "发布时间",
    }.get(sort, "编号")
    initial_sort = {"name": "title", "hotness": "hotness", "releaseDate": "date"}.get(sort, "id")

    def format_time_stamp(value: Any) -> str:
        text = title_text(value).strip()
        if not text:
            return "未知"
        try:
            dt = datetime.fromisoformat(text.replace("Z", "+00:00"))
        except ValueError:
            return text
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M")

    def format_date_label(value: Any) -> str:
        stamp = parse_date_stamp(value)
        if stamp is None:
            return "未填写"
        return datetime.fromtimestamp(stamp, tz=timezone.utc).strftime("%Y-%m-%d")

    def fallback_colors(seed: str) -> tuple[str, str]:
        digest = hashlib.sha1(seed.encode("utf-8")).hexdigest()
        first = int(digest[:2], 16) % 360
        second = (first + 42 + int(digest[2:4], 16) % 24) % 360
        return f"hsl({first} 58% 24%)", f"hsl({second} 64% 38%)"

    def render_action_link(label: str, href: str, extra_class: str = "") -> str:
        if not href:
            return f'<span class="action-link disabled{extra_class}">{html.escape(label)}</span>'
        return (
            f'<a class="action-link{extra_class}" href="{html.escape(href)}" target="_blank" rel="noreferrer">'
            f"{html.escape(label)}</a>"
        )

    metric_items = [
        ("总条目", str(game_count), "当前可浏览游戏数量"),
        ("分类", str(len({title_text(game.get("category", "")).strip() for game in games if title_text(game.get("category", "")).strip()})), "按分类快速筛选"),
        ("平台", str(len({title_text(game.get("platform", "")).strip() for game in games if title_text(game.get("platform", "")).strip()})), "当前索引覆盖平台"),
        ("带封面", str(sum(1 for game in games if title_text(game.get("assets", {}).get("coverUrl", "")).strip())), "封面可直接预览"),
    ]
    metrics_html = "\n".join(
        f"""
        <article class="stat-card">
          <strong>{html.escape(value)}</strong>
          <span>{html.escape(label)}</span>
          <small>{html.escape(note)}</small>
        </article>
        """
        for label, value, note in metric_items
    )

    cards_html = []
    for index, game in enumerate(games):
        game_id = title_text(game.get("id") or f"{index + 1}")
        title_raw = title_text(game.get("displayTitle") or game.get("title") or game_id).strip()
        category_raw = title_text(game.get("category", "")).strip() or "未分类"
        platform_raw = title_text(game.get("platform", "")).strip() or "未指定"
        description_raw = title_text(game.get("description", "")).strip() or "暂无简介"
        cover_url = title_text(game.get("assets", {}).get("coverUrl", "")).strip()
        search_item = search_lookup.get(game_id, {})
        search_terms = " ".join(
            [
                title_raw,
                category_raw,
                platform_raw,
                description_raw,
                game_id,
                " ".join(str(keyword) for keyword in (search_item.get("keywords") or []) if keyword),
            ]
        ).strip()
        hotness = game.get("hotness")
        hotness_text = f"{hotness:g}" if hotness is not None else "未提供"
        hotness_sort = f"{hotness:g}" if hotness is not None else ""
        release_text = title_text(game.get("releaseDate") or "")
        release_sort = ""
        if release_text:
            stamp = parse_date_stamp(release_text)
            if stamp is not None:
                release_sort = f"{stamp:g}"
        rom_url = ""
        roms = game.get("roms") or []
        if roms:
            first_rom = roms[0]
            rom_url = title_text(first_rom.get("url") or first_rom.get("path") or "").strip()

        cover_alt = html.escape(f"{title_raw} 封面")
        if cover_url:
            cover_inner = (
                f'<img class="card-cover-image" src="{html.escape(cover_url)}" alt="{cover_alt}" '
                'loading="lazy" decoding="async" />'
            )
        else:
            tone_a, tone_b = fallback_colors(game_id or title_raw)
            short_title = html.escape((title_raw[:2] or "游戏"))
            cover_inner = (
                f'<div class="card-cover-fallback" style="--tone-a:{tone_a};--tone-b:{tone_b}">'
                f"<strong>{short_title}</strong>"
                "<span>暂无封面</span>"
                "</div>"
            )

        badge_label = category_raw if category_raw != "未分类" else platform_raw
        if hotness is not None and hotness >= 9:
            badge_text = "热门"
        elif release_sort:
            badge_text = "新上"
        else:
            badge_text = "条目"

        cards_html.append(
            f"""
            <article class="game-card"
              data-game-card
              data-id="{html.escape(game_id.casefold())}"
              data-title="{html.escape(title_raw.casefold())}"
              data-search="{html.escape(search_terms.casefold())}"
              data-hotness="{html.escape(hotness_sort)}"
              data-date="{html.escape(release_sort)}"
              data-index="{index}"
            >
              <div class="card-cover">
                {cover_inner}
                <div class="card-cover-mask"></div>
                <div class="card-cover-badges">
                  <span class="chip chip-soft">{html.escape(badge_label)}</span>
                  <span class="chip chip-strong">{html.escape(platform_raw)}</span>
                </div>
                <div class="card-cover-tag">{html.escape(badge_text)}</div>
              </div>
              <div class="card-body">
                <div class="card-title-row">
                  <div>
                    <h3>{html.escape(title_raw)}</h3>
                    <p>{html.escape(description_raw)}</p>
                  </div>
                  <span class="card-index">#{index + 1:02d}</span>
                </div>
                <div class="card-meta">
                  <span>热度 {html.escape(hotness_text)}</span>
                  <span>发布日期 {html.escape(format_date_label(release_text))}</span>
                </div>
                <div class="card-actions">
                  {render_action_link("查看封面", cover_url, " cover")}
                  {render_action_link("打开资源", rom_url, " resource")}
                </div>
              </div>
            </article>
            """
        )

    cards_html_text = "\n".join(cards_html) if cards_html else ""
    empty_html = (
        '<div class="empty-state" id="emptyState" hidden>'
        '<strong>没有找到匹配的条目</strong>'
        '<span>换一个关键词，或者切回默认排序再看一次。</span>'
        "</div>"
    )

    sort_script = """
    <script>
    (function () {
      "use strict";
      var grid = document.getElementById("gameGrid");
      if (!grid) return;
      var cards = Array.prototype.slice.call(grid.querySelectorAll("[data-game-card]"));
      var searchInput = document.getElementById("searchInput");
      var emptyState = document.getElementById("emptyState");
      var visibleCount = document.getElementById("visibleCount");
      var sortLabel = document.getElementById("currentSortLabel");
      var buttons = Array.prototype.slice.call(document.querySelectorAll("[data-sort]"));
      var currentSort = "__ACTIVE__";
      var collator = typeof Intl !== "undefined" && typeof Intl.Collator === "function"
        ? new Intl.Collator("zh-Hans-CN", { sensitivity: "base", numeric: true })
        : null;

      function normalize(text) {
        return String(text || "").toLowerCase().replace(/\\s+/g, "");
      }

      function compareText(a, b) {
        if (collator) return collator.compare(a, b);
        return a < b ? -1 : a > b ? 1 : 0;
      }

      function compareCard(a, b, key) {
        if (key === "title" || key === "id") {
          return compareText(a.dataset[key] || "", b.dataset[key] || "");
        }
        if (key === "hotness") {
          var av = parseFloat(a.dataset.hotness || "");
          var bv = parseFloat(b.dataset.hotness || "");
          if (isNaN(av)) av = Number.NEGATIVE_INFINITY;
          if (isNaN(bv)) bv = Number.NEGATIVE_INFINITY;
          if (av === bv) return 0;
          return av > bv ? -1 : 1;
        }
        if (key === "date") {
          var ad = parseFloat(a.dataset.date || "");
          var bd = parseFloat(b.dataset.date || "");
          if (isNaN(ad)) ad = Number.NEGATIVE_INFINITY;
          if (isNaN(bd)) bd = Number.NEGATIVE_INFINITY;
          if (ad === bd) return 0;
          return ad > bd ? -1 : 1;
        }
        return 0;
      }

      function setActiveSort(key) {
        currentSort = key;
        buttons.forEach(function (button) {
          var active = button.getAttribute("data-sort") === key;
          button.classList.toggle("active", active);
          button.setAttribute("aria-pressed", active ? "true" : "false");
        });
        if (sortLabel) {
          var label = {
            id: "编号",
            title: "名称",
            hotness: "热度",
            date: "最新"
          }[key] || "编号";
          sortLabel.textContent = label;
        }
      }

      function applyState() {
        var query = normalize(searchInput ? searchInput.value : "");
        var visibleCards = cards.filter(function (card) {
          return !query || normalize(card.dataset.search || "").indexOf(query) !== -1;
        });
        visibleCards.sort(function (a, b) { return compareCard(a, b, currentSort); });
        visibleCards.forEach(function (card) {
          card.hidden = false;
          grid.appendChild(card);
        });
        cards.forEach(function (card) {
          card.hidden = visibleCards.indexOf(card) === -1;
        });
        if (visibleCount) {
          visibleCount.textContent = String(visibleCards.length);
        }
        if (emptyState) {
          emptyState.hidden = visibleCards.length !== 0;
        }
      }

      buttons.forEach(function (button) {
        button.addEventListener("click", function () {
          setActiveSort(button.getAttribute("data-sort"));
          applyState();
        });
      });

      if (searchInput) {
        searchInput.addEventListener("input", applyState);
      }

      setActiveSort(currentSort);
      applyState();
    })();
    </script>
""".replace("__ACTIVE__", initial_sort)

    index_html = f"""<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="robots" content="noindex" />
    <title>FC_ROMS 游戏库首页</title>
    <style>
      :root {{
        color-scheme: dark;
        --bg: #071018;
        --panel: #0c1620;
        --panel-2: #101d29;
        --text: #edf5ff;
        --muted: #94a9c2;
        --line: rgba(134, 160, 190, 0.22);
        --line-strong: rgba(148, 188, 225, 0.36);
        --accent: #71e6c1;
        --accent-2: #74a4ff;
        --warm: #ffbf67;
        --danger: #ff8585;
      }}
      * {{ box-sizing: border-box; }}
      html, body {{
        margin: 0;
        min-height: 100%;
      }}
      body {{
        background:
          linear-gradient(180deg, #061018 0%, #04070b 100%);
        color: var(--text);
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
        line-height: 1.55;
      }}
      body::before {{
        content: "";
        position: fixed;
        inset: 0;
        pointer-events: none;
        background-image:
          linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
          linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
        background-size: 44px 44px;
        mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.65), transparent 92%);
        opacity: 0.25;
      }}
      a {{
        color: inherit;
      }}
      .shell {{
        max-width: 1260px;
        margin: 0 auto;
        padding: 28px 18px 40px;
        display: grid;
        gap: 16px;
        position: relative;
        z-index: 1;
      }}
      .hero {{
        display: grid;
        grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
        gap: 22px;
        padding: 26px;
        border: 1px solid var(--line);
        border-radius: 20px;
        background:
          linear-gradient(135deg, rgba(114, 230, 193, 0.08), transparent 34%),
          linear-gradient(180deg, rgba(16, 29, 41, 0.96), rgba(10, 17, 25, 0.96));
        box-shadow: 0 22px 50px rgba(0, 0, 0, 0.22);
      }}
      .eyebrow {{
        margin: 0 0 10px;
        color: var(--accent);
        font-size: 13px;
        font-weight: 800;
        letter-spacing: 0;
        text-transform: none;
      }}
      h1 {{
        margin: 0;
        font-size: 34px;
        line-height: 1.1;
        letter-spacing: 0;
      }}
      .hero p {{
        margin: 14px 0 0;
        max-width: 62ch;
        color: var(--muted);
        font-size: 15px;
      }}
      .hero-meta {{
        margin-top: 16px;
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }}
      .hero-chip {{
        display: inline-flex;
        align-items: center;
        min-height: 32px;
        padding: 0 12px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: rgba(255, 255, 255, 0.03);
        color: var(--text);
        font-size: 13px;
        font-weight: 700;
      }}
      .hero-links {{
        margin-top: 18px;
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
      }}
      .hero-links a,
      .action-link {{
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: 40px;
        padding: 0 14px;
        border-radius: 12px;
        border: 1px solid var(--line-strong);
        background: rgba(255, 255, 255, 0.03);
        text-decoration: none;
        font-size: 14px;
        font-weight: 700;
        transition: transform 120ms ease, border-color 120ms ease, background 120ms ease;
      }}
      .hero-links a:hover,
      .action-link:hover {{
        transform: translateY(-1px);
        border-color: rgba(113, 230, 193, 0.6);
        background: rgba(113, 230, 193, 0.08);
      }}
      .summary-grid {{
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
      }}
      .stat-card {{
        padding: 16px;
        border-radius: 16px;
        border: 1px solid var(--line);
        background: rgba(255, 255, 255, 0.03);
        display: grid;
        align-content: start;
        gap: 8px;
      }}
      .stat-card strong {{
        font-size: 30px;
        line-height: 1;
      }}
      .stat-card span {{
        color: var(--muted);
        font-size: 14px;
      }}
      .stat-card small {{
        color: var(--muted);
        font-size: 12px;
      }}
      .toolbar {{
        display: grid;
        grid-template-columns: minmax(0, 1fr) auto auto;
        gap: 12px;
        align-items: center;
        padding: 16px 18px;
        border: 1px solid var(--line);
        border-radius: 18px;
        background: rgba(16, 29, 41, 0.9);
      }}
      .search-field {{
        display: grid;
        grid-template-columns: auto minmax(0, 1fr);
        align-items: center;
        gap: 12px;
        min-width: 0;
        padding: 0 14px;
        border-radius: 14px;
        border: 1px solid var(--line);
        background: rgba(255, 255, 255, 0.03);
      }}
      .search-field span {{
        color: var(--muted);
        font-size: 13px;
        font-weight: 700;
      }}
      .search-field input {{
        min-width: 0;
        width: 100%;
        height: 46px;
        border: 0;
        outline: 0;
        background: transparent;
        color: var(--text);
        font: inherit;
        font-size: 15px;
      }}
      .search-field input::placeholder {{
        color: rgba(148, 169, 194, 0.8);
      }}
      .sort-group {{
        display: inline-flex;
        flex-wrap: wrap;
        gap: 8px;
      }}
      .sort-button {{
        min-height: 40px;
        padding: 0 13px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: rgba(255, 255, 255, 0.03);
        color: var(--text);
        font: inherit;
        font-size: 14px;
        font-weight: 700;
        cursor: pointer;
      }}
      .sort-button.active {{
        border-color: rgba(113, 230, 193, 0.8);
        background: rgba(113, 230, 193, 0.12);
      }}
      .result-pill {{
        display: inline-flex;
        align-items: center;
        gap: 8px;
        min-height: 40px;
        padding: 0 14px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: rgba(255, 255, 255, 0.03);
        color: var(--muted);
        font-size: 13px;
        font-weight: 700;
      }}
      .result-pill strong {{
        color: var(--text);
        font-size: 16px;
      }}
      .library {{
        padding: 0;
      }}
      .library-head {{
        display: flex;
        justify-content: space-between;
        gap: 14px;
        align-items: end;
        padding: 20px 22px 0;
      }}
      .library-head h2 {{
        margin: 0;
        font-size: 20px;
        line-height: 1.2;
      }}
      .library-head p {{
        margin: 6px 0 0;
        color: var(--muted);
        font-size: 13px;
      }}
      .library-grid {{
        padding: 18px 22px 22px;
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 16px;
      }}
      .game-card {{
        min-width: 0;
        overflow: hidden;
        border-radius: 18px;
        border: 1px solid var(--line);
        background: linear-gradient(180deg, rgba(17, 29, 41, 0.98), rgba(10, 16, 24, 0.98));
        box-shadow: 0 18px 34px rgba(0, 0, 0, 0.18);
      }}
      .game-card[hidden] {{
        display: none !important;
      }}
      .card-cover {{
        position: relative;
        aspect-ratio: 16 / 10;
        overflow: hidden;
        background: #0b141d;
      }}
      .card-cover-image {{
        display: block;
        width: 100%;
        height: 100%;
        object-fit: cover;
      }}
      .card-cover-fallback {{
        width: 100%;
        height: 100%;
        display: grid;
        place-items: center;
        gap: 6px;
        background: linear-gradient(135deg, var(--tone-a), var(--tone-b));
        text-align: center;
      }}
      .card-cover-fallback strong {{
        font-size: 30px;
        line-height: 1;
      }}
      .card-cover-fallback span {{
        color: rgba(255, 255, 255, 0.8);
        font-size: 12px;
      }}
      .card-cover-mask {{
        position: absolute;
        inset: 0;
        background: linear-gradient(180deg, transparent 48%, rgba(5, 10, 15, 0.8) 100%);
      }}
      .card-cover-badges {{
        position: absolute;
        inset: 12px 12px auto;
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        z-index: 1;
      }}
      .chip {{
        display: inline-flex;
        align-items: center;
        min-height: 28px;
        padding: 0 10px;
        border-radius: 999px;
        border: 1px solid var(--line);
        background: rgba(5, 10, 15, 0.75);
        color: var(--text);
        font-size: 12px;
        font-weight: 700;
      }}
      .chip-soft {{
        color: var(--accent);
      }}
      .chip-strong {{
        color: var(--warm);
      }}
      .card-cover-tag {{
        position: absolute;
        right: 12px;
        bottom: 12px;
        z-index: 1;
        min-height: 28px;
        padding: 0 10px;
        border-radius: 999px;
        border: 1px solid rgba(113, 230, 193, 0.55);
        background: rgba(113, 230, 193, 0.12);
        color: var(--accent);
        font-size: 12px;
        font-weight: 800;
      }}
      .card-body {{
        padding: 16px;
        display: grid;
        gap: 12px;
      }}
      .card-title-row {{
        display: flex;
        gap: 12px;
        justify-content: space-between;
        align-items: flex-start;
      }}
      .card-title-row h3 {{
        margin: 0;
        font-size: 18px;
        line-height: 1.25;
      }}
      .card-title-row p {{
        margin: 7px 0 0;
        color: var(--muted);
        font-size: 13px;
        min-height: 39px;
      }}
      .card-index {{
        color: var(--muted);
        font-size: 12px;
        font-weight: 700;
        white-space: nowrap;
      }}
      .card-meta {{
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }}
      .card-meta span {{
        display: inline-flex;
        align-items: center;
        min-height: 28px;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.03);
        border: 1px solid var(--line);
        color: var(--muted);
        font-size: 12px;
        font-weight: 700;
      }}
      .card-actions {{
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }}
      .action-link.disabled {{
        opacity: 0.5;
        cursor: not-allowed;
      }}
      .action-link.cover {{
        border-color: rgba(113, 230, 193, 0.45);
      }}
      .action-link.resource {{
        border-color: rgba(116, 164, 255, 0.45);
      }}
      .empty-state {{
        padding: 30px 22px 36px;
        display: grid;
        gap: 8px;
        text-align: center;
        color: var(--muted);
      }}
      .empty-state strong {{
        color: var(--text);
        font-size: 18px;
      }}
      .page-note {{
        padding: 0 4px;
        color: var(--muted);
        font-size: 13px;
      }}
      @media (max-width: 980px) {{
        .hero {{
          grid-template-columns: 1fr;
        }}
        .toolbar {{
          grid-template-columns: 1fr;
        }}
        .library-head {{
          flex-direction: column;
          align-items: start;
        }}
      }}
      @media (max-width: 760px) {{
        .shell {{
          padding: 18px 12px 28px;
        }}
        .hero,
        .toolbar {{
          padding: 18px;
          border-radius: 16px;
        }}
        h1 {{
          font-size: 28px;
        }}
        .summary-grid {{
          grid-template-columns: 1fr 1fr;
        }}
        .library-grid {{
          grid-template-columns: 1fr;
          padding: 16px 18px 20px;
        }}
        .library-head {{
          padding: 18px 18px 0;
        }}
        .card-title-row {{
          flex-direction: column;
        }}
        .card-index {{
          align-self: flex-end;
        }}
      }}
      @media (max-width: 520px) {{
        .summary-grid {{
          grid-template-columns: 1fr;
        }}
        .hero-links,
        .sort-group,
        .card-actions {{
          width: 100%;
        }}
        .hero-links a,
        .action-link,
        .sort-button {{
          width: 100%;
          justify-content: center;
        }}
      }}
    </style>
  </head>
  <body>
    <main class="shell">
      <section class="hero">
        <div>
          <div class="eyebrow">静态页面 · 在线游戏库首页</div>
          <h1>FC_ROMS 游戏库</h1>
          <p>把封面、分类、平台、热度和发布日期放在一屏里，先搜索再筛选，减少翻表找资源的来回折腾。</p>
          <div class="hero-meta">
            <span class="hero-chip">关键词搜索</span>
            <span class="hero-chip">快速排序</span>
            <span class="hero-chip">封面预览</span>
            <span class="hero-chip">静态可访问</span>
          </div>
          <div class="hero-links">
            <a href="{base_href}manifest.v1.json">查看清单</a>
            <a href="{base_href}search-index.v1.json">查看索引</a>
            <a href="{base_href}games/">资源目录</a>
          </div>
          <p class="page-note">当前默认按 {sort_label} 排序，生成于 {format_time_stamp(generated_at)}。</p>
        </div>
        <div class="summary-grid">
          {metrics_html}
        </div>
      </section>

      <section class="toolbar" aria-label="搜索和排序">
        <label class="search-field">
          <span>搜索</span>
          <input id="searchInput" type="search" placeholder="搜索名称、分类、平台、简介" autocomplete="off" />
        </label>
        <div class="sort-group">
          <button class="sort-button{ ' active' if initial_sort == 'id' else '' }" data-sort="id" aria-pressed="{ 'true' if initial_sort == 'id' else 'false' }">编号</button>
          <button class="sort-button{ ' active' if initial_sort == 'title' else '' }" data-sort="title" aria-pressed="{ 'true' if initial_sort == 'title' else 'false' }">名称</button>
          <button class="sort-button{ ' active' if initial_sort == 'hotness' else '' }" data-sort="hotness" aria-pressed="{ 'true' if initial_sort == 'hotness' else 'false' }">热度</button>
          <button class="sort-button{ ' active' if initial_sort == 'date' else '' }" data-sort="date" aria-pressed="{ 'true' if initial_sort == 'date' else 'false' }">最新</button>
        </div>
        <div class="result-pill">
          显示 <strong id="visibleCount">{game_count}</strong> / {game_count}
        </div>
      </section>

      <section class="library" aria-label="游戏条目">
        <div class="library-head">
          <div>
            <h2>游戏条目</h2>
            <p>点击“查看封面”或“打开资源”进入对应页面，输入关键词后会实时过滤结果。</p>
          </div>
          <div class="page-note">按 <span id="currentSortLabel">{sort_label}</span> 排序</div>
        </div>
        <div class="library-grid" id="gameGrid">
          {cards_html_text}
        </div>
        {empty_html}
      </section>
    </main>
    {sort_script}
  </body>
</html>
"""
    path.write_text(index_html, encoding="utf-8")


if __name__ == "__main__":
    main()
