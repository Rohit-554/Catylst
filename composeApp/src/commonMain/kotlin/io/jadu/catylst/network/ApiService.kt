package io.jadu.catylst.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class ApiService(private val client: HttpClient) {

    suspend fun fetchSampleData(): SampleResponse {
        return client.get("https://jsonplaceholder.typicode.com/posts/1") {
            contentType(ContentType.Application.Json)
        }.body()
    }

    suspend fun postSampleData(request: SampleRequest): SampleResponse {
        return client.post("https://jsonplaceholder.typicode.com/posts") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}

@Serializable
data class SampleResponse(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)

@Serializable
data class SampleRequest(
    val title: String,
    val body: String,
    val userId: Int
)
