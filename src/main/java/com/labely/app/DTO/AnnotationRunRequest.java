package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Request to run SAM3 annotation on a set of images")
public class AnnotationRunRequest {

    @Schema(description = "IDs of images to annotate", example = "[1,2,3]")
    private List<Long> imageIds;

    @Schema(description = "Text prompt describing the object to annotate", example = "car")
    private String prompt;

    @Schema(description = "SAM3 routing mode", example = "sam3", defaultValue = "sam3")
    private String mode;

    @Schema(description = "Confidence threshold", example = "0.35", defaultValue = "0.35")
    private Double confThreshold;

    @Schema(description = "Keep only the largest connected mask component", defaultValue = "true")
    private Boolean largestComponent;

    @Schema(description = "If true SAM3 returns overlay + mask PNGs (base64). Default true.", defaultValue = "true")
    private Boolean returnImages;

    @Schema(description = "DefectX reference project id (required when mode=defectx)")
    private String defectxProjectId;

    public List<Long> getImageIds() { return imageIds; }
    public void setImageIds(List<Long> imageIds) { this.imageIds = imageIds; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Double getConfThreshold() { return confThreshold; }
    public void setConfThreshold(Double confThreshold) { this.confThreshold = confThreshold; }
    public Boolean getLargestComponent() { return largestComponent; }
    public void setLargestComponent(Boolean largestComponent) { this.largestComponent = largestComponent; }
    public Boolean getReturnImages() { return returnImages; }
    public void setReturnImages(Boolean returnImages) { this.returnImages = returnImages; }
    public String getDefectxProjectId() { return defectxProjectId; }
    public void setDefectxProjectId(String defectxProjectId) { this.defectxProjectId = defectxProjectId; }
}
