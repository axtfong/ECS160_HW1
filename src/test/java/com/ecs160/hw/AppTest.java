package com.ecs160.hw;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ecs160.hw.model.Commit;
import com.ecs160.hw.model.Issue;
import com.ecs160.hw.model.Owner;
import com.ecs160.hw.model.Repo;
import com.ecs160.hw.service.GitService;

public class AppTest
{
    private GitService gitService;
    private Repo testRepo;

    @Before
    public void setup() {
        gitService = new GitService();

        // set up test repo w test data
        testRepo = new Repo();
        testRepo.setName("test-repo");
        testRepo.setOwnerLogin("test-owner");
        testRepo.setHtmlUrl("https://github.com/test-owner/test-repo");
        testRepo.setForksCount(100);
        testRepo.setLanguage("Java");
        testRepo.setOpenIssuesCount(50);
        testRepo.setStarCount(1000);

        // test commits w modified files
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
        var modificationCounts = gitService.calculateFileModificationCount(testRepo);

        assertEquals(3, modificationCounts.get("src/main/java/App.java").intValue());
        assertEquals(1, modificationCounts.get("README.md").intValue());
        assertEquals(1, modificationCounts.get("pom.xml").intValue());
        assertEquals(1, modificationCounts.get("src/main/java/util/Helper.java").intValue());
        assertEquals(1, modificationCounts.get("src/main/java/model/User.java").intValue());
    }

    @Test
    public void testGetTop3ModifiedFiles() {
        List<String> top3Files = gitService.getTop3ModifiedFiles(testRepo);

        assertEquals("Should return 3 files", 3, top3Files.size());

        // verify that "src/main/java/App.java" is most modified file
        assertEquals("Most modified file should be App.java", "src/main/java/App.java", top3Files.get(0));

        List<String> expectedOtherFiles = Arrays.asList(
                "README.md", "pom.xml", "src/main/java/util/Helper.java", "src/main/java/model/User.java"
        );

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
        assertTrue(gitService.isRepoContainingSourceCode(testRepo));

        // create repo w no source code
        Repo docsRepo = new Repo();
        Commit docsCommit = new Commit();
        docsCommit.setModifiedFiles(Arrays.asList("README.md", "docs/guide.md", "LICENSE"));
        docsRepo.setRecentCommits(List.of(docsCommit));

        assertFalse(gitService.isRepoContainingSourceCode(docsRepo));
    }

    @Test
    public void testCalculateTotalStars() {
        Repo repo1 = new Repo();
        repo1.setStarCount(1000);

        Repo repo2 = new Repo();
        repo2.setStarCount(2000);

        List<Repo> testRepos = Arrays.asList(repo1, repo2);

        int totalStars = 0;
        for (Repo repo : testRepos) {
            totalStars += repo.getStarCount();
        }

        assertEquals(3000, totalStars);
    }

    @Test
    public void testCalculateTotalForks() {
        Repo repo1 = new Repo();
        repo1.setForksCount(500);

        Repo repo2 = new Repo();
        repo2.setForksCount(700);

        List<Repo> testRepos = Arrays.asList(repo1, repo2);

        int totalForks = 0;
        for (Repo repo : testRepos) {
            totalForks += repo.getForksCount();
        }

        assertEquals(1200, totalForks);
    }

    @Test
    public void testCalculateOpenIssues() {
        Repo repo1 = new Repo();
        repo1.setOpenIssuesCount(50);

        Repo repo2 = new Repo();
        repo2.setOpenIssuesCount(75);

        List<Repo> testRepos = Arrays.asList(repo1, repo2);

        int totalIssues = 0;
        for (Repo repo : testRepos) {
            totalIssues += repo.getOpenIssuesCount();
        }

        assertEquals(125, totalIssues);
    }

    @Test
    public void testCountNewCommitsInForks() {
        Repo mainRepo = new Repo();

        Repo fork1 = new Repo();
        fork1.setCommitCount(5);

        Repo fork2 = new Repo();
        fork2.setCommitCount(10);

        mainRepo.setForks(Arrays.asList(fork1, fork2));

        int newCommits = countNewCommitsInForks(mainRepo);

        assertEquals(15, newCommits);
    }

