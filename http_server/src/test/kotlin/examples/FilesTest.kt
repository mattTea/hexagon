package com.hexagonkt.http.server.examples

import com.hexagonkt.http.Method.POST
import com.hexagonkt.http.Part
import com.hexagonkt.http.Path
import com.hexagonkt.http.client.Client
import com.hexagonkt.http.client.Request
import com.hexagonkt.http.client.ahc.AhcAdapter
import com.hexagonkt.http.server.Server
import com.hexagonkt.http.server.ServerPort
import com.hexagonkt.http.client.Response
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import java.io.File
import java.net.URL
import kotlin.test.assertEquals

@TestInstance(PER_CLASS)
abstract class FilesTest(adapter: ServerPort) {

    private val directory = File("site/assets").let {
        if (it.exists()) it.path
        else "../site/assets"
    }

    // files
    private val server: Server = Server(adapter) {
        path("/static") {
            get("/files/*", URL("classpath:assets")) // Serve `assets` resources on `/html/*`
            get("/resources/*", File(directory)) // Serve `test` folder on `/pub/*`
        }

        get("/html/*", URL("classpath:assets")) // Serve `assets` resources on `/html/*`
        get("/pub/*", File(directory)) // Serve `test` folder on `/pub/*`
        get(URL("classpath:public")) // Serve `public` resources folder on `/*`

        post("/multipart") { ok(request.parts.keys.joinToString(":")) }

        post("/file") {
            val part = request.parts.values.first()
            val content = part.inputStream.reader().readText()
            ok(content)
        }

        post("/form") {
            fun serializeMap(map: Map<String, List<String>>): List<String> = listOf(
                map.map { "${it.key}:${it.value.joinToString(",")}}" }.joinToString("\n")
            )

            val queryParams = serializeMap(queryParametersValues)
            val formParams = serializeMap(formParametersValues)

            response.headersValues["queryParams"] = queryParams
            response.headersValues["formParams"] = formParams
        }
    }
    // files

    private val client: Client by lazy {
        Client(AhcAdapter(), "http://localhost:${server.runtimePort}")
    }

    @BeforeAll fun initialize() {
        server.start()
    }

    @AfterAll fun shutdown() {
        server.stop()
    }

    @Test fun `Parameters are separated from each other`() {
        val parts = mapOf("name" to Part("name", "value"))
        val response = client.send(
            Request(POST, Path("/form?queryName=queryValue"), parts = parts)
        )
        assert(response.headers["queryParams"]?.first()?.contains("queryName:queryValue") ?: false)
        assert(!(response.headers["queryParams"]?.first()?.contains("name:value") ?: true))
        assert(response.headers["formParams"]?.first()?.contains("name:value") ?: false)
        assert(!(response.headers["formParams"]?.first()?.contains("queryName:queryValue") ?: true))
    }

    @Test fun `Requesting a folder with an existing file name returns 404`() {
        val response = client.get ("/file.txt/")
        assertResponseContains(response, 404)
    }

    @Test fun `An static file from resources can be fetched`() {
        val response = client.get("/file.txt")
        assertResponseEquals(response, "file content")
    }

    @Test fun `Files content type is returned properly`() {
        val response = client.get("/file.css")
        assert(response.contentType?.contains("css") ?: false)
        assertResponseEquals(response, "/* css */")

        val responseFile = client.get("/pub/css/mkdocs.css")
        assert(responseFile.contentType?.contains("css") ?: false)
        assertResponseContains(responseFile, 200, "article")

        client.get("/static/resources/css/mkdocs.css").apply {
            assert(contentType?.contains("css") ?: false)
            assertResponseContains(this, 200, "article")
        }
    }

    @Test fun `Not found resources return 404`() {
        assert(client.get("/not_found.css").status == 404)
    }

    @Test fun `Sending multi part content works properly`() {
        // clientForm
        val parts = mapOf("name" to Part("name", "value"))
        val response = client.send(Request(POST, "/multipart", parts = parts))
        // clientForm
        assert(response.body == "name")
    }

    @Test fun `Sending files works properly`() {
        // clientFile
        val stream = URL("classpath:assets/index.html").openStream()
        val parts = mapOf("file" to Part("file", stream, "index.html"))
        val response = client.send(Request(POST, "/file", parts = parts))
        // clientFile
        assertResponseContains(response, 200, "<title>Hexagon</title>")
    }

    @Test fun `Files mounted on a path are returned properly`() {
        val response = client.get("/html/index.html")
        assert(response.contentType?.contains("html") ?: false)
        assertResponseContains(response, 200, "<title>Hexagon</title>")

        client.get("/static/files/index.html").apply {
            assert(contentType?.contains("html") ?: false)
            assertResponseContains(this, 200, "<title>Hexagon</title>")
        }
    }

    private fun assertResponseEquals(response: Response?, content: String, status: Int = 200) {
        assertEquals(status, response?.status)
        assertEquals(content, response?.body?.trim())
    }

    private fun assertResponseContains(response: Response?, status: Int, vararg content: String) {
        assert(response?.status == status)
        content.forEach {
            assert (response?.body?.contains (it) ?: false)
        }
    }
}
