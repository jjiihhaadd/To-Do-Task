package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.api.*
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tasks", appName)
  }

  @Test
  fun `test gemini api call`() = runBlocking {
      val apiKey = BuildConfig.GEMINI_API_KEY
      val request = GenerateContentRequest(
          contents = listOf(Content(parts = listOf(Part(text = "Hello")))),
          generationConfig = GenerationConfig(thinkingConfig = ThinkingConfig("HIGH"), responseMimeType = "application/json")
      )
      try {
          val response = RetrofitClient.service.generateContent("gemini-3.1-pro-preview", apiKey, request)
          val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No text"
          println("API_RESPONSE_SUCCESS: $text")
      } catch (e: retrofit2.HttpException) {
          println("API_RESPONSE_ERROR_BODY: ${e.response()?.errorBody()?.string()}")
      } catch (e: Exception) {
          println("API_RESPONSE_ERROR: ${e.message}")
          e.printStackTrace()
      }
  }
}
