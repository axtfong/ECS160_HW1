package com.ecs160.hw2.application;

import com.ecs160.hw2.application.model.BugIssue;
import com.ecs160.hw2.application.model.IssueModel;
import com.ecs160.hw2.application.model.RepoModel;
import com.ecs160.hw2.microservice.MicroserviceLauncher;
import com.ecs160.hw2.persistence.RedisDB;
import com.ecs160.hw2.application.microservice.BugFinderMicroservice;
import com.ecs160.hw2.application.microservice.IssueComparatorMicroservice;
import com.ecs160.hw2.application.microservice.IssueSummarizerMicroservice;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Main application for HW2.
 */
public class App {
    private static final String SELECTED_REPO_FILE = "selected_repo.dat";
    private static final String ANALYSIS_FILE = "ANALYSIS.md";
    private static final int MICROSERVICE_PORT = 8080;
    private static final String MICROSERVICE_BASE_URL = "http://localhost:" + MICROSERVICE_PORT;
    
    private RedisDB redisDB;
    private RedisDB issueRedisDB;
    private Gson gson;
    private MicroserviceLauncher launcher;
    
    public App() {
        this.redisDB = new RedisDB("localhost", 6379, 0);
        this.issueRedisDB = new RedisDB("localhost", 6379, 1);
        this.gson = new Gson();
        this.launcher = new MicroserviceLauncher();
    }
    
    public static void main(String[] args) {
        App app = new App();
        app.run();
    }
    
    public void run() {
        try {
            // Start microservices
            System.out.println("Starting microservices...");
            startMicroservices();
            Thread.sleep(2000); // Wait for server to start
            
            // Load selected repo
            System.out.println("Loading selected repository...");
            String repoId = loadSelectedRepo();
            if (repoId == null) {
                System.err.println("No repository selected. Please create " + SELECTED_REPO_FILE);
                return;
            }
            
            // Load repo and issues from Redis
            System.out.println("Loading repository from Redis...");
            RepoModel repo = loadRepoFromRedis(repoId);
            if (repo == null) {
                System.err.println("Repository not found: " + repoId);
                return;
            }
            
            List<IssueModel> issues = loadIssuesFromRedis(repo.getIssues());
            System.out.println("Loaded " + issues.size() + " issues");
            
            // Clone repository
            System.out.println("Cloning repository...");
            String repoPath = cloneRepository(repo.getUrl());
            
            // Load files to analyze
            List<String> filesToAnalyze = loadFilesToAnalyze();
            
            // Step 1: Invoke Microservice A to summarize issues
            System.out.println("Summarizing GitHub issues...");
            List<BugIssue> issueList1 = summarizeIssues(issues);
            
            // Step 2: Invoke Microservice B to find bugs in C files
            System.out.println("Finding bugs in C files...");
            List<BugIssue> issueList2 = findBugsInFiles(repoPath, filesToAnalyze);
            
            // Step 3: Invoke Microservice C to compare and find common issues
            System.out.println("Comparing issues...");
            List<BugIssue> commonIssues = compareIssues(issueList1, issueList2);
            
            // Print results
            System.out.println("\n=== RESULTS ===");
            System.out.println("Issues from GitHub: " + issueList1.size());
            System.out.println("Bugs found by LLM: " + issueList2.size());
            System.out.println("Common issues: " + commonIssues.size());
            
            System.out.println("\nCommon Issues:");
            for (BugIssue issue : commonIssues) {
                System.out.println("- " + issue.getBug_type() + " at line " + issue.getLine() + 
                    ": " + issue.getDescription());
            }
            
            // Generate analysis
            generateAnalysis(repo, issueList1, issueList2, commonIssues);
            
            // Stop microservices
            launcher.stop();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            redisDB.close();
            issueRedisDB.close();
        }
    }
    
    private void startMicroservices() throws Exception {
        launcher.registerMicroservice(
            IssueSummarizerMicroservice.class,
            BugFinderMicroservice.class,
            IssueComparatorMicroservice.class
        );
        
        new Thread(() -> launcher.launch(MICROSERVICE_PORT)).start();
    }
    
