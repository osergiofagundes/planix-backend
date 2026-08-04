package com.sergio.planix.storage;

/**
 * As subpastas de {@code planix.upload-dir}. Cada tipo de arquivo tem a sua, para não misturar
 * anexo de cartão com foto de perfil na mesma pasta.
 */
public enum StorageFolder {

    DOCUMENTS("documents"),
    PROFILE_PICTURES("profile_pictures");

    private final String path;

    StorageFolder(String path) { this.path = path; }

    public String path() { return path; }
}
