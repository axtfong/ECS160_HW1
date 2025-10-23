package com.ecs160.hw;

import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;
import com.ecs160.hw.util.ConfigUtil;
import com.ecs160.hw.util.JsonHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class App
{
    private static final String GITHUB_API_URL = "https://api.github.com/search/repositories";

    public static void main(String[] args) {
        String apiKey = ConfigUtil.getGitHubApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Warning: GitHub API token not found");
            System.out.println("API requests will be rate-limited to 60 per hour");
        } else {
            System.out.println("GitHub API token loaded successfully");
        }

        JsonHandler jsonHandler = new JsonHandler();
        GitService gitService = new GitService();

        String javaReposPath = "top_java_repos.json";
        String cCppReposPath = "top_c_cpp_repos.json";
        String rustReposPath = "top_rust_repos.json";

        // ensure cache files exist for each language or fetch from github api
        ensureJsonFileExists(javaReposPath, "java", jsonHandler);
        ensureJsonFileExists(cCppReposPath, "c++", jsonHandler);
        ensureJsonFileExists(rustReposPath, "rust", jsonHandler);

        List<Repo> javaRepos = jsonHandler.loadReposFromFile(javaReposPath);
        List<Repo> cCppRepos = jsonHandler.loadReposFromFile(cCppReposPath);
        List<Repo> rustRepos = jsonHandler.loadReposFromFile(rustReposPath);

        // calculate statistics and print modified file data for each language
        processLanguage("Java", javaRepos, gitService);
        processLanguage("C/C++", cCppRepos, gitService);
        processLanguage("Rust", rustRepos, gitService);

        // find and clone the most popular repo containing actual source code
        cloneMostPopularSourceRepo("Java", javaRepos, gitService);
        cloneMostPopularSourceRepo("C/C++", cCppRepos, gitService);
        cloneMostPopularSourceRepo("Rust", rustRepos, gitService);

        // persist all repository data to redis
        saveAllReposToRedis(javaRepos, cCppRepos, rustRepos, gitService);

        System.out.println("Processing complete!");
    }


    private static void ensureJsonFileExists(String filePath, String language, JsonHandler jsonHandler) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("File " + filePath + " not found. Fetching data from GitHub API...");
            try {
                String jsonData = fetchReposFromGitHub(language);

                // format json data for readability before saving
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

        // query parameter
        String languageQuery = language.replace("+", "%2B").replace(" ", "+");
        String query = String.format("language:%s", languageQuery);

        String url = String.format("%s?q=%s&sort=stars&order=desc&per_page=10",
                GITHUB_API_URL, query);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json");

        // add authentication token if available
        String apiKey = ConfigUtil.getGitHubApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
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
        int totalStars = 0;
        int totalForks = 0;
        int openIssues = 0;
        int newCommitsInForks = 0;

        // load fork data first for all repos
        for (Repo repo : repos) {
            gitService.fetchRecentCommitsWithCache(repo, language);
            // Fetch issues for each repo
            gitService.fetchIssues(repo);
        }

        // accumulate statistics from all repositories
        for (Repo repo : repos) {
            totalStars += repo.getStarCount();
            totalForks += repo.getForksCount();
            newCommitsInForks += countNewCommitsInForks(repo);
            openIssues += repo.getOpenIssuesCount();
        }

        System.out.println("\nLanguage: " + language);
        System.out.println("Total stars: " + totalStars);
        System.out.println("Total forks: " + totalForks);
        System.out.println("New commits in forked repos: " + newCommitsInForks);
        System.out.println("Open issues in top-10 repos: " + openIssues);

         // display the top 3 most modified files for each repository
        for (int i = 0; i < repos.size(); i++) {
            Repo repo = repos.get(i);

            List<String> top3ModifiedFiles = gitService.getTop3ModifiedFiles(repo);

            System.out.println((i + 1) + "/10: Top 3 modified files: ");
            System.out.println("Repo name: " + repo.getName());
            for (int j = 0; j < top3ModifiedFiles.size(); j++) {
                System.out.println("File name" + (j + 1) + ": " + top3ModifiedFiles.get(j));
            }
        }
    }

    private static int countNewCommitsInForks(Repo repo) {
        int count = 0;

        // limit to 20 forks
        int forksToCheck = Math.min(20, repo.getForks().size());

        // start from end of list to get the most recent forks
        int startIndex = Math.max(0, repo.getForks().size() - forksToCheck);

        for (int i = startIndex; i < repo.getForks().size(); i++) {
            Repo fork = repo.getForks().get(i);
            int forkCommits = fork.getCommitCount();
            count += forkCommits;
        }

        return count;
    }

    private static void cloneMostPopularSourceRepo(String language, List<Repo> repos, GitService gitService) {
        Repo mostPopular = null;

        // find repository with most stars that contains actual source code
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

        // store all repository data for each language to redis
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