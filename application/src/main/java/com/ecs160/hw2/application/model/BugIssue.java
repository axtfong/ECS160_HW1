package com.ecs160.hw2.application.model;

/**
 * Issue format in JSON for bug reports.
 */
public class BugIssue {
    private String bug_type;
    private int line;
    private String description;
    private String filename;

    public BugIssue() {
    }

    public BugIssue(String bug_type, int line, String description, String filename) {
        this.bug_type = bug_type;
        this.line = line;
        this.description = description;
        this.filename = filename;
    }

    public String getBug_type() {
        return bug_type;
    }

    public void setBug_type(String bug_type) {
        this.bug_type = bug_type;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

