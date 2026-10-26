package com.example.networkrulessummary

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.OutputStreamWriter

class MainActivity : Activity() {
    private data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable?)
    private val categories = listOf("Not verified", "Wi-Fi or mobile data", "Wi-Fi only", "Mobile data only")
    private val prefs by lazy { getSharedPreferences("verified_network_rules", MODE_PRIVATE) }
    private val allApps = mutableListOf<AppEntry>()
    private lateinit var root: LinearLayout
    private lateinit var search: EditText
    private lateinit var counts: TextView
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private var filter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadApps()
        renderApps()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(18), dp(16), dp(24)) }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "Network Rules Summary"; textSize = 25f; setTextColor(0xff183B56.toInt()); setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Samsung Galaxy · guided, read-only inventory"; textSize = 14f; setTextColor(0xff555555.toInt())
            setPadding(0, dp(4), 0, dp(14))
        })

        val info = TextView(this).apply {
            text = "Important: Android does not expose Samsung's Allowed networks choices through a standard public API. Check each app in Samsung Settings, then select the matching category below. Counts include verified entries only. This app does not change network settings."
            textSize = 14f; setTextColor(0xff263238.toInt()); setPadding(dp(12), dp(12), dp(12), dp(12)); setBackgroundColor(0xffEAF2F6.toInt())
        }
        root.addView(info)
        val settingsButton = Button(this).apply {
            text = "Open phone Settings"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Find Allowed networks for apps")
                    .setMessage("In Settings, open Connections → Data usage → Allowed networks for apps. Check an app's selected option, return here, and record the same option in the list. This app cannot read the selection automatically.")
                    .setPositiveButton("Open Settings") { _, _ -> startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    .setNegativeButton("Cancel", null).show()
            }
        }
        root.addView(settingsButton)

        counts = TextView(this).apply { textSize = 15f; setTextColor(0xff183B56.toInt()); setTypeface(null, Typeface.BOLD); setPadding(0, dp(14), 0, dp(12)) }
        root.addView(counts)
        status = TextView(this).apply { textSize = 13f; setTextColor(0xff555555.toInt()); setPadding(0, 0, 0, dp(10)) }
        root.addView(status)

        search = EditText(this).apply {
            hint = "Search installed apps"; singleLine = true
            addTextChangedListener(SimpleTextWatcher { filter = it.trim(); renderApps() })
        }
        root.addView(search)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        actions.addView(Button(this).apply { text = "Export CSV"; setOnClickListener { createCsv() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Button(this).apply { text = "Refresh apps"; setOnClickListener { loadApps(); renderApps() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
    }

    @Suppress("DEPRECATION")
    private fun loadApps() {
        allApps.clear()
        val pm = packageManager
        val installed = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else pm.getInstalledApplications(0)
        installed.mapNotNull { ai ->
            try {
                val label = pm.getApplicationLabel(ai).toString().ifBlank { ai.packageName }
                AppEntry(label, ai.packageName, pm.getApplicationIcon(ai))
            } catch (_: Exception) { null }
        }.sortedBy { it.label.lowercase() }.forEach { allApps.add(it) }
    }

    private fun savedCategory(pkg: String): String = prefs.getString(pkg, "Not verified") ?: "Not verified"
    private fun updateCounts() {
        val values = allApps.map { savedCategory(it.packageName) }
        val both = values.count { it == categories[1] }
        val wifi = values.count { it == categories[2] }
        val mobile = values.count { it == categories[3] }
        val unknown = values.count { it == categories[0] }
        counts.text = "Wi-Fi or mobile data: $both\nWi-Fi only: $wifi\nMobile data only: $mobile\nNot verified: $unknown"
        status.text = "${allApps.size} apps visible · ${allApps.size - unknown} manually verified · Counts are based only on entries you recorded."
    }

    private fun renderApps() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        updateCounts()
        val shown = allApps.filter { it.label.contains(filter, true) || it.packageName.contains(filter, true) }
        shown.forEach { app ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }
            val icon = ImageView(this).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER }
            row.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
            val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(6), 0) }
            names.addView(TextView(this).apply { text = app.label; textSize = 15f; setTextColor(0xff202124.toInt()); maxLines = 2 })
            names.addView(TextView(this).apply { text = app.packageName; textSize = 10f; setTextColor(0xff777777.toInt()); maxLines = 1 })
            row.addView(names, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val spinner = Spinner(this)
            spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
            spinner.setSelection(categories.indexOf(savedCategory(app.packageName)).coerceAtLeast(0))
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selected = categories[position]
                    if (savedCategory(app.packageName) != selected) {
                        prefs.edit().putString(app.packageName, selected).apply()
                        updateCounts()
                    }
                }
            }
            row.addView(spinner, LinearLayout.LayoutParams(dp(155), ViewGroup.LayoutParams.WRAP_CONTENT))
            list.addView(row)
            val divider = View(this).apply { setBackgroundColor(0xffE4E7EB.toInt()) }
            list.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        }
        if (shown.isEmpty()) list.addView(TextView(this).apply { text = "No matching apps."; setPadding(0, dp(16), 0, dp(16)) })
    }

    private fun createCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "text/csv"; putExtra(Intent.EXTRA_TITLE, "network_rules_summary.csv")
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 2001)
    }

    @Deprecated("Handled for Android document picker compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2001 || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { out ->
                    out.write("App name,Package name,Recorded network category,Verification status\r\n")
                    allApps.forEach { app ->
                        val cat = savedCategory(app.packageName)
                        out.write(listOf(app.label, app.packageName, cat, if (cat == "Not verified") "Not verified" else "User recorded").joinToString(",") { csv(it) } + "\r\n")
                    }
                }
            }
            Toast.makeText(this, "CSV exported", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

private class SimpleTextWatcher(private val onChange: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange(s?.toString() ?: "") }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
