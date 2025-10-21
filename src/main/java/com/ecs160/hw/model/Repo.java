package com.ecs160.hw.model;

import java.util.ArrayList;
import java.util.List;

public class Repo {
    private String name;
    private String ownerLogin;
    private String htmlUrl;
    private int forksCount;
    private String language;
    private int openIssuesCount;
    private int starCount;
    private List<Repo> forks;
    private List<Commit> recentCommits;
    private List<Issue> issues;
    private int commitCount;

    public Repo() {
        this.forks = new ArrayList<>();
        this.recentCommits = new ArrayList<>();
        this.issues = new ArrayList<>();
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public void setOwnerLogin(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public int getForksCount() {
        return forksCount;
    }

    public void setForksCount(int forksCount) {
        this.forksCount = forksCount;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(int openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public List<Repo> getForks() {
        return forks;
    }

    public void setForks(List<Repo> forks) {
        this.forks = forks;
    }

    public List<Commit> getRecentCommits() {
        return recentCommits;
    }

    public void setRecentCommits(List<Commit> recentCommits) {
        this.recentCommits = recentCommits;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    public int getCommitCount() {
        return commitCount;
    }

    public void setCommitCount(int commitCount) {
        this.commitCount = commitCount;
    }

    public int getStarCount() {
        return starCount;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
    }
}
