package com.ecs160.hw2.application;

import com.ecs160.hw2.application.model.IssueModel;
import com.ecs160.hw2.application.model.RepoModel;
import com.ecs160.hw2.persistence.RedisDB;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Tests for the persistence framework.
 */
public class PersistenceFrameworkTest {
    private RedisDB redisDB;
    private RedisDB issueRedisDB;

    @Before
    public void setUp() {
        redisDB = new RedisDB("localhost", 6379, 0);
        issueRedisDB = new RedisDB("localhost", 6379, 1);
    }

    @Test
    public void testPersistAndLoadRepo() {
        String testRepoId = "repo-test-1";
        RepoModel repo = new RepoModel();
        repo.setId(testRepoId);
        repo.setUrl("https://github.com/test/repo");
        repo.setCreatedAt(new Date());
        repo.setAuthorName("testuser");
        repo.setIssues("iss-1,iss-2");

        try {
            // Persist
            boolean persisted = redisDB.persist(repo);
            assertTrue("Repository should be persisted", persisted);

            // Load
            RepoModel loadedRepo = new RepoModel();
            loadedRepo.setId(testRepoId);
            loadedRepo = (RepoModel) redisDB.load(loadedRepo);

            assertNotNull("Repository should be loaded", loadedRepo);
            assertEquals(testRepoId, loadedRepo.getId());
            assertEquals("https://github.com/test/repo", loadedRepo.getUrl());
            assertEquals("testuser", loadedRepo.getAuthorName());
            assertEquals("iss-1,iss-2", loadedRepo.getIssues());
        } finally {
            // Clean up test data
            redisDB.deleteKey(testRepoId);
        }
    }

    @Test
    public void testPersistAndLoadIssue() {
        String testIssueId = "iss-test-1";
        IssueModel issue = new IssueModel();
        issue.setId(testIssueId);
        issue.setDate(new Date());
        issue.setDescription("Test issue description");

        try {
            // Persist
            boolean persisted = issueRedisDB.persist(issue);
            assertTrue("Issue should be persisted", persisted);

            // Load
            IssueModel loadedIssue = new IssueModel();
            loadedIssue.setId(testIssueId);
            loadedIssue = (IssueModel) issueRedisDB.load(loadedIssue);

            assertNotNull("Issue should be loaded", loadedIssue);
            assertEquals(testIssueId, loadedIssue.getId());
            assertEquals("Test issue description", loadedIssue.getDescription());
        } finally {
            // Clean up test data
            issueRedisDB.deleteKey(testIssueId);
        }
    }
    
    @Test
    public void testPersistRepoWithEmptyFields() {
        // Test persisting repo with some empty fields
        String testRepoId = "repo-test-empty";
        RepoModel repo = new RepoModel();
        repo.setId(testRepoId);
        repo.setUrl("https://github.com/test/repo");
        repo.setCreatedAt(new Date());
        repo.setAuthorName(""); // Empty author name
        repo.setIssues(""); // Empty issues list
        
        try {
            boolean persisted = redisDB.persist(repo);
            assertTrue("Repository with empty fields should be persisted", persisted);
            
            RepoModel loadedRepo = new RepoModel();
            loadedRepo.setId(testRepoId);
            loadedRepo = (RepoModel) redisDB.load(loadedRepo);
            
            assertNotNull("Repository should be loaded", loadedRepo);
            assertEquals(testRepoId, loadedRepo.getId());
            assertEquals("https://github.com/test/repo", loadedRepo.getUrl());
        } finally {
            redisDB.deleteKey(testRepoId);
        }
    }
    
    @Test
    public void testPersistIssueWithEmptyDescription() {
        // Test persisting issue with empty description
        String testIssueId = "iss-test-empty";
        IssueModel issue = new IssueModel();
        issue.setId(testIssueId);
        issue.setDate(new Date());
        issue.setDescription(""); // Empty description
        
        try {
            boolean persisted = issueRedisDB.persist(issue);
            assertTrue("Issue with empty description should be persisted", persisted);
            
            IssueModel loadedIssue = new IssueModel();
            loadedIssue.setId(testIssueId);
            loadedIssue = (IssueModel) issueRedisDB.load(loadedIssue);
            
            assertNotNull("Issue should be loaded", loadedIssue);
            assertEquals(testIssueId, loadedIssue.getId());
            // Empty description should be preserved (may be empty string or null)
            // Just verify the issue was loaded successfully
            assertTrue("Issue should be loaded with empty description", 
                loadedIssue.getDescription() == null || loadedIssue.getDescription().isEmpty());
        } finally {
            issueRedisDB.deleteKey(testIssueId);
        }
    }
    
    @Test
    public void testLoadNonExistentRepo() {
        // Test loading a repo that doesn't exist in Redis
        RepoModel repo = new RepoModel();
        repo.setId("repo-nonexistent-12345");
        
        RepoModel loadedRepo = (RepoModel) redisDB.load(repo);
        assertNull("Non-existent repo should return null", loadedRepo);
    }
    
    @Test
    public void testLoadNonExistentIssue() {
        // Test loading an issue that doesn't exist in Redis
        IssueModel issue = new IssueModel();
        issue.setId("iss-nonexistent-12345");
        
        IssueModel loadedIssue = (IssueModel) issueRedisDB.load(issue);
        assertNull("Non-existent issue should return null", loadedIssue);
    }
    
    @Test
    public void testPersistRepoWithManyIssues() {
        // Test persisting repo with many issue IDs
        String testRepoId = "repo-test-many-issues";
        RepoModel repo = new RepoModel();
        repo.setId(testRepoId);
        repo.setUrl("https://github.com/test/repo");
        repo.setCreatedAt(new Date());
        repo.setAuthorName("testuser");
        
        // Create comma-separated list of many issue IDs
        StringBuilder issues = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            if (i > 1) issues.append(",");
            issues.append("iss-").append(i);
        }
        repo.setIssues(issues.toString());
        
        try {
            boolean persisted = redisDB.persist(repo);
            assertTrue("Repository with many issues should be persisted", persisted);
            
            RepoModel loadedRepo = new RepoModel();
            loadedRepo.setId(testRepoId);
            loadedRepo = (RepoModel) redisDB.load(loadedRepo);
            
            assertNotNull("Repository should be loaded", loadedRepo);
            assertEquals("Should preserve all issue IDs", issues.toString(), loadedRepo.getIssues());
        } finally {
            redisDB.deleteKey(testRepoId);
        }
    }
    
    @Test
    public void testPersistAndLoadIssueWithLongDescription() {
        // Test persisting issue with very long description
        String testIssueId = "iss-test-long";
        IssueModel issue = new IssueModel();
        issue.setId(testIssueId);
        issue.setDate(new Date());
        
        // Create a long description
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longDesc.append("This is a test description line. ");
        }
        issue.setDescription(longDesc.toString());
        
        try {
            boolean persisted = issueRedisDB.persist(issue);
            assertTrue("Issue with long description should be persisted", persisted);
            
            IssueModel loadedIssue = new IssueModel();
            loadedIssue.setId(testIssueId);
            loadedIssue = (IssueModel) issueRedisDB.load(loadedIssue);
            
            assertNotNull("Issue should be loaded", loadedIssue);
            assertEquals("Long description should be preserved", longDesc.toString(), loadedIssue.getDescription());
        } finally {
            issueRedisDB.deleteKey(testIssueId);
        }
    }
}

