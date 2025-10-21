package com.ecs160.hw;

import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;
import com.ecs160.hw.util.JsonHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class App
{
    private static final String GITHUB_API_URL = "https://api.github.com/search/repositories";
    private static String GITHUB_TOKEN;

    public static void main( String[] args ) {
        // Load configuration
        loadConfig();

        JsonHandler jsonHandler = new JsonHandler();
        GitService gitService = new GitService();

        String javaReposPath = "top_java_repos.json";
        String cCppReposPath = "top_c_cpp_repos.json";
        String rustReposPath = "top_rust_repos.json";

        // Check and fetch JSON files if missing
        ensureJsonFileExists(javaReposPath, "java", jsonHandler);
        ensureJsonFileExists(cCppReposPath, "c++", jsonHandler);
        ensureJsonFileExists(rustReposPath, "rust", jsonHandler);

        List<Repo> javaRepos = jsonHandler.loadReposFromFile(javaReposPath);
        List<Repo> cCppRepos = jsonHandler.loadReposFromFile(cCppReposPath);
        List<Repo> rustRepos = jsonHandler.loadReposFromFile(rustReposPath);

        processLanguage("Java", javaRepos, gitService);
        processLanguage("C/C++", cCppRepos, gitService);
        processLanguage("Rust", rustRepos, gitService);

        cloneMostPopularSourceRepo("Java", javaRepos, gitService);
        cloneMostPopularSourceRepo("C/C++", cCppRepos, gitService);
        cloneMostPopularSourceRepo("Rust", rustRepos, gitService);

        saveAllReposToRedis(javaRepos, cCppRepos, rustRepos, gitService);

        System.out.println("Processing complete!");
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
            GITHUB_TOKEN = properties.getProperty("github.api.key");

            if (GITHUB_TOKEN == null || GITHUB_TOKEN.isEmpty()) {
                System.out.println("Warning: github.api.key not found in config.properties");
                System.out.println("API requests will be rate-limited to 60 per hour");
            } else {
                System.out.println("GitHub API token loaded successfully");
            }
        } catch (IOException e) {
            System.out.println("Warning: config.properties file not found");
            System.out.println("API requests will be made without authentication (rate-limited to 60 per hour)");
        }
    }

    private static void ensureJsonFileExists(String filePath, String language, JsonHandler jsonHandler) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("File " + filePath + " not found. Fetching data from GitHub API...");
            try {
                String jsonData = fetchReposFromGitHub(language);

                // Pretty print the JSON before saving
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Object jsonObject = JsonParser.parseString(jsonData);
                String prettyJson = gson.toJson(jsonObject);

                try (FileWriter writer = new FileWriter(filePath)) {
                    writer.write(prettyJson);
                }

                System.out.println("Successfully saved " + filePath);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error fetching data for " + language + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Found existing file: " + filePath);
        }
    }

    private static String fetchReposFromGitHub(String language) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        // Build query parameter - format language for URL
        String languageQuery = language.replace("+", "%2B").replace(" ", "+");
        String query = String.format("language:%s", languageQuery);

        // Build URL with query parameters
        String url = String.format("%s?q=%s&sort=stars&order=desc&per_page=10",
                GITHUB_API_URL, query);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json");

        // Add authentication token if available
        if (GITHUB_TOKEN != null && !GITHUB_TOKEN.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + GITHUB_TOKEN);
        }

        HttpRequest request = requestBuilder.build();

        System.out.println("Fetching repositories for language: " + language);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("GitHub API request failed with status code: " + response.statusCode()
                    + "\nResponse: " + response.body());
        }
    }

    private static void processLanguage(String language, List<Repo> repos, GitService gitService) {
        System.out.println("\nLanguage: " + language);

        int totalStars = 0;
        int totalForks = 0;
        int openIssues = 0;
        int newCommitsInForks = 0;

        for (Repo repo : repos) {
            totalStars += repo.getStarCount();
            totalForks += repo.getForksCount();
            openIssues += repo.getOpenIssuesCount();
            newCommitsInForks += countNewCommitsInForks(repo);

            List<String> top3ModifiedFiles = gitService.getTop3ModifiedFiles(repo);

            System.out.println("\nRepo name: " + repo.getName());
            for (int i = 0; i < top3ModifiedFiles.size(); i++) {
                System.out.println("File name" + (i + 1) + ": " + top3ModifiedFiles.get(i));
            }
        }

        System.out.println("\nTotal stars: " + totalStars);
        System.out.println("Total forks: " + totalForks);
        System.out.println("New commits in forked repos: " + newCommitsInForks);
        System.out.println("Open issues in top-10 repos: " + openIssues);
    }

    private static int countNewCommitsInForks(Repo repo) {
        int count = 0;

        // Count commits in the 20 most-recent forked repos or fewer
        int forksToCheck = Math.min(20, repo.getForks().size());

        for (int i = 0; i < forksToCheck; i++) {
            Repo fork = repo.getForks().get(i);
            count += fork.getCommitCount();
        }

        return count;
    }

    private static void cloneMostPopularSourceRepo(String language, List<Repo> repos, GitService gitService) {
        // Filter repos that contain source code
        Repo mostPopular = null;

        for (Repo repo : repos) {
            if (gitService.isRepoContainingSourceCode(repo)) {
                if (mostPopular == null || repo.getStarCount() > mostPopular.getStarCount()) {
                    mostPopular = repo;
                }
            }
        }

        if (mostPopular != null) {
            System.out.println("\nCloning most popular " + language + " repository: " + mostPopular.getName());
            gitService.cloneRepository(mostPopular, "cloned_repos");
        } else {
            System.out.println("\nNo source code repositories found for " + language);
        }
    }

    private static void saveAllReposToRedis(List<Repo> javaRepos, List<Repo> cCppRepos, List<Repo> rustRepos, GitService gitService) {
        System.out.println("\nSaving repositories to Redis...");

        for (Repo repo : javaRepos) {
            gitService.saveRepoToRedis(repo);
        }

        for (Repo repo : cCppRepos) {
            gitService.saveRepoToRedis(repo);
        }

        for (Repo repo : rustRepos) {
            gitService.saveRepoToRedis(repo);
        }

        System.out.println("All repositories saved to Redis successfully!");
    }
}