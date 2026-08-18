#!/usr/bin/env python3
"""Generate Retro Hall GitHub Pages catalog files."""

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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--games-dir", default="games")
    parser.add_argument("--output-dir", default="public")
    parser.add_argument("--base-url", default="")
    args = parser.parse_args()

    root = Path.cwd()
    games_dir = (root / args.games_dir).resolve()
    output_dir = (root / args.output_dir).resolve()
    base_url = args.base_url.rstrip("/")

    output_dir.mkdir(parents=True, exist_ok=True)
    games = load_games(games_dir=games_dir, output_dir=output_dir, base_url=base_url)

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
    )

    if games_dir.exists():
        target_games_dir = (output_dir / "games").resolve()
        if target_games_dir == games_dir or target_games_dir.is_relative_to(games_dir):
            raise ValueError("output games directory must not be the source games directory or a child of it")
        if target_games_dir.exists():
            shutil.rmtree(target_games_dir)
        shutil.copytree(games_dir, target_games_dir, ignore=shutil.ignore_patterns("game.json"))


def load_games(games_dir: Path, output_dir: Path, base_url: str) -> list[dict[str, Any]]:
    if not games_dir.exists():
        return []

    cover_cache: dict[str, str] = {}
    games: list[dict[str, Any]] = []
    for game_json in sorted(games_dir.glob("*/game.json")):
        raw = json.loads(game_json.read_text(encoding="utf-8"))
        game_dir = game_json.parent
        slug = game_dir.name
        games.append(
            normalize_game(
                raw=raw,
                slug=slug,
                game_dir=game_dir,
                output_dir=output_dir,
                base_url=base_url,
                cover_cache=cover_cache,
            ),
        )
    return sorted(games, key=lambda item: item["id"])


