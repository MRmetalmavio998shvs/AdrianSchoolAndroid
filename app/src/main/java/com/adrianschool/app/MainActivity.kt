package com.adrianschool.app

import android.app.Activity
import android.os.Bundle
import android.os.StrictMode
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType
import org.json.JSONArray
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class MainActivity : Activity() {

    private val orange = Color.rgb(242, 140, 40)
    private val dark = Color.rgb(35, 38, 45)
    private val muted = Color.rgb(120, 125, 135)
    private val background = Color.rgb(247, 248, 250)
    private val baseUrl = "http://127.0.0.1:3000"

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var titleText: TextView

    private var currentUser = JSONObject()
    private var currentPage = "dashboard"
    private var classes = JSONArray()
    private var users = JSONArray()
    private var students = JSONArray()
    private var selectedClassId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().permitAll().build()
        )

        if (CookieHandler.getDefault() == null) {
            val manager = CookieManager()
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            CookieHandler.setDefault(manager)
        }

        showLoading()

        val response = api("GET", "/api/auth/me")
        if (response.first && response.second.has("user")) {
            currentUser = response.second.getJSONObject("user")
            showApp()
        } else {
            showLogin()
        }
    }

    private fun api(
        method: String,
        path: String,
        body: JSONObject? = null
    ): Pair<Boolean, JSONObject> {
        return try {
            val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")

            if (body != null) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            connection.disconnect()

            Pair(code in 200..299, JSONObject(text))
        } catch (e: Exception) {
            Pair(false, JSONObject().put("error", "Server non raggiungibile. Avvia Adrian School in Termux."))
        }
    }

    private fun showLoading() {
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(background)
        root.addView(label("Adrian School", 28, dark, true))
        root.addView(label("Caricamento...", 15, muted))
        setContentView(root)
    }

    private fun showLogin() {
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(28, 24, 28, 24)
        root.setBackgroundColor(background)

        val scroll = ScrollView(this)
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(22, 28, 22, 28)
        panel.background = rounded(Color.WHITE, 22)
        scroll.addView(panel)

        panel.addView(label("🎓", 48, orange, true, Gravity.CENTER))
        panel.addView(label("Adrian School", 30, dark, true, Gravity.CENTER))
        panel.addView(label("Il tuo registro elettronico", 15, muted, false, Gravity.CENTER))
        space(panel, 28)

        val status = api("GET", "/api/auth/status")
        val adminExists = status.second.optBoolean("adminExists", true)

        if (!adminExists) {
            panel.addView(label("Configura amministratore", 21, dark, true))
            val username = input(panel, "Nome utente", false)
            val email = input(panel, "Email", false)
            val password = input(panel, "Password (almeno 12 caratteri)", true)

            button(panel, "Crea amministratore") {
                val data = JSONObject()
                    .put("username", username.text.toString().trim())
                    .put("email", email.text.toString().trim())
                    .put("password", password.text.toString())

                val result = api("POST", "/api/auth/setup-admin", data)
                if (result.first) {
                    toast("Amministratore creato. Accedi.")
                    showLogin()
                } else {
                    toast(result.second.optString("error", "Errore"))
                }
            }
        } else {
            panel.addView(label("Accedi al tuo account", 21, dark, true))
            space(panel, 10)
            val email = input(panel, "Email", false)
            val password = input(panel, "Password", true)

            button(panel, "Accedi") {
                val data = JSONObject()
                    .put("email", email.text.toString().trim())
                    .put("password", password.text.toString())

                val result = api("POST", "/api/auth/login", data)
                if (result.first && result.second.has("user")) {
                    currentUser = result.second.getJSONObject("user")
                    showApp()
                } else {
                    toast(result.second.optString("error", "Accesso non riuscito"))
                }
            }
        }

        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun showApp() {
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(background)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(18, 14, 18, 14)
        header.setBackgroundColor(Color.WHITE)

        val brand = LinearLayout(this)
        brand.orientation = LinearLayout.VERTICAL
        brand.addView(label("🎓 Adrian School", 20, orange, true))
        brand.addView(label("Registro elettronico", 12, muted))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val logout = TextView(this)
        logout.text = "Esci"
        logout.textSize = 14f
        logout.setTextColor(orange)
        logout.setPadding(12, 10, 4, 10)
        logout.setOnClickListener {
            api("POST", "/api/auth/logout", JSONObject())
            currentUser = JSONObject()
            showLogin()
        }
        header.addView(logout)
        root.addView(header)

        val divider = View(this)
        divider.setBackgroundColor(Color.rgb(235, 236, 239))
        root.addView(divider, LinearLayout.LayoutParams(-1, 1))

        val scroll = ScrollView(this)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(18, 18, 18, 24)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.gravity = Gravity.CENTER
        nav.setPadding(4, 8, 4, 8)
        nav.setBackgroundColor(Color.WHITE)

        navButton(nav, "⌂\nOggi", "today")
        navButton(nav, "▦\nDashboard", "dashboard")
        navButton(nav, "✦\nFunzioni", "functions")
        navButton(nav, "☰\nProfilo", "profile")

        root.addView(nav)
        setContentView(root)
        openPage("dashboard")
    }

    private fun navButton(parent: LinearLayout, text: String, page: String) {
        val item = TextView(this)
        item.text = text
        item.gravity = Gravity.CENTER
        item.textSize = 12f
        item.setTextColor(if (currentPage == page) orange else muted)
        item.setPadding(8, 7, 8, 7)
        item.setOnClickListener { openPage(page) }
        parent.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun openPage(page: String) {
        currentPage = page
        content.removeAllViews()

        when (page) {
            "dashboard" -> dashboard()
            "today" -> today()
            "functions" -> functions()
            "profile" -> profile()
            "accounts" -> accountsPage()
            "classes" -> classesPage()
            "grades" -> gradesPage()
            "registro" -> registroPage()
            "agenda" -> placeholder("Agenda", "Qui saranno disponibili gli eventi scolastici.")
            "timetable" -> placeholder("Orario", "La gestione dell'orario richiede ancora le relative API sul server.")
            "attendance" -> placeholder("Assenze e ritardi", "La gestione delle assenze richiede ancora le relative API sul server.")
            "homework" -> placeholder("Compiti", "La gestione dei compiti richiede ancora le relative API sul server.")
            "communications" -> placeholder("Comunicazioni", "Le comunicazioni saranno disponibili quando verranno aggiunte al server.")
            "bulletin" -> placeholder("Bacheca", "La bacheca sarà collegata alle API in un aggiornamento successivo.")
            "teaching" -> placeholder("Didattica", "I materiali didattici saranno collegati al server in un aggiornamento successivo.")
            else -> dashboard()
        }
    }

    private fun dashboard() {
        val name = currentUser.optString("username", "Utente")
        val role = currentUser.optString("role", "student")

        content.addView(label("Ciao, $name 👋", 25, dark, true))
        content.addView(label(
            when (role) {
                "admin" -> "Amministratore"
                "teacher" -> "Insegnante"
                else -> "Studente"
            },
            14, muted
        ))
        space(content, 18)

        when (role) {
            "admin" -> {
                val userResult = api("GET", "/api/auth/users")
                users = userResult.second.optJSONArray("users") ?: JSONArray()

                val classResult = api("GET", "/api/classes/")
                classes = classResult.second.optJSONArray("classes") ?: JSONArray()

                statCard(content, "Account registrati", users.length().toString(), "Gestisci gli account") {
                    openPage("accounts")
                }
                statCard(content, "Classi scolastiche", classes.length().toString(), "Gestisci le classi") {
                    openPage("classes")
                }
                statCard(content, "Valutazioni", "Registro", "Consulta e inserisci i voti") {
                    openPage("grades")
                }
                sectionTitle(content, "Gestione rapida")
                actionCard(content, "👥  Account", "Crea e consulta gli utenti") { openPage("accounts") }
                actionCard(content, "🏫  Classi", "Crea classi e assegna gli utenti") { openPage("classes") }
                actionCard(content, "📝  Voti", "Inserisci e consulta le valutazioni") { openPage("grades") }
            }

            "teacher" -> {
                val result = api("GET", "/api/classes/mine")
                classes = result.second.optJSONArray("classes") ?: JSONArray()

                statCard(content, "Le mie classi", classes.length().toString(), "Visualizza le classi assegnate") {
                    openPage("classes")
                }
                statCard(content, "Valutazioni", "Voti", "Inserisci valutazioni") {
                    openPage("grades")
                }
                sectionTitle(content, "Le tue funzioni")
                actionCard(content, "📝  Inserisci voti", "Registra una valutazione") { openPage("grades") }
                actionCard(content, "🏫  Le mie classi", "Consulta le classi assegnate") { openPage("classes") }
                actionCard(content, "📅  Agenda", "Sezione in preparazione") { openPage("agenda") }
            }

            else -> {
                val result = api("GET", "/api/grades/")
                val grades = result.second.optJSONArray("grades") ?: JSONArray()
                var total = 0.0
                var count = 0
                for (i in 0 until grades.length()) {
                    val g = grades.optJSONObject(i) ?: continue
                    val value = g.optDouble("grade", Double.NaN)
                    if (!value.isNaN()) {
                        total += value
                        count++
                    }
                }
                val average = if (count > 0) String.format("%.2f", total / count) else "—"

                statCard(content, "Media generale", average, "Calcolata sui voti disponibili") {
                    openPage("grades")
                }
                statCard(content, "Valutazioni", grades.length().toString(), "Consulta il registro") {
                    openPage("grades")
                }
                sectionTitle(content, "Accesso rapido")
                actionCard(content, "📘  Registro", "Consulta i tuoi voti") { openPage("registro") }
                actionCard(content, "📅  Agenda", "Eventi e appuntamenti") { openPage("agenda") }
                actionCard(content, "🕒  Orario", "Consulta l'orario scolastico") { openPage("timetable") }
                actionCard(content, "📚  Compiti", "Sezione in preparazione") { openPage("homework") }
                sectionTitle(content, "Ultime valutazioni")
                showGradeList(content, grades, 5)
            }
        }
    }

    private fun today() {
        content.addView(label("Oggi", 27, dark, true))
        content.addView(label("Benvenuto in Adrian School", 15, muted))
        space(content, 18)
        sectionTitle(content, "Le tue sezioni")
        actionCard(content, "📘  Registro", "Voti e valutazioni") { openPage("registro") }
        actionCard(content, "📅  Agenda", "Eventi e appuntamenti") { openPage("agenda") }
        actionCard(content, "🕒  Orario", "Lezioni scolastiche") { openPage("timetable") }
        actionCard(content, "📢  Comunicazioni", "Avvisi della scuola") { openPage("communications") }
    }

    private fun functions() {
        content.addView(label("Funzioni", 27, dark, true))
        content.addView(label("Tutti gli strumenti di Adrian School", 14, muted))
        space(content, 16)

        val items = listOf(
            Triple("📘  Registro", "Voti e valutazioni", "registro"),
            Triple("📅  Agenda", "Eventi scolastici", "agenda"),
            Triple("📚  Didattica", "Materiali didattici", "teaching"),
            Triple("📝  Valutazioni", "Gestione dei voti", "grades"),
            Triple("📋  Assenze", "Presenze e ritardi", "attendance"),
            Triple("✏️  Compiti", "Compiti assegnati", "homework"),
            Triple("🕒  Orario", "Orario delle lezioni", "timetable"),
            Triple("📢  Bacheca", "Avvisi scolastici", "bulletin"),
            Triple("✉️  Comunicazioni", "Messaggi scolastici", "communications")
        )

        items.forEach { item ->
            actionCard(content, item.first, item.second) { openPage(item.third) }
        }

        if (currentUser.optString("role") == "admin") {
            sectionTitle(content, "Amministrazione")
            actionCard(content, "👥  Account", "Gestione utenti") { openPage("accounts") }
            actionCard(content, "🏫  Classi", "Gestione classi") { openPage("classes") }
        }
    }

    private fun profile() {
        content.addView(label("Profilo", 27, dark, true))
        space(content, 12)
        card(content) {
            addView(label("Nome utente", 13, muted))
            addView(label(currentUser.optString("username"), 19, dark, true))
            space(this, 12)
            addView(label("Email", 13, muted))
            addView(label(currentUser.optString("email", "Non disponibile"), 16, dark))
            space(this, 12)
            addView(label("Ruolo", 13, muted))
            addView(label(currentUser.optString("role"), 16, dark, true))
        }
        space(content, 16)
        button(content, "Esci dall'account") {
            api("POST", "/api/auth/logout", JSONObject())
            currentUser = JSONObject()
            showLogin()
        }
    }

    private fun accountsPage() {
        if (currentUser.optString("role") != "admin") {
            placeholder("Account", "Accesso riservato all'amministratore.")
            return
        }

        content.addView(label("Gestione account", 26, dark, true))
        space(content, 12)

        sectionTitle(content, "Crea un account")
        val username = input(content, "Nome utente (3-20 caratteri)", false)
        val email = input(content, "Email", false)
        val password = input(content, "Password (minimo 12 caratteri)", true)
        val role = Spinner(this)
        role.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Studente" to "student", "Insegnante" to "teacher", "Amministratore" to "admin")
                .map { it.first }
        )
        content.addView(role)

        button(content, "Crea account") {
            val selectedRole = when (role.selectedItemPosition) {
                1 -> "teacher"
                2 -> "admin"
                else -> "student"
            }

            val data = JSONObject()
                .put("username", username.text.toString().trim())
                .put("email", email.text.toString().trim())
                .put("password", password.text.toString())
                .put("role", selectedRole)

            val result = api("POST", "/api/auth/users", data)
            if (result.first) {
                toast("Account creato!")
                openPage("accounts")
            } else {
                toast(result.second.optString("error", "Errore"))
            }
        }

        space(content, 20)
        sectionTitle(content, "Account registrati")
        val result = api("GET", "/api/auth/users")
        users = result.second.optJSONArray("users") ?: JSONArray()

        for (i in 0 until users.length()) {
            val user = users.optJSONObject(i) ?: continue
            val id = user.optInt("id")
            val usernameValue = user.optString("username")
            val roleValue = user.optString("role")
            val active = user.optInt("active") == 1

            card(content) {
                addView(label(usernameValue, 17, dark, true))
                addView(label(user.optString("email"), 13, muted))
                addView(label("Ruolo: $roleValue · ${if (active) "Attivo" else "Disattivato"}", 13, muted))
                if (roleValue == "student") {
                    val delete = TextView(this@MainActivity)
                    delete.text = "Elimina account studente"
                    delete.setTextColor(Color.RED)
                    delete.setPadding(0, 10, 0, 0)
                    delete.setOnClickListener {
                        val response = api("DELETE", "/api/auth/users/$id")
                        toast(response.second.optString("message", response.second.optString("error", "Operazione completata")))
                        openPage("accounts")
                    }
                    addView(delete)
                }
            }
        }
    }

    private fun classesPage() {
        val role = currentUser.optString("role")
        if (role != "admin" && role != "teacher") {
            placeholder("Classi", "Questa sezione è riservata al personale scolastico.")
            return
        }

        content.addView(label(if (role == "admin") "Gestione classi" else "Le mie classi", 26, dark, true))
        space(content, 12)

        if (role == "admin") {
            sectionTitle(content, "Crea una classe")
            val name = input(content, "Anno (1-5)", false)
            name.inputType = InputType.TYPE_CLASS_NUMBER
            val section = input(content, "Sezione (A-Z)", false)
            val schoolYear = input(content, "Anno scolastico (es. 2026/2027)", false)
            val program = input(content, "Indirizzo di studio", false)

            button(content, "Crea classe") {
                val data = JSONObject()
                    .put("name", name.text.toString().trim())
                    .put("section", section.text.toString().trim())
                    .put("school_year", schoolYear.text.toString().trim())
                    .put("study_program", program.text.toString().trim())

                val response = api("POST", "/api/classes/", data)
                if (response.first) {
                    toast("Classe creata!")
                    openPage("classes")
                } else {
                    toast(response.second.optString("error", "Errore"))
                }
            }
        }

        val result = api("GET", "/api/classes/mine")
        classes = result.second.optJSONArray("classes") ?: JSONArray()

        sectionTitle(content, "Elenco classi")
        if (classes.length() == 0) {
            content.addView(label("Nessuna classe disponibile.", 14, muted))
        }

        for (i in 0 until classes.length()) {
            val item = classes.optJSONObject(i) ?: continue
            val id = item.optInt("id")
            val className = "${item.optString("name")}${item.optString("section")}"
            val year = item.optString("school_year")
            val program = item.optString("study_program")

            card(content) {
                addView(label(className, 21, dark, true))
                addView(label(year, 13, muted))
                if (program.isNotBlank()) addView(label(program, 13, muted))
                if (role == "admin") {
                    button(this, "Assegna studente") { assignmentDialog(id, false) }
                    button(this, "Assegna insegnante") { assignmentDialog(id, true) }
                }
                button(this, "Visualizza studenti") {
                    selectedClassId = id
                    showStudents(id, className)
                }
            }
        }
    }

    private fun assignmentDialog(classId: Int, teacher: Boolean) {
        val result = api("GET", "/api/auth/users")
        val list = result.second.optJSONArray("users") ?: JSONArray()
        val allowed = mutableListOf<JSONObject>()

        for (i in 0 until list.length()) {
            val user = list.optJSONObject(i) ?: continue
            val expected = if (teacher) "teacher" else "student"
            if (user.optString("role") == expected && user.optInt("active") == 1) {
                allowed.add(user)
            }
        }

        if (allowed.isEmpty()) {
            toast(if (teacher) "Non ci sono insegnanti attivi." else "Non ci sono studenti attivi.")
            return
        }

        val names = allowed.map { "${it.optString("username")} (ID ${it.optInt("id")})" }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(20, 10, 20, 10)
        box.addView(label(if (teacher) "Seleziona insegnante" else "Seleziona studente", 16, dark, true))
        box.addView(spinner)

        android.app.AlertDialog.Builder(this)
            .setTitle("Assegnazione alla classe")
            .setView(box)
            .setNegativeButton("Annulla", null)
            .setPositiveButton("Assegna") { _, _ ->
                val id = allowed[spinner.selectedItemPosition].optInt("id")
                val body = JSONObject()
                val path: String
                if (teacher) {
                    body.put("teacherId", id)
                    path = "/api/classes/$classId/teachers"
                } else {
                    body.put("studentId", id)
                    path = "/api/classes/$classId/students"
                }
                val response = api("POST", path, body)
                toast(response.second.optString("message", response.second.optString("error", "Operazione completata")))
            }
            .show()
    }

    private fun showStudents(classId: Int, className: String) {
        val response = api("GET", "/api/classes/$classId/students")
        students = response.second.optJSONArray("students") ?: JSONArray()

        content.removeAllViews()
        content.addView(label("Studenti · $className", 24, dark, true))
        space(content, 12)

        if (students.length() == 0) {
            content.addView(label("Nessuno studente assegnato.", 14, muted))
        }

        for (i in 0 until students.length()) {
            val student = students.optJSONObject(i) ?: continue
            card(content) {
                addView(label(student.optString("username"), 17, dark, true))
                addView(label("ID: ${student.optInt("id")}", 13, muted))
                if (currentUser.optString("role") == "admin" ||
                    currentUser.optString("role") == "teacher") {
                    button(this, "Inserisci un voto") {
                        gradeForm(classId, student.optInt("id"), student.optString("username"))
                    }
                }
            }
        }

        button(content, "Torna alle classi") { openPage("classes") }
    }

    private fun gradesPage() {
        val role = currentUser.optString("role")
        content.addView(label("Valutazioni", 26, dark, true))
        space(content, 12)

        if (role == "admin" || role == "teacher") {
            button(content, "＋ Inserisci un voto") { gradeForm() }
            space(content, 10)

            if (role == "admin") {
                val result = api("GET", "/api/auth/users")
                users = result.second.optJSONArray("users") ?: JSONArray()
                val studentsList = mutableListOf<JSONObject>()
                for (i in 0 until users.length()) {
                    val user = users.optJSONObject(i) ?: continue
                    if (user.optString("role") == "student" && user.optInt("active") == 1) {
                        studentsList.add(user)
                    }
                }
                sectionTitle(content, "Studenti")
                studentsList.forEach { student ->
                    actionCard(
                        content,
                        student.optString("username"),
                        "ID ${student.optInt("id")} · Tocca per consultare i voti"
                    ) {
                        loadGradesForStudent(student.optInt("id"), student.optString("username"))
                    }
                }
            } else {
                val result = api("GET", "/api/classes/mine")
                val teacherClasses = result.second.optJSONArray("classes") ?: JSONArray()
                sectionTitle(content, "Classi assegnate")
                for (i in 0 until teacherClasses.length()) {
                    val c = teacherClasses.optJSONObject(i) ?: continue
                    val id = c.optInt("id")
                    actionCard(
                        content,
                        "${c.optString("name")}${c.optString("section")}",
                        "Seleziona per vedere gli studenti"
                    ) {
                        showStudents(id, "${c.optString("name")}${c.optString("section")}")
                    }
                }
            }
        } else {
            val response = api("GET", "/api/grades/")
            val grades = response.second.optJSONArray("grades") ?: JSONArray()
            showGradeList(content, grades, grades.length())
        }
    }

    private fun gradeForm(
        presetClassId: Int = 0,
        presetStudentId: Int = 0,
        presetStudentName: String = ""
    ) {
        val role = currentUser.optString("role")
        if (role != "admin" && role != "teacher") return

        val classResponse = api("GET", "/api/classes/mine")
        val classArray = classResponse.second.optJSONArray("classes") ?: JSONArray()
        val classItems = mutableListOf<JSONObject>()
        for (i in 0 until classArray.length()) {
            classArray.optJSONObject(i)?.let { classItems.add(it) }
        }

        if (classItems.isEmpty()) {
            toast("Non hai classi disponibili.")
            return
        }

        val classNames = classItems.map {
            "${it.optString("name")}${it.optString("section")} · ${it.optString("school_year")}"
        }
        val classSpinner = Spinner(this)
        classSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, classNames)

        val studentSpinner = Spinner(this)
        val studentItems = mutableListOf<JSONObject>()

        fun loadStudentChoices(classId: Int) {
            studentItems.clear()
            val response = api("GET", "/api/classes/$classId/students")
            val array = response.second.optJSONArray("students") ?: JSONArray()
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { studentItems.add(it) }
            }
            studentSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                if (studentItems.isEmpty()) listOf("Nessuno studente") else studentItems.map { it.optString("username") }
            )
        }

        val initialIndex = classItems.indexOfFirst { it.optInt("id") == presetClassId }
            .let { if (it >= 0) it else 0 }
        classSpinner.setSelection(initialIndex)
        loadStudentChoices(classItems[initialIndex].optInt("id"))

        classSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadStudentChoices(classItems[position].optInt("id"))
            }
        }

        val subject = input(content, "Materia", false)
        val grade = input(content, "Voto (1-10)", false)
        grade.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val date = input(content, "Data (YYYY-MM-DD)", false)
        date.setText(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALY).format(java.util.Date()))
        val description = input(content, "Descrizione (facoltativa)", false)

        content.addView(label("Classe", 13, muted))
        content.addView(classSpinner)
        content.addView(label("Studente", 13, muted))
        content.addView(studentSpinner)
        content.addView(subject)
        content.addView(grade)
        content.addView(date)
        content.addView(description)

        if (presetStudentId > 0) {
            val idx = studentItems.indexOfFirst { it.optInt("id") == presetStudentId }
            if (idx >= 0) studentSpinner.setSelection(idx)
        }

        button(content, "Salva valutazione") {
            if (studentItems.isEmpty()) {
                toast("La classe non contiene studenti.")
                return@button
            }

            val data = JSONObject()
                .put("classId", classItems[classSpinner.selectedItemPosition].optInt("id"))
                .put("studentId", studentItems[studentSpinner.selectedItemPosition].optInt("id"))
                .put("subject", subject.text.toString().trim())
                .put("grade", grade.text.toString().trim().toDoubleOrNull() ?: -1)
                .put("date", date.text.toString().trim())
                .put("description", description.text.toString().trim())

            val response = api("POST", "/api/grades/", data)
            if (response.first) {
                toast("Valutazione salvata!")
                openPage("grades")
            } else {
                toast(response.second.optString("error", "Errore nel salvataggio"))
            }
        }
    }

    private fun loadGradesForStudent(studentId: Int, studentName: String) {
        val response = api("GET", "/api/grades/?studentId=$studentId")
        val grades = response.second.optJSONArray("grades") ?: JSONArray()

        content.removeAllViews()
        content.addView(label("Voti di $studentName", 24, dark, true))
        space(content, 12)
        showGradeList(content, grades, grades.length())
        button(content, "Torna alle valutazioni") { openPage("grades") }
    }

    private fun registroPage() {
        content.addView(label("Registro", 27, dark, true))
        space(content, 12)
        actionCard(content, "📝  Valutazioni", "Consulta i voti") { openPage("grades") }
        actionCard(content, "📋  Assenze e ritardi", "Presenze scolastiche") { openPage("attendance") }
        actionCard(content, "📢  Bacheca", "Avvisi della scuola") { openPage("bulletin") }
    }

    private fun showGradeList(parent: LinearLayout, grades: JSONArray, limit: Int) {
        if (grades.length() == 0) {
            parent.addView(label("Nessuna valutazione disponibile.", 14, muted))
            return
        }

        val start = maxOf(0, grades.length() - limit)
        for (i in start until grades.length()) {
            val item = grades.optJSONObject(i) ?: continue
            val value = item.opt("grade")?.toString() ?: "—"
            val subject = item.optString("subject", "Materia")
            val date = item.optString("date", "")
            val description = item.optString("description", "")

            card(parent) {
                val row = LinearLayout(this@MainActivity)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(
                    label(subject, 16, dark, true),
                    LinearLayout.LayoutParams(0, -2, 1f)
                )
                row.addView(label(value, 22, orange, true))
                addView(row)
                addView(label(date, 12, muted))
                if (description.isNotBlank()) addView(label(description, 13, muted))
            }
        }
    }

    private fun placeholder(name: String, message: String) {
        content.addView(label(name, 27, dark, true))
        space(content, 16)
        card(content) {
            addView(label("Sezione in preparazione", 18, orange, true))
            space(this, 8)
            addView(label(message, 14, muted))
        }
    }

    private fun statCard(
        parent: LinearLayout,
        heading: String,
        value: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(18, 16, 18, 16)
        box.background = rounded(Color.WHITE, 18)
        box.addView(label(heading, 14, muted))
        box.addView(label(value, 29, orange, true))
        box.addView(label(subtitle, 12, muted))
        box.setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 12
        parent.addView(box, params)
    }

    private fun actionCard(
        parent: LinearLayout,
        heading: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(17, 15, 17, 15)
        box.background = rounded(Color.WHITE, 16)
        box.addView(label(heading, 16, dark, true))
        box.addView(label(subtitle, 13, muted))
        box.setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 10
        parent.addView(box, params)
    }

    private fun card(parent: LinearLayout, contentBuilder: LinearLayout.() -> Unit) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(16, 15, 16, 15)
        box.background = rounded(Color.WHITE, 16)
        contentBuilder(box)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 12
        parent.addView(box, params)
    }

    private fun sectionTitle(parent: LinearLayout, text: String) {
        val view = label(text, 18, dark, true)
        view.setPadding(0, 10, 0, 10)
        parent.addView(view)
    }

    private fun label(
        text: String,
        size: Int,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.START
    ): TextView {
        val view = TextView(this)
        view.text = text
        view.textSize = size.toFloat()
        view.setTextColor(color)
        view.gravity = gravity
        if (bold) view.setTypeface(null, Typeface.BOLD)
        view.setPadding(0, 3, 0, 3)
        return view
    }

    private fun input(
        parent: LinearLayout,
        hint: String,
        password: Boolean
    ): EditText {
        val edit = EditText(this)
        edit.hint = hint
        edit.textSize = 15f
        edit.setSingleLine(true)
        edit.setPadding(14, 10, 14, 10)
        edit.background = rounded(Color.rgb(248, 249, 251), 12)
        if (password) {
            edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            edit.inputType = InputType.TYPE_CLASS_TEXT
        }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 10
        parent.addView(edit, params)
        return edit
    }

    private fun button(parent: LinearLayout, text: String, action: () -> Unit) {
        val view = TextView(this)
        view.text = text
        view.textSize = 15f
        view.setTypeface(null, Typeface.BOLD)
        view.gravity = Gravity.CENTER
        view.setTextColor(Color.WHITE)
        view.setPadding(16, 14, 16, 14)
        view.background = rounded(orange, 13)
        view.setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = 10
        parent.addView(view, params)
    }

    private fun space(parent: LinearLayout, height: Int) {
        val view = View(this)
        parent.addView(view, LinearLayout.LayoutParams(1, height))
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
