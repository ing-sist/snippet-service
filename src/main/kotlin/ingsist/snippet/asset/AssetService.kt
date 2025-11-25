package ingsist.snippet.asset

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AssetService(private val assetRestClient: RestClient) : AssetServiceInterface {
    override fun upload(
        container: String,
        key: String,
        content: String,
    ): String {
        val response =
            assetRestClient.post()
                .uri("/v1/asset/{container}/{key}", container, key)
                .body(content)
                .retrieve()
                .toEntity(String::class.java)
        return when (response.statusCode.value()) {
            201 -> "Asset uploaded successfully in $container with key $key"
            else -> TODO()
//            else -> throw RuntimeException(
//                "Asset upload failed with status code:" +
//                    " ${response.statusCode}",
//            )
        }
    }

    override fun delete(
        container: String,
        key: String,
    ): String {
        val response =
            assetRestClient.delete()
                .uri("/v1/asset/{container}/{key}", container, key)
                .retrieve()
                .toEntity(String::class.java)
        return when (response.statusCode.value()) {
            201 -> "Asset deleted successfully in $container with key $key"
            else -> TODO()
//            else -> throw RuntimeException(
//                "Asset deleted failed with status code: " +
//                    "${response.statusCode}",
//            )
        }
    }

    override fun get(
        container: String,
        key: String,
    ): String {
        //        val response =
        assetRestClient.get()
            .uri("/v1/asset/{container}/{key}", container, key)
            .retrieve()
            .toEntity(String::class.java)

        return TODO()
//        return response.body ?: throw RuntimeException(
//            "Asset not found in " +
//                "$container with key $key",
//        )
    }

    override fun update(
        container: String,
        key: String,
        content: String,
    ): String {
        val response =
            assetRestClient.patch()
                .uri("/v1/asset/{container}/{key}", container, key)
                .body(content)
                .retrieve()
                .toEntity(String::class.java)
        return when (response.statusCode.value()) {
            201 -> "Asset updated successfully in $container with key $key"
            else -> TODO()
//            else -> throw RuntimeException(
//                "Asset updated failed with status code:" +
//                    " ${response.statusCode}",
//            )
        }
    }
}
