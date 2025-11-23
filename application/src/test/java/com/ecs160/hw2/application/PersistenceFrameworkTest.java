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
        RepoModel repo = new RepoModel();
        repo.setId("repo-test-1");
        repo.setUrl("https://github.com/test/repo");
        repo.setCreatedAt(new Date());
        repo.setAuthorName("testuser");
        repo.setIssues("iss-1,iss-2");

        // Persist
        boolean persisted = redisDB.persist(repo);
        assertTrue("Repository should be persisted", persisted);

        // Load
        RepoModel loadedRepo = new RepoModel();
        loadedRepo.setId("repo-test-1");
        loadedRepo = (RepoModel) redisDB.load(loadedRepo);

        assertNotNull("Repository should be loaded", loadedRepo);
        assertEquals("repo-test-1", loadedRepo.getId());
        assertEquals("https://github.com/test/repo", loadedRepo.getUrl());
        assertEquals("testuser", loadedRepo.getAuthorName());
        assertEquals("iss-1,iss-2", loadedRepo.getIssues());
    }

    @Test
    public void testPersistAndLoadIssue() {
        IssueModel issue = new IssueModel();
        issue.setId("iss-test-1");
        issue.setDate(new Date());
        issue.setDescription("Test issue description");

        // Persist
        boolean persisted = issueRedisDB.persist(issue);
        assertTrue("Issue should be persisted", persisted);

        // Load
        IssueModel loadedIssue = new IssueModel();
        loadedIssue.setId("iss-test-1");
        loadedIssue = (IssueModel) issueRedisDB.load(loadedIssue);

        assertNotNull("Issue should be loaded", loadedIssue);
        assertEquals("iss-test-1", loadedIssue.getId());
        assertEquals("Test issue description", loadedIssue.getDescription());
    }
}

