package com.arbon.vata.floralens.data.plantnet

import android.graphics.Bitmap
import com.arbon.vata.floralens.domain.species.SpeciesIdentifier
import com.arbon.vata.floralens.domain.species.SpeciesMap
import com.arbon.vata.floralens.domain.species.SpeciesMatch
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Pl@ntNet v2 "identify all" client (free tier: 500 requests/day). Results below
 * [MIN_SCORE] are treated as no match. Any failure returns null so the scan
 * pipeline degrades to the on-device verdict instead of crashing.
 */
class PlantNetClient(
    private val client: HttpClient,
    private val apiKey: String,
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
) : SpeciesIdentifier {
    override suspend fun identify(photo: Bitmap): SpeciesMatch? {
        // Blank key = local.properties has no PLANTNET_API_KEY yet; scans stay
        // fully offline rather than failing a network call on every capture.
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
                client.post(
                    URLBuilder(IDENTIFY_URL).apply {
                        parameters.append("api-key", apiKey)
                    }.build(),
                ) {
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
                                append("organs", "leaf")
                            },
                        ),
                    )
                }
            val response = json.decodeFromString<PlantNetResponse>(body.bodyAsText())
            response.toSpeciesMatch()
        } catch (_: Exception) {
            // The second opinion is strictly optional: offline, rate-limited or a
            // changed response shape must degrade to the on-device verdict, never
            // break the scan.
            null
        }
    }

    private fun PlantNetResponse.toSpeciesMatch(): SpeciesMatch? {
        val best = results.maxByOrNull { it.score } ?: return null
        // Below this the API mostly guesses; treating it as "no match" keeps the
        // wishlist redirect from firing on noise.
        if (best.score < MIN_SCORE) return null
        val scientificName = best.species.scientificName
        val mappedCrop = SpeciesMap.match(scientificName)
        val commonName =
            mappedCrop
                ?: best.species.commonNames.firstOrNull { it.isNotBlank() }
                    ?.trim()
                    ?.replaceFirstChar { it.uppercase() }
                ?: scientificName
        return SpeciesMatch(
            commonName = commonName,
            scientificName = scientificName,
            confidence = best.score,
            isSupported = mappedCrop != null && SpeciesMap.isSupportedCrop(mappedCrop),
        )
    }

    private companion object {
        const val IDENTIFY_URL = "https://my-api.plantnet.org/v2/identify/all"

        /** 90 keeps leaf-detail quality for identification at a modest upload size. */
        const val JPEG_QUALITY = 90
        const val MIN_SCORE = 0.2f
    }
}