    private String loadSelectedRepo() {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(SELECTED_REPO_FILE))) {
            return reader.readLine().trim();
        } catch (IOException e) {
            System.err.println("Error reading " + SELECTED_REPO_FILE + ": " + e.getMessage());
            return null;
        }
    }
    
    private RepoModel loadRepoFromRedis(String repoId) {
        RepoModel repo = new RepoModel();
        repo.setId(repoId);
        RepoModel loaded = (RepoModel) redisDB.load(repo);
        
        // Map the "Author Name" field from Redis to authorName in Java
        if (loaded != null) {
            // The RedisDB should handle the mapping, but if not, we do it here
            // For now, we'll rely on the RedisDB mapping
        }
        
        return loaded;
    }
    
    private List<IssueModel> loadIssuesFromRedis(String issueIdsStr) {
        List<IssueModel> issues = new ArrayList<>();
        if (issueIdsStr == null || issueIdsStr.isEmpty()) {
            return issues;
        }
        
        String[] issueIds = issueIdsStr.split(",");
        for (String issueId : issueIds) {
            issueId = issueId.trim();
            if (!issueId.isEmpty()) {
                IssueModel issue = new IssueModel();
                issue.setId(issueId);
                IssueModel loaded = (IssueModel) issueRedisDB.load(issue);
                if (loaded != null) {
                    issues.add(loaded);
                }
            }
        }
        
        return issues;
    }
    
    private String cloneRepository(String repoUrl) throws IOException, InterruptedException {
        String repoName = extractRepoName(repoUrl);
        String cloneDir = "cloned_repos_hw2";
        
        File dir = new File(cloneDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        String clonePath = cloneDir + "/" + repoName;
        File repoDir = new File(clonePath);
        
        // If already cloned, skip
        if (repoDir.exists()) {
            System.out.println("Repository already cloned at: " + clonePath);
            return clonePath;
        }
        
        String cloneCommand = "git clone --depth 1 " + repoUrl + " " + clonePath;
        Process process = Runtime.getRuntime().exec(cloneCommand);
        process.waitFor();
        
        if (process.exitValue() != 0) {
            throw new IOException("Failed to clone repository");
        }
        
        return clonePath;
    }
    
    private String extractRepoName(String repoUrl) {
        // Extract repo name from URL like https://github.com/user/repo.git
        String[] parts = repoUrl.replace(".git", "").split("/");
        return parts[parts.length - 1];
    }
    
    private List<String> loadFilesToAnalyze() {
        List<String> files = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(SELECTED_REPO_FILE))) {
            String repoId = reader.readLine(); // Skip first line (repo ID)
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    files.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading files from " + SELECTED_REPO_FILE + ": " + e.getMessage());
        }
        return files;
    }
    
    private List<BugIssue> summarizeIssues(List<IssueModel> issues) throws IOException {
        List<BugIssue> summarizedIssues = new ArrayList<>();
        
        for (IssueModel issue : issues) {
            // Convert IssueModel to JSON for microservice
            JsonObject issueJson = new JsonObject();
            issueJson.addProperty("description", issue.getDescription());
            issueJson.addProperty("date", issue.getDate() != null ? 
                new SimpleDateFormat("yyyy-MM-dd").format(issue.getDate()) : "");
            
            String input = issueJson.toString();
            String response = callMicroservice("summarize_issue", input);
            
            if (response != null && !response.isEmpty()) {
                try {
                    BugIssue bugIssue = gson.fromJson(response, BugIssue.class);
                    summarizedIssues.add(bugIssue);
                } catch (Exception e) {
                    System.err.println("Error parsing summarized issue: " + e.getMessage());
                }
            }
        }
        
        return summarizedIssues;
    }
    
    private List<BugIssue> findBugsInFiles(String repoPath, List<String> files) throws IOException {
        List<BugIssue> allBugs = new ArrayList<>();
        
        for (String filePath : files) {
            String fullPath = repoPath + "/" + filePath;
            File file = new File(fullPath);
            
            if (!file.exists() || !file.isFile()) {
                System.err.println("File not found: " + fullPath);
                continue;
            }
            
            // Read file content
            String content = new String(Files.readAllBytes(Paths.get(fullPath)), StandardCharsets.UTF_8);
            
            // Prepare input for microservice
            JsonObject inputJson = new JsonObject();
            inputJson.addProperty("filename", filePath);
            inputJson.addProperty("content", content);
            
            String input = inputJson.toString();
            String response = callMicroservice("find_bugs", input);
            
            if (response != null && !response.isEmpty()) {
                try {
                    BugIssue[] bugs = gson.fromJson(response, BugIssue[].class);
                    allBugs.addAll(Arrays.asList(bugs));
                } catch (Exception e) {
                    System.err.println("Error parsing bugs: " + e.getMessage());
                }
            }
        }
        
        return allBugs;
    }
    
    private List<BugIssue> compareIssues(List<BugIssue> list1, List<BugIssue> list2) throws IOException {
        JsonObject inputJson = new JsonObject();
        inputJson.add("list1", gson.toJsonTree(list1));
        inputJson.add("list2", gson.toJsonTree(list2));
        
        String input = inputJson.toString();
        String response = callMicroservice("check_equivalence", input);
        
        if (response != null && !response.isEmpty()) {
            try {
                BugIssue[] common = gson.fromJson(response, BugIssue[].class);
                return Arrays.asList(common);
            } catch (Exception e) {
                System.err.println("Error parsing common issues: " + e.getMessage());
            }
        }
        
        return new ArrayList<>();
    }
    
    private String callMicroservice(String endpoint, String input) throws IOException {
        try {
            String url = MICROSERVICE_BASE_URL + "/" + endpoint + "?input=" + 
                java.net.URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
            
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Microservice returned error code: " + responseCode);
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return response.toString();
        } catch (Exception e) {
            System.err.println("Error calling microservice " + endpoint + ": " + e.getMessage());
            return null;
        }
    }
    
    private void generateAnalysis(RepoModel repo, List<BugIssue> githubIssues, 
                                   List<BugIssue> llmBugs, List<BugIssue> commonIssues) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(ANALYSIS_FILE))) {
            writer.println("# Analysis: LLM Bug Detection Validation");
            writer.println();
            writer.println("## Repository: " + repo.getId());
            writer.println("## URL: " + repo.getUrl());
            writer.println();
            writer.println("## Results");
            writer.println();
            writer.println("- **GitHub Issues Found**: " + githubIssues.size());
            writer.println("- **Bugs Detected by LLM**: " + llmBugs.size());
            writer.println("- **Common Issues**: " + commonIssues.size());
            writer.println();
            writer.println("## Analysis");
            writer.println();
            
            if (commonIssues.isEmpty()) {
                writer.println("The LLM did not detect any bugs that were also reported in GitHub issues.");
                writer.println();
                writer.println("### Possible Reasons:");
                writer.println("1. The LLM may have identified different types of bugs than what users reported");
                writer.println("2. The issue descriptions may not have been specific enough to match");
                writer.println("3. The LLM may have found bugs in different parts of the code than reported issues");
                writer.println("4. The deepcoder:1.5b model may have limitations in bug detection accuracy");
            } else {
                writer.println("The LLM successfully detected " + commonIssues.size() + " bug(s) that were also reported in GitHub issues:");
                writer.println();
                for (BugIssue issue : commonIssues) {
                    writer.println("- **" + issue.getBug_type() + "** at line " + issue.getLine());
                    writer.println("  - Description: " + issue.getDescription());
                    writer.println("  - File: " + issue.getFilename());
                }
            }
            
            writer.println();
            writer.println("## Conclusion");
            writer.println();
            writer.println("This analysis demonstrates the ability of LLMs (specifically deepcoder:1.5b) ");
            writer.println("to detect bugs in code and cross-reference them with real GitHub issue reports. ");
            writer.println("The results show that LLMs can identify some bugs that are also reported by users, ");
            writer.println("though the matching process has limitations and may not catch all reported issues.");
            
        } catch (IOException e) {
            System.err.println("Error writing analysis: " + e.getMessage());
        }
    }
}

