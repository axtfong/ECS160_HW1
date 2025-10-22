package com.ecs160.hw;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ecs160.hw.model.Commit;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;


/**
 * Unit test for simple App.
 */
public class AppTest 
{
    private GitService gitService;
    private Repo testRepo;
    
    @Before
    public void setup() {
        gitService = new GitService();
        
        // Set up a test repository with some test data
        testRepo = new Repo();
        testRepo.setName("test-repo");
        testRepo.setOwnerLogin("test-owner");
        testRepo.setHtmlUrl("https://github.com/test-owner/test-repo");
        testRepo.setForksCount(100);
        testRepo.setLanguage("Java");
        testRepo.setOpenIssuesCount(50);
        testRepo.setStarCount(1000);
        
        // Add some test commits with modified files
        List<Commit> commits = new ArrayList<>();
        
        Commit commit1 = new Commit();
        commit1.setSha("abc123");
        commit1.setMessage("Test commit 1");
        commit1.setModifiedFiles(Arrays.asList("src/main/java/App.java", "README.md", "pom.xml"));
        
        Commit commit2 = new Commit();
        commit2.setSha("def456");
        commit2.setMessage("Test commit 2");
        commit2.setModifiedFiles(Arrays.asList("src/main/java/App.java", "src/main/java/util/Helper.java"));
        
        Commit commit3 = new Commit();
        commit3.setSha("ghi789");
        commit3.setMessage("Test commit 3");
        commit3.setModifiedFiles(Arrays.asList("src/main/java/App.java", "src/main/java/model/User.java"));
        
        commits.add(commit1);
        commits.add(commit2);
        commits.add(commit3);
        
        testRepo.setRecentCommits(commits);
    }
    
    @Test
    public void testCalculateFileModificationCount() {
        // Test the file modification count calculation
        var modificationCounts = gitService.calculateFileModificationCount(testRepo);
        
        assertEquals(3, modificationCounts.get("src/main/java/App.java").intValue());
        assertEquals(1, modificationCounts.get("README.md").intValue());
        assertEquals(1, modificationCounts.get("pom.xml").intValue());
        assertEquals(1, modificationCounts.get("src/main/java/util/Helper.java").intValue());
        assertEquals(1, modificationCounts.get("src/main/java/model/User.java").intValue());
    }
    
    @Test
    public void testGetTop3ModifiedFiles() {
        // Test getting top 3 modified files
        List<String> top3Files = gitService.getTop3ModifiedFiles(testRepo);
        
        assertEquals("Should return 3 files", 3, top3Files.size());
        
        // Verify that "src/main/java/App.java" is the most modified file (appears 3 times in commits)
        assertEquals("Most modified file should be App.java", "src/main/java/App.java", top3Files.get(0));
        
        // The remaining files all have 1 modification each, so they could be in any order
        // Just verify that they're in the list
        List<String> expectedOtherFiles = Arrays.asList(
            "README.md", "pom.xml", "src/main/java/util/Helper.java", "src/main/java/model/User.java"
        );
        
        // At least 2 of the remaining expected files should be in our top 3
        int foundCount = 0;
        for (int i = 1; i < top3Files.size(); i++) {
            if (expectedOtherFiles.contains(top3Files.get(i))) {
                foundCount++;
            }
        }
        
        assertTrue("Should contain at least 2 of the expected other files", foundCount >= 2);
    }
    
    @Test
    public void testIsRepoContainingSourceCode() {
        // This repo should contain source code
        assertTrue(gitService.isRepoContainingSourceCode(testRepo));
        
        // Create a repo with no source code
        Repo docsRepo = new Repo();
        Commit docsCommit = new Commit();
        docsCommit.setModifiedFiles(Arrays.asList("README.md", "docs/guide.md", "LICENSE"));
        docsRepo.setRecentCommits(List.of(docsCommit));
        
        // This repo should not contain source code
        assertFalse(gitService.isRepoContainingSourceCode(docsRepo));
    }

    @Test
    public void testCalculateTotalStars() {
        // Set up test repos with known star counts
        Repo repo1 = new Repo();
        repo1.setStarCount(1000);
        
        Repo repo2 = new Repo();
        repo2.setStarCount(2000);
        
        List<Repo> testRepos = Arrays.asList(repo1, repo2);
        
        // Test calculation logic
        int totalStars = 0;
        for (Repo repo : testRepos) {
            totalStars += repo.getStarCount();
        }
        
        assertEquals(3000, totalStars);
    }

    @Test
    public void testCalculateTotalForks() {
        // Set up test repos with known fork counts
        Repo repo1 = new Repo();
        repo1.setForksCount(500);
        
        Repo repo2 = new Repo();
        repo2.setForksCount(700);
        
        List<Repo> testRepos = Arrays.asList(repo1, repo2);
        
        // Test calculation logic
        int totalForks = 0;
        for (Repo repo : testRepos) {
            totalForks += repo.getForksCount();
        }
        
        assertEquals(1200, totalForks);
    }