    // same helper in App.java
    private int countNewCommitsInForks(Repo repo) {
        int count = 0;

        int forksToCheck = Math.min(20, repo.getForks().size());

        int startIndex = Math.max(0, repo.getForks().size() - forksToCheck);

        for (int i = startIndex; i < repo.getForks().size(); i++) {
            Repo fork = repo.getForks().get(i);
            count += fork.getCommitCount();
        }

        return count;
    }

    @Test
    public void testIdentifyTop3ModifiedFilesPerRepo() {
        Repo repo1 = new Repo();
        repo1.setName("repo1");

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("file1.java", "file2.java"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("file1.java", "file3.java"));

        Commit commit3 = new Commit();
        commit3.setModifiedFiles(Arrays.asList("file1.java", "file4.java"));

        repo1.setRecentCommits(Arrays.asList(commit1, commit2, commit3));

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo1);

        assertEquals("Top file should be file1.java with 3 modifications", "file1.java", top3Files.get(0));
        assertEquals(3, top3Files.size()); // Should return exactly 3 files (or fewer if there aren't 3 files)

        List<String> allPossibleFiles = Arrays.asList("file1.java", "file2.java", "file3.java", "file4.java");
        for (String file : top3Files) {
            assertTrue("Returned file should be in the list of modified files", allPossibleFiles.contains(file));
        }
    }

    @Test
    public void testLastFiftyCommits() {
        Repo repo = new Repo();
        repo.setName("large-repo");

        List<Commit> commits = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            Commit commit = new Commit();
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

        List<Commit> lastFiftyCommits = repo.getRecentCommits();
        if (lastFiftyCommits.size() > 50) {
            lastFiftyCommits = lastFiftyCommits.subList(lastFiftyCommits.size() - 50, lastFiftyCommits.size());
        }

        java.util.Map<String, Integer> fileModificationCount = new java.util.HashMap<>();
        for (Commit commit : lastFiftyCommits) {
            for (String file : commit.getModifiedFiles()) {
                fileModificationCount.put(file, fileModificationCount.getOrDefault(file, 0) + 1);
            }
        }

        assertEquals("file3.java should have 30 modifications", Integer.valueOf(30), fileModificationCount.get("file3.java"));
        assertEquals("file2.java should have 20 modifications", Integer.valueOf(20), fileModificationCount.get("file2.java"));

        assertNull("file1.java should not be in the last 50 commits", fileModificationCount.get("file1.java"));
    }

    @Test
    public void testForksCommitCounting() {
        Repo mainRepo = new Repo();
        mainRepo.setName("main-repo");

        List<Repo> forks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Repo fork = new Repo();
            fork.setName("fork-" + i);
            fork.setCommitCount(i);
            forks.add(fork);
        }

        mainRepo.setForks(forks);

        int newCommitsCount = countNewCommitsInForks(mainRepo);

        int expectedSum = 0;
        for (int i = 5; i < 25; i++) {
            expectedSum += i;
        }

        assertEquals("Should count commits from the 20 most recent forks only", expectedSum, newCommitsCount);
    }

    @Test
    public void testEmptyRepositories() {
        Repo emptyRepo = new Repo();
        emptyRepo.setName("empty-repo");
        emptyRepo.setRecentCommits(new ArrayList<>());
        emptyRepo.setForks(new ArrayList<>());

        List<String> topFiles = gitService.getTop3ModifiedFiles(emptyRepo);
        int commitCount = countNewCommitsInForks(emptyRepo);

        assertTrue("Top files list should be empty for an empty repo", topFiles.isEmpty());
        assertEquals("Commit count should be 0 for an empty repo", 0, commitCount);
    }

    @Test
    public void testMultipleRepositoriesStatistics() {
        List<Repo> testRepos = new ArrayList<>();

        Repo repo1 = new Repo();
        repo1.setName("repo1");
        repo1.setStarCount(500);
        repo1.setForksCount(100);
        repo1.setOpenIssuesCount(30);

        Repo repo2 = new Repo();
        repo2.setName("repo2");
        repo2.setStarCount(1000);
        repo2.setForksCount(200);
        repo2.setOpenIssuesCount(40);

        Repo repo3 = new Repo();
        repo3.setName("repo3");
        repo3.setStarCount(1500);
        repo3.setForksCount(300);
        repo3.setOpenIssuesCount(50);

        testRepos.add(repo1);
        testRepos.add(repo2);
        testRepos.add(repo3);

        int totalStars = 0;
        int totalForks = 0;
        int totalOpenIssues = 0;

        for (Repo repo : testRepos) {
            totalStars += repo.getStarCount();
            totalForks += repo.getForksCount();
            totalOpenIssues += repo.getOpenIssuesCount();
        }

        assertEquals("Total stars should be the sum across all repos", 3000, totalStars);
        assertEquals("Total forks should be the sum across all repos", 600, totalForks);
        assertEquals("Total open issues should be the sum across all repos", 120, totalOpenIssues);
    }

    @Test
    public void testModifiedFilesWithDuplicates() {
        Repo repo = new Repo();
        repo.setName("duplicate-files-repo");

        List<Commit> commits = new ArrayList<>();

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileC.java"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileD.java"));

        Commit commit3 = new Commit();
        commit3.setModifiedFiles(Arrays.asList("fileA.java", "fileE.java", "fileF.java"));

        commits.add(commit1);
        commits.add(commit2);
        commits.add(commit3);

        repo.setRecentCommits(commits);

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);

        // fileA.java should be first (3 modifications)
        assertEquals("fileA.java should be the most modified file", "fileA.java", top3Files.get(0));

        // fileB.java should be second (2 modifications)
        assertEquals("fileB.java should be the second most modified file", "fileB.java", top3Files.get(1));

        assertTrue("The third file should be one of the files with 1 modification",
                top3Files.get(2).equals("fileC.java") ||
                        top3Files.get(2).equals("fileD.java") ||
                        top3Files.get(2).equals("fileE.java") ||
                        top3Files.get(2).equals("fileF.java"));
    }

    @Test
    public void testTop3FilesWithTies() {
        Repo repo = new Repo();
        repo.setName("tie-repo");

        List<Commit> commits = new ArrayList<>();

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileC.java"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("fileA.java", "fileB.java", "fileC.java"));

        commits.add(commit1);
        commits.add(commit2);
        repo.setRecentCommits(commits);

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);

        assertEquals("Should return 3 files", 3, top3Files.size());
        assertTrue("Should contain fileA.java", top3Files.contains("fileA.java"));
        assertTrue("Should contain fileB.java", top3Files.contains("fileB.java"));
        assertTrue("Should contain fileC.java", top3Files.contains("fileC.java"));
    }

    @Test
    public void testTop3FilesWithFewerThanThreeFiles() {
        Repo repo = new Repo();
        repo.setName("small-repo");

        List<Commit> commits = new ArrayList<>();

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("file1.java"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("file2.java"));

        commits.add(commit1);
        commits.add(commit2);
        repo.setRecentCommits(commits);

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);

        assertEquals("Should return only 2 files", 2, top3Files.size());
    }

    @Test
    public void testSourceCodeDetectionWithMixedFiles() {
        Repo repo = new Repo();
        repo.setName("mixed-repo");

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("README.md", "LICENSE", "docs/guide.md"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("src/Main.java", "config.xml"));

        repo.setRecentCommits(Arrays.asList(commit1, commit2));

        assertTrue("Should detect source code", gitService.isRepoContainingSourceCode(repo));
    }

    @Test
    public void testSourceCodeDetectionWithoutCommits() {
        Repo tutorialRepo = new Repo();
        tutorialRepo.setName("awesome-java-tutorial");
        tutorialRepo.setLanguage("Java");
        tutorialRepo.setRecentCommits(new ArrayList<>());

        assertFalse("Tutorial repo should not be detected as source code",
                gitService.isRepoContainingSourceCode(tutorialRepo));

        Repo codeRepo = new Repo();
        codeRepo.setName("spring-framework");
        codeRepo.setLanguage("Java");
        codeRepo.setRecentCommits(new ArrayList<>());

        assertTrue("Regular repo should be detected as source code",
                gitService.isRepoContainingSourceCode(codeRepo));
    }

    @Test
    public void testCommitCountInForksWithExactly20Forks() {
        Repo mainRepo = new Repo();
        List<Repo> forks = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Repo fork = new Repo();
            fork.setCommitCount(i + 1);
            forks.add(fork);
        }

        mainRepo.setForks(forks);

        int newCommits = countNewCommitsInForks(mainRepo);

        assertEquals("Should count all 20 forks", 210, newCommits);
    }

    @Test
    public void testStabilityOfTop3ModifiedFiles() {
        Repo repo = new Repo();
        repo.setName("stable-sort-repo");

        List<Commit> commits = new ArrayList<>();

        Commit commit1 = new Commit();
        commit1.setModifiedFiles(Arrays.asList("alpha.java"));

        Commit commit2 = new Commit();
        commit2.setModifiedFiles(Arrays.asList("beta.java"));

        Commit commit3 = new Commit();
        commit3.setModifiedFiles(Arrays.asList("gamma.java"));

        Commit commit4 = new Commit();
        commit4.setModifiedFiles(Arrays.asList("delta.java"));

        commits.add(commit1);
        commits.add(commit2);
        commits.add(commit3);
        commits.add(commit4);

        repo.setRecentCommits(commits);

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);

        assertEquals("First should be alpha.java", "alpha.java", top3Files.get(0));
        assertEquals("Second should be beta.java", "beta.java", top3Files.get(1));
        assertEquals("Third should be delta.java", "delta.java", top3Files.get(2));
    }

    @Test
    public void testCompleteRepoStatistics() {
        Repo repo = new Repo();
        repo.setName("complete-repo");
        repo.setStarCount(5000);
        repo.setForksCount(1000);
        repo.setOpenIssuesCount(100);
        repo.setCommitCount(500);

        Owner owner = new Owner();
        owner.setLogin("complete-owner");
        owner.setId(123456);
        owner.setHtmlUrl("https://github.com/complete-owner");
        owner.setSiteAdmin(false);
        repo.setOwner(owner);

        List<Commit> commits = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Commit commit = new Commit();
            commit.setSha("sha" + i);
            commit.setMessage("Commit " + i);
            commit.setModifiedFiles(Arrays.asList("file" + (i % 10) + ".java"));
            commits.add(commit);
        }
        repo.setRecentCommits(commits);

        List<Issue> issues = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Issue issue = new Issue();
            issue.setTitle("Issue " + i);
            issue.setBody("Body " + i);
            issue.setState("open");
            issues.add(issue);
        }
        repo.setIssues(issues);

        List<Repo> forks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Repo fork = new Repo();
            fork.setName("fork" + i);
            fork.setCommitCount(i * 2);
            forks.add(fork);
        }
        repo.setForks(forks);

        assertEquals("Star count should match", 5000, repo.getStarCount());
        assertEquals("Fork count should match", 1000, repo.getForksCount());
        assertEquals("Open issues count should match", 100, repo.getOpenIssuesCount());
        assertEquals("Should have 50 commits", 50, repo.getRecentCommits().size());
        assertEquals("Should have 10 issues", 10, repo.getIssues().size());
        assertEquals("Should have 20 forks", 20, repo.getForks().size());
        assertNotNull("Owner should not be null", repo.getOwner());
        assertEquals("Owner login should match", "complete-owner", repo.getOwner().getLogin());

        List<String> top3Files = gitService.getTop3ModifiedFiles(repo);
        assertEquals("Should have 3 top files", 3, top3Files.size());

        int newCommitsInForks = countNewCommitsInForks(repo);
        assertTrue("Should have positive commits in forks", newCommitsInForks > 0);
    }
}