package com.ecs160.hw;

import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;
import com.ecs160.hw.util.JsonHandler;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {
        JsonHandler jsonHandler = new JsonHandler();
        GitService gitService = new GitService();
        
        // Paths to JSON files
        String javaReposPath = "top_java_repos.json";
        String cCppReposPath = "top_c_cpp_repos.json";
        String rustReposPath = "top_rust_repos.json";
        
        // Load repositories from JSON files
        List<Repo> javaRepos = jsonHandler.loadReposFromFile(javaReposPath);
        List<Repo> cCppRepos = jsonHandler.loadReposFromFile(cCppReposPath);
        List<Repo> rustRepos = jsonHandler.loadReposFromFile(rustReposPath);
        
        // Process and display results for each language
        processLanguage("Java", javaRepos, gitService);
        processLanguage("C/C++", cCppRepos, gitService);
        processLanguage("Rust", rustRepos, gitService);
        
        // Find and clone the most popular non-tutorial repository for each language
        cloneMostPopularSourceRepo("Java", javaRepos, gitService);
        cloneMostPopularSourceRepo("C/C++", cCppRepos, gitService);
        cloneMostPopularSourceRepo("Rust", rustRepos, gitService);
        
        // Save all repositories to Redis
        saveAllReposToRedis(javaRepos, cCppRepos, rustRepos, gitService);
        
        System.out.println("Processing complete!");
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
            
            // Get Top-3 modified files for each repo
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
        
        // Save Java repositories
        for (Repo repo : javaRepos) {
            gitService.saveRepoToRedis(repo);
        }
        
        // Save C/C++ repositories
        for (Repo repo : cCppRepos) {
            gitService.saveRepoToRedis(repo);
        }
        
        // Save Rust repositories
        for (Repo repo : rustRepos) {
            gitService.saveRepoToRedis(repo);
        }
        
        System.out.println("All repositories saved to Redis successfully!");
    }
}
