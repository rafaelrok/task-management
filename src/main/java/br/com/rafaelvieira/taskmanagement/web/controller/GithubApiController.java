package br.com.rafaelvieira.taskmanagement.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GithubApiController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/user")
    public ResponseEntity<@NotNull String> fetchUser(@RequestParam("username") String username)
            throws IOException, InterruptedException {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\":\"username is required\"}");
        }
        String url = "https://api.github.com/users/" + username;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("Accept", "application/vnd.github+json")
                        .header("Accept-Charset", "UTF-8")
                        .build();
        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(
                                java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            // pass through all relevant fields from GitHub API
            JsonNode node = objectMapper.readTree(response.body());
            var result = objectMapper.createObjectNode();
            result.put("login", node.path("login").asText(null));
            result.put("name", node.path("name").asText(null));
            result.put("bio", node.path("bio").asText(null));
            result.put("blog", node.path("blog").asText(null));
            result.put("location", node.path("location").asText(null));
            result.put("html_url", node.path("html_url").asText(null));
            result.put("avatar_url", node.path("avatar_url").asText(null));
            result.put("company", node.path("company").asText(null));
            result.put("twitter_username", node.path("twitter_username").asText(null));
            result.put("hireable", node.path("hireable").asBoolean(false));
            result.put("public_repos", node.path("public_repos").asInt(0));
            result.put("public_gists", node.path("public_gists").asInt(0));
            result.put("followers", node.path("followers").asInt(0));
            result.put("following", node.path("following").asInt(0));
            result.put("created_at", node.path("created_at").asText(null));
            result.put("updated_at", node.path("updated_at").asText(null));
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body(objectMapper.writeValueAsString(result));
        }
        return ResponseEntity.status(response.statusCode()).body(response.body());
    }
}
