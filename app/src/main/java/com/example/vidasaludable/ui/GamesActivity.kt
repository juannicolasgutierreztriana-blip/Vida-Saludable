package com.example.vidasaludable.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.vidasaludable.R
import java.util.Collections

class GamesActivity : AppCompatActivity() {

    private lateinit var gridGame: GridLayout
    private lateinit var tvGameScore: TextView
    private val cardButtons = ArrayList<Button>()
    private val emojis = ArrayList<String>()

    private var firstSelectedIndex = -1
    private var matchesFound = 0
    private var isBusy = false
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

        prefs = getSharedPreferences("habitos_saludables", MODE_PRIVATE)

        gridGame = findViewById(R.id.gridGame)
        tvGameScore = findViewById(R.id.tvGameScore)

        findViewById<View>(R.id.btnBackGames).setOnClickListener { finish() }
        findViewById<View>(R.id.btnRestartGame).setOnClickListener { setupGame() }

        setupGame()
    }

    private fun setupGame() {
        gridGame.removeAllViews()
        cardButtons.clear()
        emojis.clear()
        firstSelectedIndex = -1
        matchesFound = 0
        isBusy = false

        tvGameScore.text = "Parejas encontradas: 0 / 4"

        emojis.add("🍎"); emojis.add("🍎")
        emojis.add("💊"); emojis.add("💊")
        emojis.add("💧"); emojis.add("💧")
        emojis.add("🌸"); emojis.add("🌸")

        Collections.shuffle(emojis)

        for (i in emojis.indices) {
            val index = i
            val btnCard = Button(this).apply {
                text = "❓"
                textSize = 26f
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(90)
                columnSpec = GridLayout.spec(i % 2, 1, 1f)
                rowSpec = GridLayout.spec(i / 2, 1, 1f)
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            btnCard.layoutParams = params

            btnCard.setOnClickListener { onCardClicked(index) }

            cardButtons.add(btnCard)
            gridGame.addView(btnCard)
        }
    }

    private fun onCardClicked(index: Int) {
        if (isBusy) return

        val clickedBtn = cardButtons[index]
        if (clickedBtn.text.toString() != "❓") return

        clickedBtn.text = emojis[index]

        if (firstSelectedIndex == -1) {
            firstSelectedIndex = index
        } else {
            if (firstSelectedIndex == index) return

            isBusy = true
            val prevIndex = firstSelectedIndex
            firstSelectedIndex = -1

            if (emojis[prevIndex] == emojis[index]) {
                matchesFound++
                tvGameScore.text = "Parejas encontradas: $matchesFound / 4"
                isBusy = false

                if (matchesFound == 4) {
                    val userName = prefs.getString("user_name", "Amigo/a")
                    Toast.makeText(this, "🎉 ¡Excelente Memoria $userName! Has completado el juego.", Toast.LENGTH_LONG).show()
                }
            } else {
                clickedBtn.postDelayed({
                    cardButtons[prevIndex].text = "❓"
                    clickedBtn.text = "❓"
                    isBusy = false
                }, 800)
            }
        }
    }

    private fun dp(v: Int): Int {
        return Math.round(v * resources.displayMetrics.density)
    }
}
