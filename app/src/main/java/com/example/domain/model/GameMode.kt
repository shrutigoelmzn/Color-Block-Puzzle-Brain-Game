package com.example.domain.model

enum class GameMode(val title: String, val description: String) {
    CLASSIC(
        title = "Classic",
        description = "Endless puzzle mode. Place blocks until no moves remain."
    ),
    TIMED(
        title = "Timed Rush",
        description = "2-minute rush! Clear lines and combos to gain bonus time."
    ),
    CHALLENGE(
        title = "Daily Challenge",
        description = "Unique daily puzzle. Complete the objective for rewards!"
    );

    val displayName: String get() = title
}
