package com.ecs160.hw.service;

import com.ecs160.hw.model.Commit;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.util.ConfigUtil;
import redis.clients.jedis.Jedis;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.net.URL;
import java.net.HttpURLConnection;

public class GitService {
    private Jedis jedis;

    public GitService() {
       try {
        // Use the proper constructor with host and port
        this.jedis = new Jedis("localhost", 6379);
        } catch (Exception e) {
            System.err.println("Error connecting to Redis: " + e.getMessage());
            // Initialize to null so we can check if Redis is available
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
    
        // Sort files by modification count (descending)
        List<Map.Entry<String, Integer>> sortedFiles = new ArrayList<>(fileModificationCount.entrySet());
        
        // Stable sort: Sort by modification count (descending), then by filename (ascending) for ties
        sortedFiles.sort((a, b) -> {
            int countComparison = b.getValue().compareTo(a.getValue());
            if (countComparison != 0) {
                return countComparison;
            }
            // For ties, sort alphabetically by filename for consistent results
            return a.getKey().compareTo(b.getKey());
        });
        
        // Get top 3 or fewer
        List<String> top3Files = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sortedFiles.size()); i++) {
            top3Files.add(sortedFiles.get(i).getKey());
        }
        
        return top3Files;
    }

    public boolean isRepoContainingSourceCode(Repo repo) {
        // Check if repo has any recent commits with modified files
        if (repo.getRecentCommits() != null && !repo.getRecentCommits().isEmpty()) {
            // Use the original check on modified files
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
            // No commit info available, use a simpler heuristic:
            // Assume it's a source code repo if it's not explicitly marked as a tutorial/documentation
            // and it has a recognized programming language
            String name = repo.getName().toLowerCase();
            String language = repo.getLanguage();
            
            // Check if repo name suggests it's a tutorial/documentation
            boolean isTutorial = name.contains("guide") || 
                                name.contains("tutorial") || 
                                name.contains("awesome") || 
                                name.contains("example") || 
                                name.contains("learn") ||
                                name.contains("book") ||
                                name.contains("course") ||
                                name.contains("doc");
            
            // If it has a language and doesn't look like a tutorial, assume it's a source code repo
            return language != null && !language.isEmpty() && !isTutorial;
        }
    }

    public void cloneRepository(Repo repo, String destinationDir) {
        try {
            System.out.println("Cloning repository: " + repo.getName());
            
            // Create destination directory if it doesn't exist
            File dir = new File(destinationDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String cloneCommand = "git clone --depth 1 " + repo.getHtmlUrl() + " " + destinationDir + "/" + repo.getName();
            
            Process process = Runtime.getRuntime().exec(cloneCommand);
            
            // Read output
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
        
        // Save basic repo info
        jedis.hset(repoKey, "name", repo.getName());
        jedis.hset(repoKey, "ownerLogin", repo.getOwnerLogin());
        jedis.hset(repoKey, "htmlUrl", repo.getHtmlUrl());
        jedis.hset(repoKey, "forksCount", String.valueOf(repo.getForksCount()));
        jedis.hset(repoKey, "language", repo.getLanguage() != null ? repo.getLanguage() : "");
        jedis.hset(repoKey, "openIssuesCount", String.valueOf(repo.getOpenIssuesCount()));
        jedis.hset(repoKey, "starCount", String.valueOf(repo.getStarCount()));
        jedis.hset(repoKey, "commitCount", String.valueOf(repo.getCommitCount()));
        
        // Save owner info
        String ownerKey = "owner:" + repo.getOwnerLogin();
        jedis.hset(ownerKey, "login", repo.getOwnerLogin());
        
        // Link owner to repo
        jedis.hset(repoKey, "owner", ownerKey);
    }

    // Method to make an authenticated GitHub API request
    private String makeGitHubApiRequest(String endpoint) {
        try {
            URL url = new URL("https://api.github.com/" + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            
            // Add API key if available
            String apiKey = ConfigUtil.getGitHubApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            
            // Read response
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
}
