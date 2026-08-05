package com.sergio.planix.storage;

public enum StorageFolder {

    DOCUMENTS("documents"),
    PROFILE_PICTURES("profile_pictures");

    private final String path;

    StorageFolder(String path) { this.path = path; }

    public String path() { return path; }
}
