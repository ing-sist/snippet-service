package ingsist.snippet.runner.snippet.helpers

import ingsist.snippet.runner.snippet.service.LanguageService
import java.util.UUID

internal fun adjustedAssetKey(
    snippetId: UUID,
    newLanguage: String,
    currentLanguage: String,
    currentAssetKey: String,
    languageService: LanguageService,
): String {
    if (newLanguage == currentLanguage) {
        return currentAssetKey
    }
    val extension = languageService.getExtension(newLanguage)
    return assetKeyForLanguage(snippetId, extension)
}

internal fun assetKeyForLanguage(
    snippetId: UUID,
    extension: String,
): String {
    return "snippet-$snippetId.$extension"
}

internal fun assetKeyForLanguage(
    snippetId: UUID,
    language: String,
    languageService: LanguageService,
): String {
    return assetKeyForLanguage(snippetId, languageService.getExtension(language))
}
