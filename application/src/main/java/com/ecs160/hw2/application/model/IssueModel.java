package com.ecs160.hw2.application.model;

import com.ecs160.hw2.persistence.Id;
import com.ecs160.hw2.persistence.PersistableField;
import com.ecs160.hw2.persistence.PersistableObject;

import java.util.Date;

// issue model for persistence framework
@PersistableObject
public class IssueModel {
    @Id
    @PersistableField
    private String id;
    
    @PersistableField
    private Date date;  // maps to "Date" in redis
    
    @PersistableField
    private String Description;
    
    // getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Date getDate() {
        return date;
    }
    
    public void setDate(Date date) {
        this.date = date;
    }
    
    public String getDescription() {
        return Description;
    }
    
    public void setDescription(String description) {
        this.Description = description;
    }
}