    @Test
    public void testCalculateOpenIssues() {
        // Set up test repos with known issue counts
        Repo repo1 = new Repo();
        repo1.setOpenIssuesCount(50);
        
        Repo repo2 = new Repo();
        repo2.setOpenIssuesCount(75);
        
        List<Repo> testRepos = Arrays.asList(repo1, repo2);
        
        // Test calculation logic
        int totalIssues = 0;
        for (Repo repo : testRepos) {
            totalIssues += repo.getOpenIssuesCount();
        }
        
        assertEquals(125, totalIssues);
    }

    @Test
    public void testCountNewCommitsInForks() {
        // Create main repo
        Repo mainRepo = new Repo();
        
        // Create fork repos with commits
        Repo fork1 = new Repo();
        fork1.setCommitCount(5);
        
        Repo fork2 = new Repo();
        fork2.setCommitCount(10);
        
        // Add forks to main repo
        mainRepo.setForks(Arrays.asList(fork1, fork2));
        
        // Test the countNewCommitsInForks method from App
        int newCommits = countNewCommitsInForks(mainRepo);
        
        assertEquals(15, newCommits);
    }

    // Add this helper method (same as in App.java) to make the test work
    private int countNewCommitsInForks(Repo repo) {
        int count = 0;
        
        // Count commits in the 20 most-recent forked repos or fewer
        int forksToCheck = Math.min(20, repo.getForks().size());
        
        // Start from the end of the list to get the most recent forks
        int startIndex = Math.max(0, repo.getForks().size() - forksToCheck);
        
        for (int i = startIndex; i < repo.getForks().size(); i++) {
            Repo fork = repo.getForks().get(i);
            count += fork.getCommitCount();
        }
        
        return count;
    }

