package com.ecs160.hw.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ecs160.hw.model.Repo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonHandler {
    private Gson gson;

    public JsonHandler() {
        this.gson = new Gson();
    }

    public List<Repo> loadReposFromFile(String filePath) {
        List<Repo> repos = new ArrayList<>();
        try (FileReader reader = new FileReader(filePath)) {
            // Print file path for debugging
            System.out.println("Loading repos from: " + filePath);
            
            // Try to parse the top-level structure
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            // Debug - print the keys in the top-level object
            System.out.println("JSON keys: " + jsonObject.keySet());
            
            // Check if the file has an "items" array or is just an array of repos
            JsonArray items;
            if (jsonObject.has("items")) {
                items = jsonObject.getAsJsonArray("items");
            } else if (jsonObject.has("repositories")) {
                items = jsonObject.getAsJsonArray("repositories");
            } else {
                // If it doesn't have specific keys, try to parse it directly as an array
                try (FileReader reReader = new FileReader(filePath)) {
                    items = gson.fromJson(reReader, JsonArray.class);
                }
            }
            
            System.out.println("Found " + items.size() + " repositories");
            
            // Process each repo
            for (JsonElement item : items) {
                Repo repo = new Repo();
                JsonObject repoObj = item.getAsJsonObject();
                
                // Print repo details for debugging
                System.out.println("Processing repo: " + repoObj.toString().substring(0, Math.min(100, repoObj.toString().length())) + "...");
                
                // Basic repo data
                repo.setName(getString(repoObj, "name"));
                repo.setHtmlUrl(getString(repoObj, "html_url"));
                repo.setForksCount(getInt(repoObj, "forks_count"));
                
                // Language might be null for some repos
                if (repoObj.has("language") && !repoObj.get("language").isJsonNull()) {
                    repo.setLanguage(repoObj.get("language").getAsString());
                }
                
                repo.setOpenIssuesCount(getInt(repoObj, "open_issues_count"));
                repo.setStarCount(getInt(repoObj, "stargazers_count"));
                
                // Owner info
                if (repoObj.has("owner") && !repoObj.get("owner").isJsonNull()) {
                    JsonObject owner = repoObj.getAsJsonObject("owner");
                    repo.setOwnerLogin(getString(owner, "login"));
                }
                
                // Add to our list
                repos.add(repo);
            }
            
        } catch (IOException e) {
            System.err.println("Error loading repositories from file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }
        
        return repos;
    }
    
    // Helper methods to safely get values from JSON
    private String getString(JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception e) {
            System.err.println("Error getting string for key " + key + ": " + e.getMessage());
        }
        return "";
    }
    
    private int getInt(JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception e) {
            System.err.println("Error getting int for key " + key + ": " + e.getMessage());
        }
        return 0;
    }
}