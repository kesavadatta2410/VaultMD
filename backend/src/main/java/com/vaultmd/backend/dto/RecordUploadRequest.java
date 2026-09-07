package com.vaultmd.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RecordUploadRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String type;

    @NotBlank
    private String text;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
