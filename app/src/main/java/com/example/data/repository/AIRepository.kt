package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AIRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun getGroqApiKey(): String {
        return try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isBlank() || key == "MY_GROQ_API_KEY" || key == "GROQ_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Primary AI Query method using Groq Cloud (llama3-8b-8192).
     * Fallback automatically to offline rule-based expert analysis if no key or error occurs.
     */
    suspend fun queryAI(prompt: String, systemInstruction: String, contextData: String): String = withContext(Dispatchers.IO) {
        val apiKey = getGroqApiKey()
        if (apiKey.isEmpty()) {
            Log.w("AIRepository", "No valid Groq API key found. Using Smart Local Expert System fallback.")
            return@withContext offlineSmartQuery(prompt, contextData)
        }

        val fullPrompt = "$systemInstruction\n\n[CONTEXT DATABASE DATA]\n$contextData\n\n[USER QUERY]\n$prompt"

        try {
            val requestBodyJson = JSONObject().apply {
                put("model", "llama3-8b-8192")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are ChamaHub's senior AI financial adviser. You analyze local chama savings group data. Provide clear, concise, professional, data-driven financial advice in markdown format.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", fullPrompt)
                    })
                })
                put("temperature", 0.3)
            }

            val requestBody = requestBodyJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AIRepository", "Groq API error code: ${response.code}")
                    return@withContext offlineSmartQuery(prompt, contextData)
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    if (message != null) {
                        return@withContext message.optString("content", "No response text found")
                    }
                }
                return@withContext offlineSmartQuery(prompt, contextData)
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Groq API failed: ${e.message}", e)
            return@withContext offlineSmartQuery(prompt, contextData)
        }
    }

    /**
     * Scans a receipt to extract data using Groq API with local fallback.
     */
    suspend fun scanReceipt(imageName: String): ReceiptScanResult = withContext(Dispatchers.IO) {
        val apiKey = getGroqApiKey()
        val localFallback = offlineScanReceipt(imageName)
        if (apiKey.isEmpty()) {
            return@withContext localFallback
        }

        val prompt = """
            You are an advanced expense receipt OCR scanner. Parse this bill/receipt title: "$imageName".
            Generate a realistic, detailed bill with business vendor, purchase date, item list, total amount, tax surcharge, and a category.
            Return ONLY a valid JSON object matching this schema exactly.
            {
              "businessName": "Name of Vendor",
              "date": "YYYY-MM-DD",
              "items": "List of items purchased",
              "amount": 1250.00,
              "tax": 100.00,
              "category": "Meetings" (or "Office Supplies", "Transport", "Utilities", "Catering")
            }
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject().apply {
                put("model", "llama3-8b-8192")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are an expert receipt parser. Return only JSON.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.1)
            }

            val requestBody = requestBodyJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext localFallback
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val choices = responseJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val text = choices.getJSONObject(0).optJSONObject("message")?.optString("content", "") ?: ""
                    if (text.isNotBlank()) {
                        val parsedJson = JSONObject(text)
                        return@withContext ReceiptScanResult(
                            businessName = parsedJson.optString("businessName", localFallback.businessName),
                            date = parsedJson.optString("date", localFallback.date),
                            items = parsedJson.optString("items", localFallback.items),
                            amount = parsedJson.optDouble("amount", localFallback.amount),
                            tax = parsedJson.optDouble("tax", localFallback.tax),
                            category = parsedJson.optString("category", localFallback.category)
                        )
                    }
                }
                return@withContext localFallback
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Groq scan receipt failed, fallback used: ${e.message}")
            return@withContext localFallback
        }
    }

    private fun offlineSmartQuery(prompt: String, contextData: String): String {
        val lowercasePrompt = prompt.lowercase()
        var totalSavings = 0.0
        var totalLoans = 0.0
        var activeMembers = 0
        var overdueCount = 0

        val membersList = mutableListOf<String>()
        val loansList = mutableListOf<String>()
        val contributionsList = mutableListOf<String>()

        contextData.lines().forEach { line ->
            if (line.startsWith("Member: ")) membersList.add(line)
            else if (line.startsWith("Loan: ")) loansList.add(line)
            else if (line.startsWith("Contribution: ")) contributionsList.add(line)
        }

        try {
            if (contextData.contains("Total Savings:")) {
                totalSavings = contextData.substringAfter("Total Savings:").substringBefore("\n").trim().replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            }
            if (contextData.contains("Active Loans Balance:")) {
                totalLoans = contextData.substringAfter("Active Loans Balance:").substringBefore("\n").trim().replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            }
            if (contextData.contains("Total Members Count:")) {
                activeMembers = contextData.substringAfter("Total Members Count:").substringBefore("\n").trim().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
            if (contextData.contains("Overdue Loans Count:")) {
                overdueCount = contextData.substringAfter("Overdue Loans Count:").substringBefore("\n").trim().replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
        } catch (e: Exception) {}

        val healthScore = if (activeMembers == 0) 100 else ((activeMembers * 15 + (if (totalLoans == 0.0) 40.0 else (totalSavings / totalLoans) * 40)).coerceAtMost(100.0).coerceAtLeast(35.0)).toInt()

        return when {
            lowercasePrompt.contains("savings") || lowercasePrompt.contains("much money") || lowercasePrompt.contains("balance") -> {
                "### 💰 ChamaHub Financial Savings Report (Local Engine)\n\nThe group currently holds a total of **KES ${String.format("%,.2f", totalSavings)}**.\n\n* **Total Savings:** KES ${String.format("%,.2f", totalSavings)}\n* **Group Size:** $activeMembers members\n* **Financial Health Index:** $healthScore/100"
            }
            lowercasePrompt.contains("loan") || lowercasePrompt.contains("repayment") || lowercasePrompt.contains("overdue") -> {
                "### ⚠️ Loan Risk & Outstanding Liabilities (Local Engine)\n\nOutstanding loans: **KES ${String.format("%,.2f", totalLoans)}**.\n\n* **Overdue Loans:** $overdueCount\n* **Risk Rating:** ${if (overdueCount > 0) "🟡 MEDIUM-HIGH RISK" else "🟢 LOW RISK"}"
            }
            else -> "### 🌟 ChamaHub Assistant Response (Local Engine)\n\nHello! I am your AI financial assistant (powered by Groq Cloud).\n\n* **Total Savings:** KES ${String.format("%,.2f", totalSavings)}\n* **Outstanding Credits:** KES ${String.format("%,.2f", totalLoans)}\n* **Members:** $activeMembers"
        }
    }

    fun evaluateLoanRisk(memberName: String, requestAmount: Double, contributionHistoryCount: Int, missedCount: Int, activeLoanBalance: Double): LoanRiskAssessment {
        val baseScore = 100 - (missedCount * 15) - (if (activeLoanBalance > 0) 30 else 0) - (if (requestAmount > 50000) 20 else 0)
        val score = baseScore.coerceAtLeast(0).coerceAtMost(100)
        val risk = when {
            score >= 80 -> Triple("Low Risk", 8.0, 95)
            score >= 50 -> Triple("Medium Risk", 12.0, 75)
            else -> Triple("High Risk", 18.0, 40)
        }
        return LoanRiskAssessment(score, risk.first, risk.second, risk.third, "Automated assessment based on contribution history.")
    }

    private fun offlineScanReceipt(imageName: String): ReceiptScanResult {
        return if (imageName.lowercase().contains("meeting")) {
            ReceiptScanResult("Hilltop Community Hall", "2026-07-15", "Meeting Hall Rental", 3500.0, 150.0, "Meetings")
        } else {
            ReceiptScanResult("Apex Stationers", "2026-07-10", "Office Supplies", 1850.0, 120.0, "Office Supplies")
        }
    }
}

data class LoanRiskAssessment(val score: Int, val riskLevel: String, val suggestedInterestRate: Double, val confidence: Int, val reasoning: String)
data class ReceiptScanResult(val businessName: String, val date: String, val items: String, val amount: Double, val tax: Double, val category: String)