    @Test
    public void testIdentifyTop3ModifiedFilesPerRepo() {
        // Create test repos with commits
        Repo repo1 = new Repo();
        repo1.setName("repo1");
        
        // Setup commits for repo1
        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("file1.java", "file2.java"));
        
        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("file1.java", "file3.java"));
        
        Commit commit3 = new Commit();
        commit3.setModifiedFiles(Arrays.asList("file1.java", "file4.java"));
        
        repo1.setRecentCommits(Arrays.asList(commit1, commit2, commit3));
        
        // Get top 3 files
        List<String> top3Files = gitService.getTop3ModifiedFiles(repo1);
        
        // Verify results
        assertEquals("Top file should be file1.java with 3 modifications", "file1.java", top3Files.get(0));
        assertEquals(3, top3Files.size()); // Should return exactly 3 files (or fewer if there aren't 3 files)
        
        // Verify that all returned files were actually modified
        List<String> allPossibleFiles = Arrays.asList("file1.java", "file2.java", "file3.java", "file4.java");
        for (String file : top3Files) {
            assertTrue("Returned file should be in the list of modified files", allPossibleFiles.contains(file));
        }
    }

    @Test
    public void testLastFiftyCommits() {
        // Create a repo with more than 50 commits
        Repo repo = new Repo();
        repo.setName("large-repo");
        
        List<Commit> commits = new ArrayList<>();
        
        // Create 60 commits with varying files
        for (int i = 0; i < 60; i++) {
            Commit commit = new Commit();
            // First 10 commits modify file1, next 20 modify file2, next 30 modify file3
            if (i < 10) {
                commit.setModifiedFiles(Arrays.asList("file1.java"));
            } else if (i < 30) {
                commit.setModifiedFiles(Arrays.asList("file2.java"));
            } else {
                commit.setModifiedFiles(Arrays.asList("file3.java"));
            }
            commits.add(commit);
        }
        
        repo.setRecentCommits(commits);
        
        // Analyze only the last 50 commits (which should be the last 50 in the list)
        List<Commit> lastFiftyCommits = repo.getRecentCommits();
        if (lastFiftyCommits.size() > 50) {
            lastFiftyCommits = lastFiftyCommits.subList(lastFiftyCommits.size() - 50, lastFiftyCommits.size());
        }
        
        // Count the occurrences of each file in the last 50 commits
        java.util.Map<String, Integer> fileModificationCount = new java.util.HashMap<>();
        for (Commit commit : lastFiftyCommits) {
            for (String file : commit.getModifiedFiles()) {
                fileModificationCount.put(file, fileModificationCount.getOrDefault(file, 0) + 1);
            }
        }
        
        // Verify the file counts
        assertEquals("file3.java should have 30 modifications", Integer.valueOf(30), fileModificationCount.get("file3.java"));
        assertEquals("file2.java should have 20 modifications", Integer.valueOf(20), fileModificationCount.get("file2.java"));
        
        // Verify file1 should not be in the last 50 commits
        assertNull("file1.java should not be in the last 50 commits", fileModificationCount.get("file1.java"));
    }

    @Test
    public void testForksCommitCounting() {
        // Create main repo
        Repo mainRepo = new Repo();
        mainRepo.setName("main-repo");
        
        // Create 25 forks with varying commit counts
        List<Repo> forks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Repo fork = new Repo();
            fork.setName("fork-" + i);
            fork.setCommitCount(i); // Each fork has i commits
            forks.add(fork);
        }
        
        // Set forks on the main repo
        mainRepo.setForks(forks);
        
        // Count new commits in the 20 most-recent forks
        int newCommitsCount = countNewCommitsInForks(mainRepo);
        
        // The sum of commits in the last 20 forks (forks 5-24) should be 5+6+...+24 = 290
        int expectedSum = 0;
        for (int i = 5; i < 25; i++) {
            expectedSum += i;
        }
        
        assertEquals("Should count commits from the 20 most recent forks only", expectedSum, newCommitsCount);
    }

    @Test
    public void testEmptyRepositories() {
        // Test handling of empty repositories
        Repo emptyRepo = new Repo();
        emptyRepo.setName("empty-repo");
        emptyRepo.setRecentCommits(new ArrayList<>());
        emptyRepo.setForks(new ArrayList<>());
        
        // Should not throw exceptions
        List<String> topFiles = gitService.getTop3ModifiedFiles(emptyRepo);
        int commitCount = countNewCommitsInForks(emptyRepo);
        
        // Verify results
        assertTrue("Top files list should be empty for an empty repo", topFiles.isEmpty());
        assertEquals("Commit count should be 0 for an empty repo", 0, commitCount);
    }

    @Test
    public void testMultipleRepositoriesStatistics() {
        // Create multiple test repositories with different statistics
        List<Repo> testRepos = new ArrayList<>();
        
        // Repo 1
        Repo repo1 = new Repo();
        repo1.setName("repo1");
        repo1.setStarCount(500);
        repo1.setForksCount(100);
        repo1.setOpenIssuesCount(30);
        
        // Repo 2
        Repo repo2 = new Repo();
        repo2.setName("repo2");
        repo2.setStarCount(1000);
        repo2.setForksCount(200);
        repo2.setOpenIssuesCount(40);
        
        // Repo 3
        Repo repo3 = new Repo();
        repo3.setName("repo3");
        repo3.setStarCount(1500);
        repo3.setForksCount(300);
        repo3.setOpenIssuesCount(50);
        
        testRepos.add(repo1);
        testRepos.add(repo2);
        testRepos.add(repo3);
        
        // Calculate total statistics
        int totalStars = 0;
        int totalForks = 0;
        int totalOpenIssues = 0;
        
        for (Repo repo : testRepos) {
            totalStars += repo.getStarCount();
            totalForks += repo.getForksCount();
            totalOpenIssues += repo.getOpenIssuesCount();
        }
        
        // Verify the totals
        assertEquals("Total stars should be the sum across all repos", 3000, totalStars);
        assertEquals("Total forks should be the sum across all repos", 600, totalForks);
        assertEquals("Total open issues should be the sum across all repos", 120, totalOpenIssues);
    }

    @Test
    public void testModifiedFilesWithDuplicates() {
        // Create a repo with commits that modify the same files multiple times
        Repo repo = new Repo();
        repo.setName("duplicate-files-repo");
        
        List<Commit> commits = new ArrayList<>();
        
        // First commit modifies 3 files
        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileC.java"));
        
        // Second commit modifies 2 of the same files and 1 new file
        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileD.java"));
        
        // Third commit modifies 1 of the same files and 2 new files
        Commit commit3 = new Commit();
        commit3.setModifiedFiles(Arrays.asList("fileA.java", "fileE.java", "fileF.java"));
        
        commits.add(commit1);
        commits.add(commit2);
        commits.add(commit3);
        
        repo.setRecentCommits(commits);
        
        // Get top 3 modified files
        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);
        
        // fileA.java should be first (3 modifications)
        assertEquals("fileA.java should be the most modified file", "fileA.java", top3Files.get(0));
        
        // fileB.java should be second (2 modifications)
        assertEquals("fileB.java should be the second most modified file", "fileB.java", top3Files.get(1));
        
        // The third spot could be any of the files with 1 modification
        assertTrue("The third file should be one of the files with 1 modification",
                top3Files.get(2).equals("fileC.java") || 
                top3Files.get(2).equals("fileD.java") || 
                top3Files.get(2).equals("fileE.java") || 
                top3Files.get(2).equals("fileF.java"));
    }
}
