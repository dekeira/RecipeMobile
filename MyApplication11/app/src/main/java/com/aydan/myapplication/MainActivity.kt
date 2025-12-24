package com.aydan.myapplication

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // ===== КЛАСС РЕЦЕПТА =====
    data class Recipe(
        var id: Long = System.currentTimeMillis(),
        var title: String = "",
        var ingredients: String = "",
        var instructions: String = "",
        var category: String = "Основные блюда",
        var cookingTime: Int = 30,
        var author: String = "",
        var createdAt: Long = System.currentTimeMillis(),
        var isFavorite: Boolean = false
    ) {
        val formattedDate: String
            get() {
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                return sdf.format(Date(createdAt))
            }

        val formattedTime: String
            get() = when {
                cookingTime < 60 -> "$cookingTime мин"
                else -> "${cookingTime / 60} ч ${cookingTime % 60} мин"
            }
    }

    // ===== ПЕРЕМЕННЫЕ =====
    private lateinit var prefs: SharedPreferences
    private lateinit var currentUser: String
    private var recipes = mutableListOf<Recipe>()

    // UI элементы
    private lateinit var mainLayout: LinearLayout
    private lateinit var welcomeText: TextView
    private lateinit var searchInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var recipesListView: ListView

    // Категории рецептов
    private val categories = listOf(
        "Все рецепты", "Завтраки", "Основные блюда",
        "Десерты", "Напитки", "Закуски", "Избранное"
    )

    // Тестовые пользователи
    private val testUsers = mapOf(
        "admin" to "1234",
        "chef" to "cook123",
        "user" to "password"
    )

    // ===== ОСНОВНЫЕ МЕТОДЫ =====

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("cookbook", MODE_PRIVATE)

        // Проверяем авторизацию
        if (!prefs.getBoolean("is_logged_in", false)) {
            showLoginScreen()
        } else {
            currentUser = prefs.getString("username", "Гость") ?: "Гость"
            loadRecipes()
            showMainScreen()
        }
    }

    // ===== ЭКРАН ВХОДА/РЕГИСТРАЦИИ =====

    private fun showLoginScreen() {
        // Очищаем экран
        if (::mainLayout.isInitialized) mainLayout.removeAllViews()

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Заголовок
        val title = TextView(this).apply {
            text = "🍳 Книга рецептов"
            textSize = 28f
            setTextColor(Color.parseColor("#FF5722"))
            setPadding(0, 0, 0, 50)
        }

        // Поля ввода
        val usernameInput = EditText(this).apply {
            hint = "Логин"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val passwordInput = EditText(this).apply {
            hint = "Пароль (мин 4 символа)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 30 }
        }

        // Кнопка входа
        val loginBtn = Button(this).apply {
            text = "ВОЙТИ"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }

            setOnClickListener {
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    showToast("Заполните все поля")
                    return@setOnClickListener
                }

                if (testUsers[username] == password) {
                    saveUserData(username)
                    showToast("Добро пожаловать, $username!")
                    currentUser = username
                    loadRecipes()
                    showMainScreen()
                } else {
                    showToast("Неверный логин или пароль")
                }
            }
        }

        // Кнопка регистрации
        val registerBtn = Button(this).apply {
            text = "ЗАРЕГИСТРИРОВАТЬСЯ"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }

            setOnClickListener {
                val username = usernameInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (username.isEmpty() || password.isEmpty()) {
                    showToast("Заполните все поля")
                    return@setOnClickListener
                }

                if (password.length < 4) {
                    showToast("Пароль должен быть минимум 4 символа")
                    return@setOnClickListener
                }

                // Сохраняем нового пользователя
                saveUserData(username)
                showToast("Регистрация успешна!")
                currentUser = username
                addTestRecipes()
                showMainScreen()
            }
        }

        // Добавление тестовых рецептов (только для первого запуска)
        val addTestBtn = Button(this).apply {
            text = "ДОБАВИТЬ ТЕСТОВЫЕ РЕЦЕПТЫ"
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 30 }

            setOnClickListener {
                addTestRecipes()
                showToast("Тестовые рецепты добавлены")
            }
        }

        // Собираем интерфейс
        mainLayout.addView(title)
        mainLayout.addView(usernameInput)
        mainLayout.addView(passwordInput)
        mainLayout.addView(loginBtn)
        mainLayout.addView(registerBtn)
        mainLayout.addView(addTestBtn)

        setContentView(mainLayout)
    }

    // ===== ГЛАВНЫЙ ЭКРАН =====

    private fun showMainScreen() {
        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Приветствие
        welcomeText = TextView(this).apply {
            text = "👨‍🍳 Привет, $currentUser!"
            textSize = 20f
            setPadding(0, 0, 0, 20)
        }

        // Поиск
        searchInput = EditText(this).apply {
            hint = "🔍 Поиск рецептов..."
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }

            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    filterRecipes()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Категории
        categorySpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                categories
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    filterRecipes()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        // Кнопки
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val addRecipeBtn = Button(this).apply {
            text = "➕ ДОБАВИТЬ"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { rightMargin = 10 }

            setOnClickListener { showAddRecipeDialog() }
        }

        val statsButton = Button(this).apply {
            text = "📊 СТАТИСТИКА"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

            setOnClickListener { showStatistics() }
        }

        val logoutBtn = Button(this).apply {
            text = "🚪 ВЫЙТИ"
            setBackgroundColor(Color.parseColor("#F44336"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { leftMargin = 10 }

            setOnClickListener {
                prefs.edit().putBoolean("is_logged_in", false).apply()
                showLoginScreen()
            }
        }

        buttonsLayout.addView(addRecipeBtn)
        buttonsLayout.addView(statsButton)
        buttonsLayout.addView(logoutBtn)

        // Список рецептов
        recipesListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply { topMargin = 10 }

            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_list_item_1,
                recipes.map {
                    val favoriteIcon = if (it.isFavorite) "❤️ " else ""
                    "🍽 $favoriteIcon${it.title} (${it.category})"
                }
            )

            // Просмотр рецепта
            setOnItemClickListener { _, _, position, _ ->
                showRecipeDetails(position)
            }

            // Удаление рецепта (долгое нажатие)
            setOnItemLongClickListener { _, _, position, _ ->
                if (recipes[position].author == currentUser) {
                    showDeleteDialog(position)
                    true
                } else {
                    showToast("Вы можете удалять только свои рецепты")
                    true
                }
            }
        }

        // Собираем интерфейс
        mainLayout.addView(welcomeText)
        mainLayout.addView(searchInput)
        mainLayout.addView(categorySpinner)
        mainLayout.addView(buttonsLayout)
        mainLayout.addView(recipesListView)

        setContentView(mainLayout)
    }

    // ===== ДИАЛОГИ И ФУНКЦИОНАЛ =====

    private fun showAddRecipeDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val titleInput = EditText(this).apply {
            hint = "Название рецепта"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }
        }

        val categoryInput = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                categories.filter { it != "Все рецепты" && it != "Избранное" }
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }
        }

        val timeInput = EditText(this).apply {
            hint = "Время приготовления (минуты)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }
        }

        val ingredientsInput = EditText(this).apply {
            hint = "Ингредиенты (через запятую)"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }
        }

        val instructionsInput = EditText(this).apply {
            hint = "Инструкция приготовления"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                200
            )
        }

        dialogView.addView(titleInput)
        dialogView.addView(categoryInput)
        dialogView.addView(timeInput)
        dialogView.addView(ingredientsInput)
        dialogView.addView(instructionsInput)

        AlertDialog.Builder(this)
            .setTitle("НОВЫЙ РЕЦЕПТ")
            .setView(dialogView)
            .setPositiveButton("СОХРАНИТЬ") { _, _ ->
                val title = titleInput.text.toString().trim()
                val category = categoryInput.selectedItem.toString()
                val time = timeInput.text.toString().toIntOrNull() ?: 30
                val ingredients = ingredientsInput.text.toString().trim()
                val instructions = instructionsInput.text.toString().trim()

                if (title.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
                    showToast("Заполните обязательные поля")
                    return@setPositiveButton
                }

                val newRecipe = Recipe(
                    id = System.currentTimeMillis(),
                    title = title,
                    ingredients = ingredients,
                    instructions = instructions,
                    category = category,
                    cookingTime = time,
                    author = currentUser
                )

                recipes.add(newRecipe)
                saveRecipes()
                filterRecipes()
                showToast("Рецепт добавлен!")
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun showRecipeDetails(position: Int) {
        val recipe = recipes[position]

        val details = """
            🍽 ${recipe.title}
            
            📋 Категория: ${recipe.category}
            ⏱ Время: ${recipe.formattedTime}
            👨‍🍳 Автор: ${recipe.author}
            📅 Добавлено: ${recipe.formattedDate}
            
            🛒 Ингредиенты:
            ${recipe.ingredients}
            
            👨‍🍳 Приготовление:
            ${recipe.instructions}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(recipe.title)
            .setMessage(details)
            .setPositiveButton("ОК", null)
            .setNeutralButton(if (recipe.isFavorite) "❤️ УБРАТЬ ИЗ ИЗБРАННОГО" else "🤍 В ИЗБРАННОЕ") { _, _ ->
                recipe.isFavorite = !recipe.isFavorite
                saveRecipes()
                filterRecipes()
                showToast(if (recipe.isFavorite) "Добавлено в избранное" else "Убрано из избранного")
            }
            .show()
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("УДАЛЕНИЕ РЕЦЕПТА")
            .setMessage("Вы уверены, что хотите удалить рецепт '${recipes[position].title}'?")
            .setPositiveButton("УДАЛИТЬ") { _, _ ->
                recipes.removeAt(position)
                saveRecipes()
                filterRecipes()
                showToast("Рецепт удален")
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun showStatistics() {
        if (recipes.isEmpty()) {
            showToast("Нет рецептов для статистики")
            return
        }

        val totalRecipes = recipes.size
        val myRecipes = recipes.count { it.author == currentUser }
        val favoriteRecipes = recipes.count { it.isFavorite }
        val categoriesCount = recipes.groupBy { it.category }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        val statsText = StringBuilder()
        statsText.append("📊 СТАТИСТИКА РЕЦЕПТОВ\n\n")
        statsText.append("Всего рецептов: $totalRecipes\n")
        statsText.append("Мои рецепты: $myRecipes\n")
        statsText.append("В избранном: $favoriteRecipes\n\n")
        statsText.append("По категориям:\n")

        categoriesCount.forEach { (category, count) ->
            statsText.append("• $category: $count\n")
        }

        AlertDialog.Builder(this)
            .setTitle("СТАТИСТИКА")
            .setMessage(statsText.toString())
            .setPositiveButton("ОК", null)
            .show()
    }

    // ===== ФИЛЬТРАЦИЯ И ПОИСК =====

    private fun filterRecipes() {
        val searchQuery = searchInput.text.toString().lowercase()
        val selectedCategory = categorySpinner.selectedItem.toString()

        var filteredRecipes = recipes

        // Фильтр по поиску
        if (searchQuery.isNotEmpty()) {
            filteredRecipes = filteredRecipes.filter {
                it.title.lowercase().contains(searchQuery) ||
                        it.ingredients.lowercase().contains(searchQuery) ||
                        it.category.lowercase().contains(searchQuery)
            }.toMutableList()
        }

        // Фильтр по категории
        filteredRecipes = when (selectedCategory) {
            "Все рецепты" -> filteredRecipes
            "Избранное" -> filteredRecipes.filter { it.isFavorite }.toMutableList()
            else -> filteredRecipes.filter { it.category == selectedCategory }.toMutableList()
        }

        // Обновляем список
        recipesListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            filteredRecipes.map {
                val favoriteIcon = if (it.isFavorite) "❤️ " else ""
                "🍽 ${favoriteIcon}${it.title} (${it.category})"
            }
        )
    }

    // ===== РАБОТА С ДАННЫМИ =====

    private fun saveUserData(username: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("username", username)
            apply()
        }
    }

    private fun saveRecipes() {
        // В реальном приложении здесь была бы база данных
        // Для демо просто сохраняем в SharedPreferences в виде строки
        val recipesString = recipes.joinToString("|") {
            "${it.title};${it.ingredients};${it.instructions};${it.category};${it.author};${it.isFavorite}"
        }
        prefs.edit().putString("recipes_$currentUser", recipesString).apply()
    }

    private fun loadRecipes() {
        val recipesString = prefs.getString("recipes_$currentUser", "")
        recipes.clear()

        if (recipesString?.isNotEmpty() == true) {
            val recipeStrings = recipesString.split("|")
            for (recipeStr in recipeStrings) {
                val parts = recipeStr.split(";")
                if (parts.size >= 6) {
                    recipes.add(Recipe(
                        title = parts[0],
                        ingredients = parts[1],
                        instructions = parts[2],
                        category = parts[3],
                        author = parts[4],
                        isFavorite = parts[5].toBoolean()
                    ))
                }
            }
        }

        // Если рецептов нет, добавляем тестовые
        if (recipes.isEmpty() && currentUser == "admin") {
            addTestRecipes()
        }
    }

    private fun addTestRecipes() {
        val testRecipes = listOf(
            Recipe(
                title = "Омлет с сыром",
                ingredients = "Яйца - 3 шт, Молоко - 50 мл, Сыр - 50 г, Соль, Перец",
                instructions = "1. Взбить яйца с молоком\n2. Добавить соль и перец\n3. Вылить на сковороду\n4. Посыпать сыром\n5. Жарить 5-7 минут",
                category = "Завтраки",
                cookingTime = 15,
                author = "admin"
            ),
            Recipe(
                title = "Борщ",
                ingredients = "Свекла - 2 шт, Картофель - 3 шт, Капуста - 200 г, Мясо - 300 г, Сметана",
                instructions = "1. Сварить мясной бульон\n2. Добавить нарезанные овощи\n3. Варить 40 минут\n4. Подавать со сметаной",
                category = "Основные блюда",
                cookingTime = 60,
                author = "chef"
            ),
            Recipe(
                title = "Шоколадный торт",
                ingredients = "Мука - 200 г, Какао - 50 г, Яйца - 4 шт, Сахар - 150 г, Сливочное масло - 100 г",
                instructions = "1. Смешать сухие ингредиенты\n2. Добавить яйца и масло\n3. Выпекать 30 минут при 180°C\n4. Украсить кремом",
                category = "Десерты",
                cookingTime = 45,
                author = "admin",
                isFavorite = true
            )
        )

        recipes.addAll(testRecipes)
        saveRecipes()
        if (::recipesListView.isInitialized) {
            filterRecipes()
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}