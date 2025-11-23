package com.ecs160.hw2.application.model;

import com.ecs160.hw2.persistence.Id;
import com.ecs160.hw2.persistence.PersistableField;
import com.ecs160.hw2.persistence.PersistableObject;

import java.util.Date;
import java.util.List;

/**
 * Repository model for persistence framework.
 */
@PersistableObject
public class RepoModel {
    @Id
    @PersistableField
    private String id;
    
    @PersistableField
    private String Url;
    
    @PersistableField
    private Date CreatedAt;
    
    @PersistableField
    private String authorName;  // Maps to "Author Name" in Redis
    
    @PersistableField
    private String Issues;  // Comma-separated issue IDs
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUrl() {
        return Url;
    }
    
    public void setUrl(String url) {
        this.Url = url;
    }
    
    public Date getCreatedAt() {
        return CreatedAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.CreatedAt = createdAt;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getIssues() {
        return Issues;
    }
    
    public void setIssues(String issues) {
        this.Issues = issues;
    }
}

