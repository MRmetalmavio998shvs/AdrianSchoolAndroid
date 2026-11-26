package com.adrianschool.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import org.json.JSONObject
import org.json.JSONArray
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity() {

    private val orange = Color.rgb(242, 140, 40)
    private val baseUrl = "http://127.0.0.1:3000"

    private lateinit var content: LinearLayout
    private lateinit var statusText: TextView
    private var currentUser: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CookieHandler.setDefault(
            CookieManager(null, CookiePolicy.ACCEPT_ALL)
        )

        showLogin()
    }

    private fun apiRequest(
        method: String,
        path: String,
        body: JSONObject? = null
    ): Pair<Int, String> {

        val connection = URL(baseUrl + path)
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Accept", "application/json")

            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.outputStream.use {
                    it.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            return Pair(code, response)

        } finally {
            connection.disconnect()
        }
    }

    private fun showLogin() {
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
            setMargins(0, 12, 0, 12)
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
                try {
                    val body = JSONObject().apply {
                        put("email", userEmail)
                        put("password", userPassword)
                    }

                    val (code, response) = apiRequest(
                        "POST",
                        "/api/auth/login",
                        body
                    )

                    val result = JSONObject(response)

                    if (code == 200) {
                        val user = result.getJSONObject("user")

                        runOnUiThread {
                            currentUser = user
                            showDashboard(user)
                        }
                    } else {
                        val message = result.optString(
                            "error",
                            "Accesso non riuscito"
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
                            "Errore: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        login.text = "ACCEDI"
                        login.isEnabled = true
                    }
                }
            }.start()
        }
    }

    private fun showDashboard(user: JSONObject) {
        val username = user.optString("username", "Utente")
        val role = user.optString("role", "student")

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(orange)
        }

        val title = TextView(this).apply {
            text = "Adrian School"
            textSize = 27f
            setTextColor(Color.WHITE)
        }

        val welcome = TextView(this).apply {
            text = "Benvenuto, $username"
            textSize = 19f
            setTextColor(Color.WHITE)
        }

        val roleLabel = when (role) {
            "admin" -> "Amministratore"
            "teacher" -> "Professore"
            else -> "Studente"
        }

        val roleView = TextView(this).apply {
            text = roleLabel
            textSize = 14f
            setTextColor(Color.WHITE)
        }

        header.addView(title)
        header.addView(welcome)
        header.addView(roleView)

        val scroll = ScrollView(this)

        val pageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 24)
        }

        statusText = TextView(this).apply {
            text = "Caricamento dei dati..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
        }

        content = pageContent

        val refresh = Button(this).apply {
            text = "AGGIORNA DATI"
            setTextColor(Color.WHITE)
            setBackgroundColor(orange)
            setOnClickListener {
                loadDashboardData()
            }
        }

        val logout = Button(this).apply {
            text = "ESCI"
            setOnClickListener {
                logout()
            }
        }

        pageContent.addView(statusText)
        pageContent.addView(refresh)
        pageContent.addView(logout)

        scroll.addView(pageContent)

        page.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        page.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(page)
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val user = currentUser ?: return
        val role = user.optString("role", "student")

        statusText.text = "Caricamento dei dati..."

        Thread {
            try {
                val classes: JSONArray
                val grades: JSONArray
                var usersCount = -1

                if (role == "student") {
                    val result = apiRequest(
                        "GET",
                        "/api/grades/"
                    )

                    if (result.first != 200) {
                        throw Exception("Impossibile caricare i voti")
                    }

                    grades = JSONObject(result.second)
                        .optJSONArray("grades") ?: JSONArray()

                    classes = JSONArray()

                } else {
                    val result = apiRequest(
                        "GET",
                        "/api/classes/mine"
                    )

                    if (result.first != 200) {
                        throw Exception("Impossibile caricare le classi")
                    }

                    classes = JSONObject(result.second)
                        .optJSONArray("classes") ?: JSONArray()

                    grades = JSONArray()

                    if (role == "admin") {
                        val usersResult = apiRequest(
                            "GET",
                            "/api/auth/users"
                        )

                        if (usersResult.first == 200) {
                            usersCount = JSONObject(usersResult.second)
                                .optJSONArray("users")?.length() ?: 0
                        }
                    }
                }

                runOnUiThread {
                    renderDashboard(
                        role,
                        classes,
                        grades,
                        usersCount
                    )
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text =
                        "Errore nel caricamento: ${e.message}"
                }
            }
        }.start()
    }

    private fun renderDashboard(
        role: String,
        classes: JSONArray,
        grades: JSONArray,
        usersCount: Int
    ) {
        content.removeAllViews()

        statusText.text = "Dati aggiornati dal server"
        content.addView(statusText)

        when (role) {
            "admin" -> {
                addCard("CLASSI", classes.length().toString())

                if (usersCount >= 0) {
                    addCard("UTENTI REGISTRATI", usersCount.toString())
                }

                addSection("Le classi")

                if (classes.length() == 0) {
                    addText("Non ci sono classi registrate.")
                }

                for (i in 0 until classes.length()) {
                    val item = classes.getJSONObject(i)
                    addClass(item)
                }
            }

            "teacher" -> {
                addCard("CLASSI ASSEGNATE", classes.length().toString())
                addSection("Le tue classi")

                if (classes.length() == 0) {
                    addText("Non hai ancora classi assegnate.")
                }

                for (i in 0 until classes.length()) {
                    val item = classes.getJSONObject(i)
                    addClass(item)
                }
            }

            else -> {
                addCard("VOTI REGISTRATI", grades.length().toString())

                var total = 0.0
                var count = 0

                for (i in 0 until grades.length()) {
                    val grade = grades.getJSONObject(i)
                        .optDouble("grade", Double.NaN)

                    if (!grade.isNaN()) {
                        total += grade
                        count++
                    }
                }

                if (count > 0) {
                    val average = total / count
                    addCard(
                        "MEDIA SCOLASTICA",
                        String.format(Locale.ITALIAN, "%.2f", average)
                    )
                } else {
                    addCard("MEDIA SCOLASTICA", "Nessun voto")
                }

                addSection("Ultimi voti")

                if (grades.length() == 0) {
                    addText("Non hai ancora voti registrati.")
                }

                for (i in 0 until grades.length()) {
                    val item = grades.getJSONObject(i)

                    val subject = item.optString("subject", "Materia")
                    val grade = item.optString("grade", "-")
                    val date = item.optString("date", "")
                    val description = item.optString("description", "")

                    val details = if (description.isNotBlank()) {
                        "$subject\nVoto: $grade\nData: $date\n$description"
                    } else {
                        "$subject\nVoto: $grade\nData: $date"
                    }

                    addText(details)
                }
            }
        }
    }

    private fun addClass(item: JSONObject) {
        val name = item.optString("name", "")
        val section = item.optString("section", "")
        val year = item.optString("school_year", "")
        val program = item.optString("study_program", "")

        val details = buildString {
            append("$name $section")
            if (year.isNotBlank()) append("\nAnno scolastico: $year")
            if (program.isNotBlank()) append("\nIndirizzo: $program")
        }

        addText(details)
    }

    private fun addSection(text: String) {
        val view = TextView(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(orange)
            setPadding(0, 22, 0, 12)
        }

        content.addView(view)
    }

    private fun addCard(label: String, value: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.DKGRAY)
        }

        val number = TextView(this).apply {
            text = value
            textSize = 26f
            setTextColor(orange)
        }

        card.addView(title)
        card.addView(number)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 8, 0, 8)
        content.addView(card, params)
    }

    private fun addText(value: String) {
        val view = TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.WHITE)
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 5, 0, 5)
        content.addView(view, params)
    }

    private fun logout() {
        Thread {
            try {
                apiRequest("POST", "/api/auth/logout")
            } catch (_: Exception) {
            }

            runOnUiThread {
                currentUser = null
                showLogin()

                Toast.makeText(
                    this,
                    "Sei uscito dall'account",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }
}
