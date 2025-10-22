package com.ecs160.hw.model;

public class Owner {
    private String login;
    private int id;
    private String htmlUrl;
    private boolean siteAdmin;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

//    public String getHtmlUrl() {
//        return htmlUrl;
//    }
//
//    public void setHtmlUrl(String htmlUrl) {
//        this.htmlUrl = htmlUrl;
//    }
//
//    public boolean isSiteAdmin() {
//        return siteAdmin;
//    }
//
//    public void setSiteAdmin(boolean siteAdmin) {
//        this.siteAdmin = siteAdmin;
//    }
}