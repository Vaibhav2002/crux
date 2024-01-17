package com.chimbori.crux_kmp.plugins

import com.chimbori.crux_kmp.api.Extractor
import com.chimbori.crux_kmp.api.Fields.BACKGROUND_COLOR_HEX
import com.chimbori.crux_kmp.api.Fields.BACKGROUND_COLOR_HTML
import com.chimbori.crux_kmp.api.Fields.DISPLAY
import com.chimbori.crux_kmp.api.Fields.FAVICON_URL
import com.chimbori.crux_kmp.api.Fields.LANGUAGE
import com.chimbori.crux_kmp.api.Fields.ORIENTATION
import com.chimbori.crux_kmp.api.Fields.THEME_COLOR_HEX
import com.chimbori.crux_kmp.api.Fields.THEME_COLOR_HTML
import com.chimbori.crux_kmp.api.Fields.TITLE
import com.chimbori.crux_kmp.api.Fields.WEB_APP_MANIFEST_URL
import com.chimbori.crux_kmp.api.Resource
import com.chimbori.crux_kmp.common.httpGetContent
import com.chimbori.crux_kmp.common.isLikelyArticle
import com.chimbori.crux_kmp.common.nullIfBlank
import com.chimbori.crux_kmp.common.toUrlOrNull
import com.chimbori.crux_kmp.extractors.extractCanonicalUrl
import com.chimbori.crux_kmp.extractors.parseSize
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class WebAppManifestParser(private val httpClient: HttpClient) : Extractor {
    override fun canExtract(url: Url): Boolean = url.isLikelyArticle()

    override suspend fun extract(request: Resource): Resource? {
        val canonicalUrl = request.document?.extractCanonicalUrl()
            ?.let { request.url?.let { url -> URLBuilder(url).takeFrom(it).build() } }
            ?: request.url
        val webAppManifestUrl =
            request.document?.select("link[rel=manifest]")?.attr("abs:href")?.nullIfBlank()
                ?.let { canonicalUrl?.let { url -> URLBuilder(url).takeFrom(it).build() } ?: it.toUrlOrNull() }
                ?: return null

        val manifest: JsonObject? = httpClient.httpGetContent(webAppManifestUrl)?.let { rawJSON ->
            try {
                Json.decodeFromString<JsonObject>(rawJSON)
            } catch (t: Throwable) {
                // Silently ignore all JSON errors, since they are not recoverable.
                null
            }
        }

        val themeColorHtml = manifest.element("theme_color")
        val backgroundColorHtml = manifest.element("background_color")
        return Resource(
            metadata = mapOf(
                WEB_APP_MANIFEST_URL to webAppManifestUrl,
                TITLE to manifest.element("name"),
                LANGUAGE to manifest.element("lang"),
                DISPLAY to manifest.element("display"),
                ORIENTATION to manifest.element("orientation"),
                FAVICON_URL to getLargestIconUrl(
                    webAppManifestUrl,
                    manifest?.get("icons") as JsonArray
                ),
                (if (themeColorHtml?.startsWith("#") == true) THEME_COLOR_HEX else THEME_COLOR_HTML) to themeColorHtml,
                (if (backgroundColorHtml?.startsWith("#") == true) BACKGROUND_COLOR_HEX else BACKGROUND_COLOR_HTML) to backgroundColorHtml,
            )
        ).removeNullValues()
    }

    private fun getLargestIconUrl(baseUrl: Url?, icons: JsonArray?): Url? {
        icons
            ?.map { it as JsonObject }
            ?.maxByOrNull { sizeElement -> parseSize(sizeElement.element("sizes")) }
            .let { iconElement -> iconElement?.element("src") }
            ?.let { iconUrl -> return if (baseUrl != null) URLBuilder(baseUrl).takeFrom(iconUrl).build() else iconUrl.toUrlOrNull() }
            ?: return null
    }

    private fun JsonObject?.element(name: String): String? = this?.get(name)?.jsonPrimitive?.content?.trim()
}