package com.abc.unscramble.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.abc.unscramble.data.MAX_NO_OF_WORDS
import com.abc.unscramble.data.SCORE_INCREASE
import com.abc.unscramble.data.allWords
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel containing the app data and methods to process the data
 */
class GameViewModel : ViewModel() {

    /* Backing Property (not backing field as in get(), set() methods).
     *
     * Cannot use private set, because we should only use immutable StateFlow outside the ViewModel
     * class and not immutable MutableStateFlow :)
     *
     * So, It is a Game UI state. Prefer StateFlow over LiveData or composable state.
     * Composable State (mutableStateOf()) is better to use only in Composables for simple tasks.
     * LiveData is old, and it doesn't have flow functionality (filter, for example).
     *
     * StateFlow works well with classes that must maintain an observable immutable state.
     * And our GameUiState is immutable state UI for the UDF pattern.
     */
    private val _uiState = MutableStateFlow<GameUiState>(value = GameUiState())
    // I can change its value in VM: _uiState.value = GameUiState object (T = GameUiState)
    // Then make this mutable state flow a read-only state flow for accessing outside the VM:
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /*
     * The problem when we use Composable State instead of StateFlow is that we can't use this VM
     * with UI system other than Compose. We should separate UI and VM.
     *
     * But we could do:
     * private val _uiState = mutableStateOf(GameUiState())
     * val uiState: State<GameUiState> = _uiState
     * But then someone could parse it back to MutableState
     *
     * This variant is better:
     * var uiState by mutableStateOf(GameUiState())
     *     private set
     */


    private lateinit var currentWord: String

    // Set of words used in the game
    private var usedWords: MutableSet<String> = mutableSetOf()

    // Observable variable that we use in the UI. With it, we track changes from the events
    var userGuess by mutableStateOf("")
        private set


    init {
        resetGame()
    }


    private fun pickRandomWordAndShuffle(): String {
        // Continue picking up a new random word until you get one that hasn't been used before. Recursion.
        currentWord = allWords.random()
        return if (usedWords.contains(currentWord)) {
            pickRandomWordAndShuffle()
        } else {
            usedWords.add(currentWord)
            shuffleCurrentWord(currentWord)
        }
    }

    private fun shuffleCurrentWord(word: String): String {
        val tempWord = word.toCharArray()
        // Scramble the word
        tempWord.shuffle()
        while (String(tempWord) == word) {
            tempWord.shuffle()
        }
        return String(tempWord)
    }

    /*
     * Picks a new currentWord and currentScrambledWord and updates UiState according to
     * current game state.
     */
    private fun updateGameState(updatedScore: Int) {
        if (usedWords.size == MAX_NO_OF_WORDS) {
            // Last round in the game, update isGameOver to true, don't pick a new word
            _uiState.update { currentState ->
                currentState.copy(
                    score = updatedScore,
                    isGameOver = true,
                    isGuessedWordWrong = false
                )
            }
        } else {
            // Normal round in the game
            _uiState.update { currentState ->
                currentState.copy(
                    score = updatedScore,
                    currentWordCount = currentState.currentWordCount.inc(),
                    currentScrambledWord = pickRandomWordAndShuffle(),
                    isGuessedWordWrong = false
                )
            }
        }
    }


    /*
     * Re-initializes the game data to restart the game.
     */
    fun resetGame() {
        usedWords.clear()
        _uiState.value = GameUiState(currentScrambledWord = pickRandomWordAndShuffle())
    }

    /*
     * Skip to next word
     */
    fun skipWord() {
        updateGameState(_uiState.value.score)
        // Reset user guess
        updateUserGuess("")
    }

    /*
     * Checks if the user's guess is correct.
     * Increases the score accordingly.
     */
    fun checkUserGuess() {
        if (userGuess.equals(currentWord, ignoreCase = true)) {
            // User's guess is correct, increase the score
            // and call updateGameState() to prepare the game for next round
            val updatedScore = _uiState.value.score.plus(SCORE_INCREASE)
            updateGameState(updatedScore)
        } else {
            // User's guess is wrong, show an error
            _uiState.update { currentState ->
                currentState.copy(isGuessedWordWrong = true)
            }
        }

        // Reset user guess
        updateUserGuess("")
    }

    // Event
    /*
     * Update the user's guess
     */
    fun updateUserGuess(guessedWord: String) {
        userGuess = guessedWord
    }
}