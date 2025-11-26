package ingsist.snippet.asset

import ingsist.snippet.exception.ExternalServiceException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AssetService(private val assetRestClient: RestClient) : AssetServiceInterface {
    override fun upload(
        container: String,
        key: String,
        content: String,
    ): String {
        assetRestClient.post()
            .uri("/v1/asset/{container}/{key}", container, key)
            .body(content)
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException("Asset upload failed with status code: ${response.statusCode}")
            }
            .toBodilessEntity()
        return "Asset uploaded successfully in $container with key $key"
    }

    override fun delete(
        container: String,
        key: String,
    ): String {
        assetRestClient.delete()
            .uri("/v1/asset/{container}/{key}", container, key)
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException("Asset deleted failed with status code: ${response.statusCode}")
            }
            .toBodilessEntity()
        return "Asset deleted successfully in $container with key $key"
    }

    override fun get(
        container: String,
        key: String,
    ): String {
        val response =
            assetRestClient.get()
                .uri("/v1/asset/{container}/{key}", container, key)
                .retrieve()
                .onStatus({ status -> status.isError }) { _, response ->
                    throw ExternalServiceException("Asset not found in $container with key $key")
                }
                .toEntity(String::class.java)

        return response.body ?: throw ExternalServiceException("Asset content is empty")
    }

    override fun update(
        container: String,
        key: String,
        content: String,
    ): String {
        assetRestClient.patch()
            .uri("/v1/asset/{container}/{key}", container, key)
            .body(content)
            .retrieve()
            .onStatus({ status -> status.isError }) { _, response ->
                throw ExternalServiceException("Asset updated failed with status code: ${response.statusCode}")
            }
            .toBodilessEntity()
        return "Asset updated successfully in $container with key $key"
    }
}
