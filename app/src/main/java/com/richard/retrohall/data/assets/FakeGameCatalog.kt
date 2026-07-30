package com.richard.retrohall.data.assets

import com.richard.retrohall.domain.game.LocalGame

object FakeGameCatalog {
    val games = listOf(
        LocalGame(
            id = "sample-action-1",
            title = "Pixel Runner",
            platform = "NES",
            category = "动作",
            coverPath = "covers/pixel-runner.png",
            romPath = "files/roms/pixel-runner.nes",
        ),
        LocalGame(
            id = "sample-action-2",
            title = "Castle Quest",
            platform = "NES",
            category = "动作",
            coverPath = "covers/castle-quest.png",
            romPath = "files/roms/castle-quest.nes",
            favorite = true,
        ),
        LocalGame(
            id = "sample-racing-1",
            title = "Turbo Road",
            platform = "NES",
            category = "竞速",
            coverPath = "covers/turbo-road.png",
            romPath = "files/roms/turbo-road.nes",
        ),
        LocalGame(
            id = "sample-puzzle-1",
            title = "Block Logic",
            platform = "NES",
            category = "益智",
            coverPath = "covers/block-logic.png",
            romPath = "files/roms/block-logic.nes",
        ),
        LocalGame(
            id = "sample-shooter-1",
            title = "Star Guard",
            platform = "NES",
            category = "射击",
            coverPath = "covers/star-guard.png",
            romPath = "files/roms/star-guard.nes",
        ),
        LocalGame(
            id = "sample-sports-1",
            title = "Retro Tennis",
            platform = "NES",
            category = "体育",
            coverPath = "covers/retro-tennis.png",
            romPath = "files/roms/retro-tennis.nes",
        ),
    )
}
