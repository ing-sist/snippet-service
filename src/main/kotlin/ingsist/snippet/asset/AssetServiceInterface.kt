package ingsist.snippet.asset

interface AssetServiceInterface {
    fun upload(
        container: String,
        key: String,
        content: String,
    ): String

    fun delete(
        container: String,
        key: String,
    ): String

    fun get(
        container: String,
        key: String,
    ): String
}
