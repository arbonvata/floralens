package com.arbon.vata.floralens.data.plantid

import android.graphics.Bitmap
import com.arbon.vata.floralens.domain.diagnosis.DiseaseAssessment
import com.arbon.vata.floralens.domain.diagnosis.DiseaseVerifier
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Plant.id v3 health-assessment client (100 free calls/month). Auth is the
 * `Api-Key` header. Any failure returns null so the scan pipeline falls back
 * to the on-device verdict instead of breaking.
 */
class PlantIdClient(
    private val client: HttpClient,
    private val apiKey: String,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
) : DiseaseVerifier {
    override suspend fun assess(photo: Bitmap): DiseaseAssessment? {
        // Blank key = no PLANT_ID_API_KEY in local.properties; the disease
        // second opinion silently stays off while everything else works.
        if (apiKey.isBlank()) return null
        return try {
            val jpeg =
                withContext(Dispatchers.IO) {
                    ByteArrayOutputStream().use { out ->
                        photo.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                        out.toByteArray()
                    }
                }
            val body =
                client.post(HEALTH_URL) {
                    header("Api-Key", apiKey)
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "images",
                                    jpeg,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=\"leaf.jpg\"")
                                    },
                                )
                            },
                        ),
                    )
                }
            json.decodeFromString<PlantIdResponse>(body.bodyAsText()).toAssessment()
        } catch (_: Exception) {
            // Optional-by-design stage: offline, quota exhausted or an unexpected
            // payload shape must degrade to the on-device verdict, never crash.
            null
        }
    }

    private companion object {
        const val HEALTH_URL = "https://plant.id/api/v3/health_assessment"
        const val JPEG_QUALITY = 90
    }
}