def normalize_game(
    raw: dict[str, Any],
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    cover_cache: dict[str, str],
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
        cover_cache=cover_cache,
    )

    item: dict[str, Any] = {
        "id": game_id,
        "title": title,
        "displayTitle": raw.get("displayTitle") or title_text(title),
        "platform": raw.get("platform") or "FC/NES",
        "category": raw.get("category") or "在线游戏库",
        "description": description,
        "assets": {
            "coverUrl": cover_url,
            "screenshots": [
                resolve_url(value, slug=slug, base_url=base_url)
                for value in assets.get("screenshots", [])
                if isinstance(value, str) and value
            ],
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
    cover_cache: dict[str, str],
) -> str:
    explicit_cover = raw.get("cover") or assets.get("cover") or assets.get("coverUrl") or ""
    if explicit_cover:
        materialized = materialize_cover(
            source=str(explicit_cover),
            slug=slug,
            game_dir=game_dir,
            output_dir=output_dir,
            base_url=base_url,
            cover_cache=cover_cache,
        )
        if materialized:
            return materialized

    playlist = thumbnail_playlist(str(raw.get("platform") or "FC/NES"))
    if not playlist:
        return ""

    candidates = [
        first_non_blank(
            roms[0].get("name") if roms else "",
            Path(str(raw.get("rom") or raw.get("romPath") or "")).stem,
        ),
        first_non_blank(
            raw.get("displayTitle"),
            title,
        ),
        slug,
    ]
    for candidate in candidates:
        if not candidate:
            continue
        boxart = thumbnail_url(playlist, "Named_Boxarts", candidate)
        if boxart:
            materialized = materialize_cover(
                source=boxart,
                slug=slug,
                game_dir=game_dir,
                output_dir=output_dir,
                base_url=base_url,
                cover_cache=cover_cache,
            )
            if materialized:
                return materialized
        logo = thumbnail_url(playlist, "Named_Logos", candidate)
        if logo:
            materialized = materialize_cover(
                source=logo,
                slug=slug,
                game_dir=game_dir,
                output_dir=output_dir,
                base_url=base_url,
                cover_cache=cover_cache,
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


def materialize_cover(
    source: str,
    slug: str,
    game_dir: Path,
    output_dir: Path,
    base_url: str,
    cover_cache: dict[str, str],
) -> str:
    if not source:
        return ""
    cached = cover_cache.get(source)
    if cached:
        return cover_asset_url(cached, base_url)

    target_dir = output_dir / "covers"
    target_dir.mkdir(parents=True, exist_ok=True)
    target_path = target_dir / cover_file_name(source, slug, game_dir)
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
        cover_cache[source] = target_path.relative_to(output_dir).as_posix()
        return cover_asset_url(cover_cache[source], base_url)
    return ""


def cover_file_name(source: str, slug: str, game_dir: Path) -> str:
    suffix = Path(urlparse(source).path).suffix
    if not suffix:
        local_path = resolve_local_path(source, game_dir)
        if local_path is not None:
            suffix = local_path.suffix
    if not suffix:
        suffix = ".png"
    digest = hashlib.sha1(source.encode("utf-8")).hexdigest()[:16]
    return f"{slug}-{digest}{suffix}"


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


def write_index_html(path: Path, manifest: dict[str, Any], search_index: dict[str, Any], base_url: str) -> None:
    games = manifest.get("games", [])
    generated_at = html.escape(str(manifest.get("generatedAt", "")))
    game_count = len(games)
    base_href = f"{base_url}/" if base_url else ""

    rows = []
    for game in games:
        title = html.escape(title_text(game.get("displayTitle") or game.get("title") or game.get("id")))
        category = html.escape(title_text(game.get("category", "")))
        platform = html.escape(title_text(game.get("platform", "")))
        description = html.escape(title_text(game.get("description", "")))
        cover_url = title_text(game.get("assets", {}).get("coverUrl", ""))
        rom_url = ""
        roms = game.get("roms") or []
        if roms:
            first_rom = roms[0]
            rom_url = title_text(first_rom.get("url") or first_rom.get("path") or "")

        cover_html = f'<a href="{html.escape(cover_url)}" target="_blank" rel="noreferrer">封面</a>' if cover_url else "无"
        rom_html = f'<a href="{html.escape(rom_url)}" target="_blank" rel="noreferrer">资源</a>' if rom_url else "无"

        rows.append(
            "<tr>"
            f"<td>{title}</td>"
            f"<td>{category}</td>"
            f"<td>{platform}</td>"
            f"<td>{description}</td>"
            f"<td>{cover_html}</td>"
            f"<td>{rom_html}</td>"
            "</tr>"
        )

    if rows:
        rows_html = "\n".join(rows)
    else:
        rows_html = (
            "<tr>"
            '<td colspan="6" class="empty">当前没有可发布的游戏条目。把 `游戏目录` 下的 `game.json` 加进来后再运行生成脚本即可。</td>'
            "</tr>"
        )

    index_html = f"""<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="robots" content="noindex" />
    <title>复古大厅游戏库</title>
    <style>
      :root {{
        color-scheme: light;
        --bg: #f6f8fb;
        --panel: #ffffff;
        --text: #172033;
        --muted: #5d6a82;
        --line: #dbe2ee;
        --accent: #2457d6;
      }}
      * {{ box-sizing: border-box; }}
      body {{
        margin: 0;
        background: var(--bg);
        color: var(--text);
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
        line-height: 1.5;
      }}
      .wrap {{
        max-width: 1120px;
        margin: 0 auto;
        padding: 32px 20px 48px;
      }}
      header {{
        display: grid;
        gap: 12px;
        margin-bottom: 24px;
      }}
      h1 {{
        margin: 0;
        font-size: 32px;
        line-height: 1.15;
      }}
      .meta {{
        color: var(--muted);
        font-size: 14px;
      }}
      .links {{
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
        margin: 8px 0 0;
      }}
      .links a {{
        color: var(--accent);
        text-decoration: none;
      }}
      .links a:hover {{ text-decoration: underline; }}
      .panel {{
        background: var(--panel);
        border: 1px solid var(--line);
        border-radius: 8px;
        overflow: hidden;
      }}
      table {{
        width: 100%;
        border-collapse: collapse;
      }}
      thead {{
        background: #eef3fb;
      }}
      th, td {{
        padding: 12px 14px;
        text-align: left;
        vertical-align: top;
        border-bottom: 1px solid var(--line);
      }}
      th {{
        font-size: 14px;
        color: var(--muted);
        font-weight: 600;
      }}
      td {{
        font-size: 14px;
      }}
      .empty {{
        color: var(--muted);
        text-align: center;
        padding: 32px 14px;
      }}
      .note {{
        margin-top: 16px;
        color: var(--muted);
        font-size: 13px;
      }}
      code {{
        background: #eef3fb;
        padding: 0 4px;
        border-radius: 4px;
      }}
      @media (max-width: 780px) {{
        .wrap {{ padding: 24px 14px 40px; }}
        h1 {{ font-size: 26px; }}
        table, thead, tbody, th, td, tr {{ display: block; }}
        thead {{ display: none; }}
        tr {{
          border-bottom: 1px solid var(--line);
          padding: 10px 0;
        }}
        td {{
          border: 0;
          padding: 6px 14px;
        }}
        td::before {{
          display: block;
          color: var(--muted);
          font-size: 12px;
          margin-bottom: 2px;
        }}
        td:nth-child(1)::before {{ content: "标题"; }}
        td:nth-child(2)::before {{ content: "分类"; }}
        td:nth-child(3)::before {{ content: "平台"; }}
        td:nth-child(4)::before {{ content: "简介"; }}
        td:nth-child(5)::before {{ content: "封面"; }}
        td:nth-child(6)::before {{ content: "资源"; }}
      }}
    </style>
  </head>
  <body>
    <main class="wrap">
      <header>
        <h1>复古大厅游戏库</h1>
        <div class="meta">
          共 {game_count} 个条目，生成时间 {generated_at}
        </div>
        <div class="links">
          <a href="{base_href}manifest.v1.json">清单文件</a>
          <a href="{base_href}search-index.v1.json">搜索索引</a>
          <a href="{base_href}games/">资源目录</a>
        </div>
      </header>

      <section class="panel">
        <table>
          <thead>
            <tr>
              <th>标题</th>
              <th>分类</th>
              <th>平台</th>
              <th>简介</th>
              <th>封面</th>
              <th>资源</th>
            </tr>
          </thead>
          <tbody>
            {rows_html}
          </tbody>
        </table>
      </section>

      <div class="note">
        说明：这个首页是静态生成的，部署到静态站点后，仓库根路径就会有可访问页面，不再直接返回 404。
      </div>
    </main>
  </body>
</html>
"""
    path.write_text(index_html, encoding="utf-8")


if __name__ == "__main__":
    main()
