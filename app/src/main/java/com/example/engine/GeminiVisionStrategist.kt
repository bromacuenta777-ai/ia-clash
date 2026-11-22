package com.example.engine

import android.graphics.Bitmap
import android.util.Log
import com.example.model.DeckArchetype
import com.example.model.GeminiStrategicReport
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiVisionStrategist"

class GeminiVisionStrategist {

    private val strategicSchema = Schema.obj(
        properties = mapOf(
            "detectedPhase" to Schema.enumeration(
                values = listOf("IN_BATTLE", "DOUBLE_ELIXIR", "IDLE_MENU", "MATCH_OVER", "OTHER"),
                description = "Fase actual visible en la captura de pantalla."
            ),
            "estimatedElixir" to Schema.double(
                description = "Elixir actual estimado de 0.0 a 10.0."
            ),
            "enemyArchetype" to Schema.string(
                description = "Arquetipo probable del mazo rival en español (ej. Golem Beatdown, Log Bait, Ciclo 2.6)."
            ),
            "threatLane" to Schema.enumeration(
                values = listOf("LEFT", "RIGHT", "BOTH", "NONE"),
                description = "Línea con mayor amenaza enemiga."
            ),
            "recommendedSlot" to Schema.integer(
                description = "Número de carta en mano recomendada del 1 al 4."
            ),
            "targetCol" to Schema.integer(
                description = "Columna objetivo de la arena (0 izquierda a 17 derecha)."
            ),
            "targetRow" to Schema.integer(
                description = "Fila objetivo en nuestro lado de la arena (16 puente a 29 detrás del rey)."
            ),
            "tacticalReason" to Schema.string(
                description = "Explicación breve y precisa en español de por qué esa jugada es nivel Top 1."
            ),
            "macroAdvice" to Schema.string(
                description = "Consejo macro estratégico para los próximos 15 segundos en español."
            ),
            "confidence" to Schema.enumeration(
                values = listOf("ALTA", "MEDIA", "BAJA"),
                description = "Nivel de confianza del análisis visual."
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = strategicSchema
                temperature = 0.25f
            }
        )
    }

    suspend fun analyzeArenaFrame(
        sourceBitmap: Bitmap,
        selectedDeck: DeckArchetype,
        onDeviceElixirHint: Float
    ): Result<GeminiStrategicReport> {
        val scaledBitmap = scaleBitmapSafely(sourceBitmap, maxDimension = 960)
        return try {
            val promptText = """
                Eres el estratega Top #1 mundial de Clash Royale operando directamente sobre el teléfono.
                Analiza esta captura de pantalla del dispositivo.
                Mazo activo del jugador: ${selectedDeck.displayName} (${selectedDeck.cards.joinToString(", ")}).
                Lectura local aproximada de elixir: ${"%.1f".format(onDeviceElixirHint)}.

                Instrucciones tácticas Top-1:
                - Evalúa la barra de elixir morada abajo, las 4 cartas en mano (1..4 de izquierda a derecha), y las tropas rojas enemigas.
                - Usa la cuadrícula estándar de Clash Royale: columnas 0..17 (izquierda=0..8, derecha=9..17) y filas 16..29 en nuestro lado (puente=16, posición 4-3 anti-Rayo=20, detrás del rey=28).
                - Si la imagen no muestra una partida activa de Clash Royale, indica detectedPhase="OTHER" o "IDLE_MENU" pero entrega igualmente una recomendación útil y clara en español.
            """.trimIndent()

            val response = withTimeout(45_000L) {
                model.generateContent(
                    content {
                        image(scaledBitmap)
                        text(promptText)
                    }
                )
            }

            val rawJson = response.text
            if (rawJson.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini devolvió una respuesta vacía."))
            }

            val json = JSONObject(rawJson)
            val report = GeminiStrategicReport(
                detectedPhase = json.optString("detectedPhase", "OTHER"),
                estimatedElixir = json.optDouble("estimatedElixir", onDeviceElixirHint.toDouble()).coerceIn(0.0, 10.0),
                enemyArchetype = json.optString("enemyArchetype", "Desconocido"),
                threatLane = json.optString("threatLane", "NONE"),
                recommendedSlot = json.optInt("recommendedSlot", 1).coerceIn(1, 4),
                targetCol = json.optInt("targetCol", 9).coerceIn(0, 17),
                targetRow = json.optInt("targetRow", 20).coerceIn(16, 30),
                tacticalReason = json.optString("tacticalReason", "Mantener control de ciclo y ventaja de elixir."),
                macroAdvice = json.optString("macroAdvice", "No desperdiciar elixir y defender con estructuras centradas."),
                confidence = json.optString("confidence", "MEDIA")
            )
            Result.success(report)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini vision analysis timed out", e)
            Result.failure(Exception("Tiempo de espera agotado (45s) al analizar con Gemini Vision."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision analysis failed", e)
            Result.failure(e)
        } finally {
            if (scaledBitmap !== sourceBitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }
    }

    private fun scaleBitmapSafely(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDimension && h <= maxDimension) {
            return bitmap
        }
        val ratio = minOf(maxDimension.toFloat() / w, maxDimension.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
