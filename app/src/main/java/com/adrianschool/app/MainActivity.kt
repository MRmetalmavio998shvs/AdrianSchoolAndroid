
package com.adrianschool.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    private val orange = Color.rgb(242, 140, 40)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CookieHandler.setDefault(
            CookieManager(null, CookiePolicy.ACCEPT_ALL)
        )

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
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()

            if (userEmail.isBlank() || userPassword.isBlank()) {
                Toast.makeText(
                    this,
                    "Inserisci email e password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            login.isEnabled = false
            login.text = "ACCESSO IN CORSO..."

            Thread {
                var connection: HttpURLConnection? = null

                try {
                    connection = URL(
                        "http://127.0.0.1:3000/api/auth/login"
                    ).openConnection() as HttpURLConnection

                    connection.requestMethod = "POST"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.doOutput = true
                    connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    val body = JSONObject().apply {
                        put("email", userEmail)
                        put("password", userPassword)
                    }

                    connection.outputStream.use {
                        it.write(body.toString().toByteArray(Charsets.UTF_8))
                    }

                    val code = connection.responseCode

                    val response = (
                        if (code in 200..299) connection.inputStream
                        else connection.errorStream
                    ).bufferedReader().use { it.readText() }

                    val result = JSONObject(response)

                    if (code == 200) {
                        val user = result.getJSONObject("user")
                        val username = user.optString("username", "Utente")

                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Benvenuto, $username!",
                                Toast.LENGTH_LONG
                            ).show()

                            subtitle.text = "Accesso effettuato: $username"
                            login.text = "ACCESSO EFFETTUATO"
                            login.isEnabled = false
                        }
                    } else {
                        val message = result.optString(
                            "error",
                            "Errore durante l'accesso"
                        )

                        runOnUiThread {
                            Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                            login.text = "ACCEDI"
                            login.isEnabled = true
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Errore di connessione: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        login.text = "ACCEDI"
                        login.isEnabled = true
                    }
                } finally {
                    connection?.disconnect()
                }
            }.start()
        }
    }
}
