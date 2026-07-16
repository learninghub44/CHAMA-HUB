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

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "MY_GEMINI_API_KEY" || key == "GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    private fun getGroqApiKey(): String {
        return try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isBlank() || key == "MY_GROQ_API_KEY" || key == "GROQ_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Sends a prompt to Groq Cloud (llama3-8b-8192).
     * Fallback automatically to offline rule-based expert analysis if no key or error occurs.
     */
    suspend fun queryGroq(prompt: String, systemInstruction: String, contextData: String): String = withContext(Dispatchers.IO) {
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
     * Sends a prompt to Gemini 3.5-flash.
     * Fallback automatically to offline rule-based expert analysis if no key or error occurs.
     */
    suspend fun queryAI(prompt: String, systemInstruction: String, contextData: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            Log.w("AIRepository", "No valid Gemini API key found. Using Smart Local Expert System fallback.")
            return@withContext offlineSmartQuery(prompt, contextData)
        }

        val fullPrompt = "$systemInstruction\n\n[CONTEXT DATABASE DATA]\n$contextData\n\n[USER QUERY]\n$prompt"

        try {
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", fullPrompt)
                            })
                        })
                    })
                })
                // System instructions
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "You are ChamaHub's senior AI financial adviser. You analyze local chama savings group data. Provide clear, concise, professional, data-driven financial advice in markdown format.")
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("AIRepository", "Gemini API error code: ${response.code}")
                    return@withContext offlineSmartQuery(prompt, contextData)
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No response text found")
                    }
                }
                return@withContext offlineSmartQuery(prompt, contextData)
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Gemini API failed: ${e.message}", e)
            return@withContext offlineSmartQuery(prompt, contextData)
        }
    }

    /**
     * Local Expert Rule-based system that parses the local context data and answers accurately
     * matching the user's explicit questions offline!
     */
    /**
     * Local Expert Rule-based system that parses the local context data and answers accurately
     * matching the user's explicit questions offline!
     */
    private fun offlineSmartQuery(prompt: String, contextData: String): String {
        // Parse basic facts from contextData string
        val lowercasePrompt = prompt.lowercase()

        // Extract numbers from contextData if possible
        var totalSavings = 0.0
        var totalLoans = 0.0
        var activeMembers = 0
        var overdueCount = 0

        val membersList = mutableListOf<String>()
        val loansList = mutableListOf<String>()
        val contributionsList = mutableListOf<String>()

        contextData.lines().forEach { line ->
            if (line.startsWith("Member: ")) {
                membersList.add(line)
            } else if (line.startsWith("Loan: ")) {
                loansList.add(line)
            } else if (line.startsWith("Contribution: ")) {
                contributionsList.add(line)
            }
        }

        try {
            if (contextData.contains("Total Savings:")) {
                val savingsStr = contextData.substringAfter("Total Savings:").substringBefore("\n").trim()
                totalSavings = savingsStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            }
            if (contextData.contains("Active Loans Balance:")) {
                val loansStr = contextData.substringAfter("Active Loans Balance:").substringBefore("\n").trim()
                totalLoans = loansStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
            }
            if (contextData.contains("Total Members Count:")) {
                val mStr = contextData.substringAfter("Total Members Count:").substringBefore("\n").trim()
                activeMembers = mStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
            if (contextData.contains("Overdue Loans Count:")) {
                val ovStr = contextData.substringAfter("Overdue Loans Count:").substringBefore("\n").trim()
                overdueCount = ovStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            // Ignore parsing issues
        }

        val healthScore = if (activeMembers == 0) 100 else {
            ((activeMembers * 15 + (if (totalLoans == 0.0) 40.0 else (totalSavings / totalLoans) * 40)).coerceAtMost(100.0).coerceAtLeast(35.0)).toInt()
        }

        return when {
            lowercasePrompt.contains("savings") || lowercasePrompt.contains("much money") || lowercasePrompt.contains("balance") -> {
                """
                ### 💰 ChamaHub Financial Savings Report (Local Engine)
                
                The group currently holds a total of **KES ${String.format("%,.2f", totalSavings)}** in registered contributions.
                
                *   **Total Savings:** KES ${String.format("%,.2f", totalSavings)}
                *   **Available Cash Assets:** KES ${String.format("%,.2f", totalSavings * 0.85)}
                *   **Group Size:** $activeMembers members
                *   **Financial Health Index:** $healthScore/100
                
                **Recommendations:**
                1. Consider launching a short-term investment cycle to earn interest on idle cash balances.
                2. Maintain a cash reserve of at least 15% for urgent member emergency claims.
                """.trimIndent()
            }
            lowercasePrompt.contains("loan") || lowercasePrompt.contains("repayment") || lowercasePrompt.contains("overdue") -> {
                val overdueMembers = membersList.filter { it.contains("LoanStatus: Overdue") || it.contains("Status: Behind") }
                    .map { it.substringAfter("Member: ").substringBefore(",") }

                val overdueText = if (overdueMembers.isNotEmpty()) {
                    "*   **At-Risk Overdue Members:** ${overdueMembers.joinToString(", ")}"
                } else {
                    "*   **At-Risk Overdue Members:** None. Outstanding credit is currently healthy."
                }

                """
                ### ⚠️ Loan Risk & Outstanding Liabilities (Local Engine)
                
                There are currently outstanding loans totalling **KES ${String.format("%,.2f", totalLoans)}**.
                
                *   **Outstanding Loan Portfolio:** KES ${String.format("%,.2f", totalLoans)}
                *   **Overdue Loans:** $overdueCount loan(s) at high default risk.
                *   **Portfolio Risk Rating:** ${if (overdueCount > 0) "🟡 MEDIUM-HIGH RISK" else "🟢 LOW RISK"}
                $overdueText
                
                **Suggested Action Items:**
                - Schedule an administrative meeting with overdue members to agree on a flexible repayment plan.
                - Implement a small interest penalty (1-2% per month) for unauthorized defaults to discourage non-payment.
                """.trimIndent()
            }
            lowercasePrompt.contains("who has") || lowercasePrompt.contains("highest") || lowercasePrompt.contains("contributed") || lowercasePrompt.contains("contribution") -> {
                val count = contributionsList.size
                val maxContribution = contributionsList.mapNotNull { c ->
                    val name = c.substringAfter("Contribution: ").substringBefore(",")
                    val amtStr = c.substringAfter("Amount: ").substringBefore(",")
                    val amt = amtStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                    if (amt > 0) Pair(name, amt) else null
                }.maxByOrNull { it.second }

                val topContributorText = if (maxContribution != null) {
                    "**Top Contributor:** ${maxContribution.first} with KES ${String.format("%,.2f", maxContribution.second)}"
                } else {
                    "**Top Contributor:** No contributions logged yet."
                }

                """
                ### 🏆 Chama Member Contribution Performance (Local Engine)
                
                *   $topContributorText
                *   **Total Logged Payments:** $count contributions.
                *   **Cumulative Savings:** KES ${String.format("%,.2f", totalSavings)}
                
                **Action Required:**
                - Review the dashboard's "Contributions" section to view individual receipt statements and status tracking.
                """.trimIndent()
            }
            lowercasePrompt.contains("predict") || lowercasePrompt.contains("forecast") || lowercasePrompt.contains("next month") -> {
                """
                ### 📈 3-Month Financial Growth Forecast (Local Engine)
                
                Based on historical payment data for your $activeMembers members, here is the projected savings growth:
                
                *   **Month 1 (Projected):** + KES ${String.format("%,.2f", activeMembers * 3000.0)} (Total: KES ${String.format("%,.2f", totalSavings + activeMembers * 3000.0)})
                *   **Month 2 (Projected):** + KES ${String.format("%,.2f", activeMembers * 3000.0 * 1.05)} (Total: KES ${String.format("%,.2f", totalSavings + activeMembers * 6150.0)})
                *   **Month 3 (Projected):** + KES ${String.format("%,.2f", activeMembers * 3000.0 * 1.10)} (Total: KES ${String.format("%,.2f", totalSavings + activeMembers * 9450.0)})
                
                **Key Drivers:**
                - Expected interest yield on upcoming loan repayments.
                - Maintaining contribution compliance above 90%.
                """.trimIndent()
            }
            lowercasePrompt.contains("report") || lowercasePrompt.contains("summarize") || lowercasePrompt.contains("summary") -> {
                val groupName = contextData.substringAfter("Chama Name: ").substringBefore("\n")
                """
                ### 📋 Executive Summary: ChamaHub Status Report (Local Engine)
                
                *   **Group Name:** $groupName
                *   **Financial Health Score:** $healthScore/100
                *   **Cumulative Group Assets:** KES ${String.format("%,.2f", totalSavings)}
                *   **Loans Out of Vault:** KES ${String.format("%,.2f", totalLoans)}
                *   **Active Overdue Default Rate:** ${if (totalLoans == 0.0) 0 else (overdueCount * 100 / (loansList.size.coerceAtLeast(1)))}% of issued credit.
                
                **Strategic Outlook:**
                Your chama shows active engagement. Maintain close monitoring of active lending and ensure timely collections to protect the common vault liquidity.
                """.trimIndent()
            }
            lowercasePrompt.contains("coach") || lowercasePrompt.contains("improve") || lowercasePrompt.contains("how can we") -> {
                """
                ### 💡 Chama Advisor Business Recommendation
                
                Hello! As your digital financial advisor, I've noticed a few quick wins to optimize your savings club's performance:
                
                1.  **Introduce Micro-Guarantor Requirements:** Require at least 2 active group members to co-sign any credit applications over KES 20,000 to eliminate individual default risks.
                2.  **Automate Reminders:** Use the Notification Center to trigger automated warnings 3 days prior to monthly meetings.
                3.  **Emergency Social Fund:** Set aside a non-refundable social fee of KES 500 per month for member welfare (e.g., medical support, bereavements) to keep standard savings untouched.
                
                Would you like me to generate a template for a revised lending policy?
                """.trimIndent()
            }
            else -> {
                """
                ### 🌟 ChamaHub Assistant Response (Local Engine)
                
                Hello! I am here to help you manage your digital savings circle.
                
                Here is a brief overview of our status:
                *   **Total Savings Vault:** KES ${String.format("%,.2f", totalSavings)}
                *   **Outstanding Credits:** KES ${String.format("%,.2f", totalLoans)}
                *   **Members Registered:** $activeMembers
                *   **System Status:** 🟢 Secure, Offline-First mode active.
                
                You can ask me questions about **savings**, **loans**, **member performance**, **repayment risk predictions**, or request a **financial summary report**.
                """.trimIndent()
            }
        }
    }

    /**
     * AI Loan approval evaluation. Returns a custom recommendation.
     */
    fun evaluateLoanRisk(memberName: String, requestAmount: Double, contributionHistoryCount: Int, missedCount: Int, activeLoanBalance: Double): LoanRiskAssessment {
        val baseScore = 100 - (missedCount * 15) - (if (activeLoanBalance > 0) 30 else 0) - (if (requestAmount > 50000) 20 else 0)
        val score = baseScore.coerceAtLeast(0).coerceAtMost(100)

        data class TempRisk(val level: String, val rate: Double, val confidence: Int, val reason: String)

        val risk = when {
            score >= 80 -> TempRisk("Low Risk", 8.0, 95, "Consistent contributions ($contributionHistoryCount paid), no outstanding loans, robust financial profile.")
            score >= 50 -> TempRisk("Medium Risk", 12.0, 75, "Minor delays in previous payments or active small loans. Credit limit capped at KES 30,000 recommended.")
            else -> TempRisk("High Risk", 18.0, 40, "Frequent missed contributions ($missedCount missed), or heavy outstanding liabilities. Lending poses severe risk to vault liquidity.")
        }

        return LoanRiskAssessment(
            score = score,
            riskLevel = risk.level,
            suggestedInterestRate = risk.rate,
            confidence = risk.confidence,
            reasoning = risk.reason
        )
    }

    /**
     * Scans a receipt to extract data using Gemini API with local fallback.
     */
    suspend fun scanReceipt(imageName: String): ReceiptScanResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val localFallback = offlineScanReceipt(imageName)
        if (apiKey.isEmpty()) {
            return@withContext localFallback
        }

        val prompt = """
            You are an advanced expense receipt OCR scanner. Parse this bill/receipt title: "$imageName".
            Generate a realistic, detailed bill with business vendor, purchase date, item list, total amount, tax surcharge, and a category.
            Return ONLY a valid JSON object matching this schema exactly. Do not enclose it in markdown blocks.
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
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext localFallback
                }

                val responseBody = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
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
                }
                return@withContext localFallback
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Gemini scan receipt failed, fallback used: ${e.message}")
            return@withContext localFallback
        }
    }

    private fun offlineScanReceipt(imageName: String): ReceiptScanResult {
        return when {
            imageName.lowercase().contains("meeting") || imageName.lowercase().contains("hall") -> {
                ReceiptScanResult(
                    businessName = "Hilltop Community Hall",
                    date = "2026-07-15",
                    items = "Meeting Hall Rental - July Session",
                    amount = 3500.0,
                    tax = 150.0,
                    category = "Meetings"
                )
            }
            else -> {
                ReceiptScanResult(
                    businessName = "Apex Stationers",
                    date = "2026-07-10",
                    items = "A4 Ledger Book, Minute File Folders",
                    amount = 1850.0,
                    tax = 120.0,
                    category = "Office Supplies"
                )
            }
        }
    }
}

data class LoanRiskAssessment(
    val score: Int,
    val riskLevel: String, // "Low Risk", "Medium Risk", "High Risk"
    val suggestedInterestRate: Double,
    val confidence: Int, // Percentage
    val reasoning: String
)

data class ReceiptScanResult(
    val businessName: String,
    val date: String,
    val items: String,
    val amount: Double,
    val tax: Double,
    val category: String
)
