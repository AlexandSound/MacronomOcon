package com.example.macronomocon.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseQueryTest {
    @Test
    fun connectionQuerySendsFiltersAndDecodesRows() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/rest/v1/profiles", request.url.encodedPath)
            assertEquals("eq.user-1", request.url.parameters["user_id"])
            respond(
                """[{"user_id":"user-1","display_name":"Student"}]""",
                HttpStatusCode.OK,
                headersOf("Content-Type", "application/json")
            )
        }
        val client = createSupabaseClient("https://example.test", "test-key") {
            httpEngine = engine
            install(Postgrest)
        }
        try {
            val rows = SupabaseConnection(client).createQuery("profiles")
                .select(mapOf("user_id" to "user-1"))
            assertEquals("Student", rows.single()["display_name"]?.jsonPrimitive?.content)
            assertEquals(1, engine.requestHistory.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun factoryQueryPreservesUpsertConflictAndPayload() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/rest/v1/recipe_bookmarks", request.url.encodedPath)
            assertEquals("user_id,recipe_id", request.url.parameters["on_conflict"])
            assertTrue(request.headers["Prefer"].orEmpty().contains("resolution=merge-duplicates"))
            val payload = Json.parseToJsonElement(
                (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            ).jsonArray.single().jsonObject
            assertEquals("recipe-1", payload["recipe_id"]?.jsonPrimitive?.content)
            assertEquals("true", payload["favorite"]?.jsonPrimitive?.content)
            respond("", HttpStatusCode.Created)
        }
        val client = createSupabaseClient("https://example.test", "test-key") {
            httpEngine = engine
            install(Postgrest)
        }
        try {
            val factory = SupabaseConnectionFactory("https://example.test", "test-key")
            val query = factory.createQuery(SupabaseConnection(client), "recipe_bookmarks")
            query.upsert(
                buildJsonObject {
                    put("user_id", "user-1")
                    put("recipe_id", "recipe-1")
                    put("favorite", true)
                },
                "user_id,recipe_id"
            )
            assertEquals(1, engine.requestHistory.size)
        } finally {
            client.close()
        }
    }
}
