package com.ecs160.hw.service;

import com.ecs160.hw.model.Commit;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.util.ConfigUtil;
import redis.clients.jedis.Jedis;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class GitService {
    private Jedis jedis;

    public GitService() {
        try {
            this.jedis = new Jedis("localhost", 6379);
        } catch (Exception e) {
            System.err.println("Error connecting to Redis: " + e.getMessage());
            this.jedis = null;
        }
    }

    public Map<String, Integer> calculateFileModificationCount(Repo repo) {
        Map<String, Integer> fileModificationCount = new HashMap<>();
        
        for (Commit commit : repo.getRecentCommits()) {
            for (String file : commit.getModifiedFiles()) {
                fileModificationCount.put(file, fileModificationCount.getOrDefault(file, 0) + 1);
            }
        }
        
        return fileModificationCount;
    }

    public List<String> getTop3ModifiedFiles(Repo repo) {
        Map<String, Integer> fileModificationCount = calculateFileModificationCount(repo);

        List<Map.Entry<String, Integer>> sortedFiles = new ArrayList<>(fileModificationCount.entrySet());
        
        // stable sort by modification count then by filename
        sortedFiles.sort((a, b) -> {
            int countComparison = b.getValue().compareTo(a.getValue());
            if (countComparison != 0) {
                return countComparison;
            }

            return a.getKey().compareTo(b.getKey());
        });

        List<String> top3Files = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sortedFiles.size()); i++) {
            top3Files.add(sortedFiles.get(i).getKey());
        }
        
        return top3Files;
    }

    public boolean isRepoContainingSourceCode(Repo repo) {
        if (repo.getRecentCommits() != null && !repo.getRecentCommits().isEmpty()) {
            List<String> sourceFileExtensions = Arrays.asList(".java", ".cpp", ".c", ".h", ".rs", ".go", ".py", ".js");
            
            for (Commit commit : repo.getRecentCommits()) {
                for (String file : commit.getModifiedFiles()) {
                    for (String ext : sourceFileExtensions) {
                        if (file.endsWith(ext)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else {
            String name = repo.getName().toLowerCase();
            String language = repo.getLanguage();
            
            // use heuristic (check if these words exist)
            boolean isTutorial = name.contains("guide") || 
                                name.contains("tutorial") ||
                                name.contains("awesome") ||
                                name.contains("example") ||
                                name.contains("learn") ||
                                name.contains("book") ||
                                name.contains("course") ||
                                name.contains("doc");

            return language != null && !language.isEmpty() && !isTutorial;
        }
    }

    public void cloneRepository(Repo repo, String destinationDir) {
        try {
            System.out.println("Cloning repository: " + repo.getName());

            File dir = new File(destinationDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String cloneCommand = "git clone --depth 1 " + repo.getHtmlUrl() + " " + destinationDir + "/" + repo.getName();
            
            Process process = Runtime.getRuntime().exec(cloneCommand);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            
            int exitCode = process.waitFor();
            System.out.println("Clone completed with exit code: " + exitCode);
            
        } catch (Exception e) {
            System.err.println("Error cloning repository: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveRepoToRedis(Repo repo) {
        String repoKey = "reponame:" + repo.getName();

        jedis.hset(repoKey, "name", repo.getName());
        jedis.hset(repoKey, "ownerLogin", repo.getOwnerLogin());
        jedis.hset(repoKey, "htmlUrl", repo.getHtmlUrl());
        jedis.hset(repoKey, "forksCount", String.valueOf(repo.getForksCount()));
        jedis.hset(repoKey, "language", repo.getLanguage() != null ? repo.getLanguage() : "");
        jedis.hset(repoKey, "openIssuesCount", String.valueOf(repo.getOpenIssuesCount()));
        jedis.hset(repoKey, "starCount", String.valueOf(repo.getStarCount()));
        jedis.hset(repoKey, "commitCount", String.valueOf(repo.getCommitCount()));

        String ownerKey = "owner:" + repo.getOwnerLogin();
        jedis.hset(ownerKey, "login", repo.getOwnerLogin());

        jedis.hset(repoKey, "owner", ownerKey);
    }

    private String makeGitHubApiRequest(String endpoint) {
        try {
            URL url = new URL("https://api.github.com/" + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

            String apiKey = ConfigUtil.getGitHubApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            System.err.println("Error making GitHub API request: " + e.getMessage());
            return null;
        }
    }

    public void fetchRecentCommits(Repo repo) {
        try {
            String endpoint = String.format("repos/%s/%s/commits?per_page=50",
                repo.getOwnerLogin(), repo.getName());
            String response = makeGitHubApiRequest(endpoint);
            
            if (response != null) {
                parseCommitsFromResponse(repo, response);
            } else {
                repo.setRecentCommits(new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error fetching commits for " + repo.getName() + ": " + e.getMessage());
            repo.setRecentCommits(new ArrayList<>());
        }
    }

    private void parseCommitsFromResponse(Repo repo, String response) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonArray commitsArray = gson.fromJson(response, com.google.gson.JsonArray.class);
            
            List<Commit> commits = new ArrayList<>();
            for (com.google.gson.JsonElement element : commitsArray) {
                com.google.gson.JsonObject commitObj = element.getAsJsonObject();
                Commit commit = new Commit();
                
                commit.setSha(commitObj.get("sha").getAsString());
                commit.setMessage(commitObj.getAsJsonObject("commit").get("message").getAsString());

                fetchCommitFilesSimplified(repo, commit);
                
                commits.add(commit);
            }
            
            repo.setRecentCommits(commits);
        } catch (Exception e) {
            System.err.println("Error parsing commits: " + e.getMessage());
        }
    }

    private void fetchCommitFiles(Repo repo, Commit commit) {
        try {
            String endpoint = String.format("repos/%s/%s/commits/%s", 
                repo.getOwnerLogin(), repo.getName(), commit.getSha());
            String response = makeGitHubApiRequest(endpoint);
            
            if (response != null) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.JsonObject commitObj = gson.fromJson(response, com.google.gson.JsonObject.class);
                com.google.gson.JsonArray filesArray = commitObj.getAsJsonArray("files");
                
                for (com.google.gson.JsonElement fileElement : filesArray) {
                    com.google.gson.JsonObject fileObj = fileElement.getAsJsonObject();
                    String filename = fileObj.get("filename").getAsString();
                    commit.addModifiedFile(filename);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching commit files: " + e.getMessage());
        }
    }

    private void fetchCommitFilesSimplified(Repo repo, Commit commit) {
        // Skip fetching files for most commits to speed up processing
        // Only fetch files for the first few commits
        if (repo.getRecentCommits().indexOf(commit) >= 50) {
            return;
        }
        
        try {
            String endpoint = String.format("repos/%s/%s/commits/%s", 
                repo.getOwnerLogin(), repo.getName(), commit.getSha());
            String response = makeGitHubApiRequest(endpoint);
            
            if (response != null) {
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.JsonObject commitObj = gson.fromJson(response, com.google.gson.JsonObject.class);
                com.google.gson.JsonArray filesArray = commitObj.getAsJsonArray("files");
                
                for (com.google.gson.JsonElement fileElement : filesArray) {
                    com.google.gson.JsonObject fileObj = fileElement.getAsJsonObject();
                    String filename = fileObj.get("filename").getAsString();
                    commit.addModifiedFile(filename);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching commit files: " + e.getMessage());
        }
    }

    public void fetchForks(Repo repo) {
        try {
            String endpoint = String.format("repos/%s/%s/forks?per_page=20&sort=newest",
                repo.getOwnerLogin(), repo.getName());
            String response = makeGitHubApiRequest(endpoint);
            
            if (response != null) {
                parseForksFromResponse(repo, response);
            } else {
                repo.setForks(new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error fetching forks for " + repo.getName() + ": " + e.getMessage());
            repo.setForks(new ArrayList<>());
        }
    }

    private void parseForksFromResponse(Repo repo, String response) {
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonArray forksArray = gson.fromJson(response, com.google.gson.JsonArray.class);
            
            List<Repo> forks = new ArrayList<>();
            for (com.google.gson.JsonElement element : forksArray) {
                com.google.gson.JsonObject forkObj = element.getAsJsonObject();
                Repo fork = new Repo();
                
                fork.setName(forkObj.get("name").getAsString());
                fork.setOwnerLogin(forkObj.getAsJsonObject("owner").get("login").getAsString());
                fork.setHtmlUrl(forkObj.get("html_url").getAsString());
                fork.setStarCount(forkObj.get("stargazers_count").getAsInt());
                fork.setForksCount(forkObj.get("forks_count").getAsInt());
                fork.setOpenIssuesCount(forkObj.get("open_issues_count").getAsInt());
                
                // Skip fetching commit count to speed up processing
                // Set a default value instead
                fork.setCommitCount(0);
                
                forks.add(fork);
            }
            
            repo.setForks(forks);
        } catch (Exception e) {
            System.err.println("Error parsing forks: " + e.getMessage());
        }
    }

    // Fetch commit count for a repository
    private void fetchCommitCount(Repo repo) {
        try {
            String endpoint = String.format("repos/%s/%s/commits?per_page=1", 
                repo.getOwnerLogin(), repo.getName());
            String response = makeGitHubApiRequest(endpoint);
            
            if (response != null) {
                // Parse the response to get total commit count
                // Note: This is a simplified approach - in reality, we'd need to handle pagination
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.JsonArray commitsArray = gson.fromJson(response, com.google.gson.JsonArray.class);
                repo.setCommitCount(commitsArray.size());
            }
        } catch (Exception e) {
            System.err.println("Error fetching commit count for " + repo.getName() + ": " + e.getMessage());
            repo.setCommitCount(0);
        }
    }

    // Save commit data to JSON file
    public void saveCommitDataToFile(Repo repo, String language) {
        try {
            // Sanitize language name for filename (remove slashes and other invalid characters)
            String sanitizedLanguage = language.toLowerCase()
                .replace("/", "_")
                .replace("+", "plus")
                .replace(" ", "_")
                .replace("\\", "_");
            String filename = String.format("commits/commits_%s_%s.json", sanitizedLanguage, repo.getName());
            File file = new File(filename);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject repoData = new JsonObject();
            
            // Save basic repo info
            repoData.addProperty("name", repo.getName());
            repoData.addProperty("ownerLogin", repo.getOwnerLogin());
            repoData.addProperty("language", repo.getLanguage());
            
            // Save commits
            JsonArray commitsArray = new JsonArray();
            for (Commit commit : repo.getRecentCommits()) {
                JsonObject commitObj = new JsonObject();
                commitObj.addProperty("sha", commit.getSha());
                commitObj.addProperty("message", commit.getMessage());
                
                JsonArray filesArray = new JsonArray();
                for (String fileName : commit.getModifiedFiles()) {
                    filesArray.add(fileName);
                }
                commitObj.add("modifiedFiles", filesArray);
                commitsArray.add(commitObj);
            }
            repoData.add("commits", commitsArray);
            
            // Save forks
            JsonArray forksArray = new JsonArray();
            for (Repo fork : repo.getForks()) {
                JsonObject forkObj = new JsonObject();
                forkObj.addProperty("name", fork.getName());
                forkObj.addProperty("ownerLogin", fork.getOwnerLogin());
                forkObj.addProperty("commitCount", fork.getCommitCount());
                forksArray.add(forkObj);
            }
            repoData.add("forks", forksArray);
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(repoData, writer);
            }
            
            System.out.println("Saved commit data for " + repo.getName() + " to " + filename);
        } catch (Exception e) {
            System.err.println("Error saving commit data for " + repo.getName() + ": " + e.getMessage());
        }
    }

    // Load commit data from JSON file
    public boolean loadCommitDataFromFile(Repo repo, String language) {
        try {
            // Sanitize language name for filename (remove slashes and other invalid characters)
            String sanitizedLanguage = language.toLowerCase()
                .replace("/", "_")
                .replace("+", "plus")
                .replace(" ", "_")
                .replace("\\", "_");
            String filename = String.format("commits/commits_%s_%s.json", sanitizedLanguage, repo.getName());
            File file = new File(filename);
            
            if (!file.exists()) {
                return false;
            }
            
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(file)) {
                JsonObject repoData = gson.fromJson(reader, JsonObject.class);
                
                // Load commits
                JsonArray commitsArray = repoData.getAsJsonArray("commits");
                List<Commit> commits = new ArrayList<>();
                for (int i = 0; i < commitsArray.size(); i++) {
                    JsonObject commitObj = commitsArray.get(i).getAsJsonObject();
                    Commit commit = new Commit();
                    commit.setSha(commitObj.get("sha").getAsString());
                    commit.setMessage(commitObj.get("message").getAsString());
                    
                    JsonArray filesArray = commitObj.getAsJsonArray("modifiedFiles");
                    for (int j = 0; j < filesArray.size(); j++) {
                        commit.addModifiedFile(filesArray.get(j).getAsString());
                    }
                    commits.add(commit);
                }
                repo.setRecentCommits(commits);
                
                // Load forks
                JsonArray forksArray = repoData.getAsJsonArray("forks");
                List<Repo> forks = new ArrayList<>();
                for (int i = 0; i < forksArray.size(); i++) {
                    JsonObject forkObj = forksArray.get(i).getAsJsonObject();
                    Repo fork = new Repo();
                    fork.setName(forkObj.get("name").getAsString());
                    fork.setOwnerLogin(forkObj.get("ownerLogin").getAsString());
                    fork.setCommitCount(forkObj.get("commitCount").getAsInt());
                    forks.add(fork);
                }
                repo.setForks(forks);
                
                System.out.println("Loaded cached commit data for " + repo.getName());
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading commit data for " + repo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // Enhanced method to fetch commits with caching
    public void fetchRecentCommitsWithCache(Repo repo, String language) {
        // Try to load from cache first
        if (loadCommitDataFromFile(repo, language)) {
            return;
        }
        
        // If not in cache, fetch from API
        System.out.println("No cached data found for " + repo.getName() + ", fetching from API...");
        fetchRecentCommits(repo);
        fetchForks(repo);
        
        // Save to cache for next time
        saveCommitDataToFile(repo, language);
    }
}
