package com.abc.unscramble.ui

// Model class
/**
 * Data class that represents the game UI state.
 */
data class GameUiState(
    val score: Int = 0,
    val currentWordCount: Int = 1,
    val currentScrambledWord: String = "",
    val isGuessedWordWrong: Boolean = false,
    val isGameOver: Boolean = false
)
