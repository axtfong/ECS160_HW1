package com.ecs160.hw2.application.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// client class for interacting with ollama api
public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "deepcoder:1.5b";
    private Gson gson;

    public OllamaClient() {
        this.gson = new Gson();
    }

    // sends prompt to ollama and returns response
    public String generate(String prompt) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL);
        requestBody.addProperty("prompt", prompt);
        requestBody.addProperty("stream", false);

        URL url = new URL(OLLAMA_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // sends request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // reads response
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Ollama API returned error code: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        // parses response
        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
        if (jsonResponse.has("response")) {
            return jsonResponse.get("response").getAsString();
        }

        return "";
    }
}

