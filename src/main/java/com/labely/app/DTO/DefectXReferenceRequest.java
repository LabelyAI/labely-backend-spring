package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Register a set of baseline images as the DefectX reference set")
public class DefectXReferenceRequest {

    @Schema(description = "IDs of images (already uploaded) to use as defect-free baselines", example = "[1,2,3]")
    private List<Long> imageIds;

    @Schema(description = "Optional existing project id (to replace an existing baseline)")
    private String projectId;

    @Schema(description = "Optional SAM3 object prompt bound to this baseline", example = "bottle")
    private String prompt;

    public List<Long> getImageIds() { return imageIds; }
    public void setImageIds(List<Long> imageIds) { this.imageIds = imageIds; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
}
