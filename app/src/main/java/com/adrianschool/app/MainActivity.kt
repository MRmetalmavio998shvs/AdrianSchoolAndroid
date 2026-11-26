package com.adrianschool.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {

    private val orange = Color.rgb(242, 140, 40)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 40, 32, 40)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "Adrian School"
            textSize = 32f
            setTextColor(orange)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Il tuo registro elettronico"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        val email = EditText(this).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        val password = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val login = Button(this).apply {
            text = "ACCEDI"
            setBackgroundColor(orange)
            setTextColor(Color.WHITE)
        }

        fun params() = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 16, 0, 16)
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(email, params())
        layout.addView(password, params())
        layout.addView(login, params())

        setContentView(layout)

        login.setOnClickListener {
            if (email.text.isBlank() || password.text.isBlank()) {
                Toast.makeText(
                    this,
                    "Inserisci email e password",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Schermata pronta! Collegamento al server da completare.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
