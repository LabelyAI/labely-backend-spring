package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Export dataset request")
public class ExportRequest {
    @Schema(description = "Export format", example = "yolo",
            allowableValues = {"yolo", "coco", "pascal", "csv", "tfrecord"})
    private String format;

    @Schema(description = "Optional list of annotation IDs to include. If null all approved annotations are exported.")
    private List<Long> annotationIds;

    @Schema(description = "If true include only approved annotations. Default true.")
    private Boolean approvedOnly;

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public List<Long> getAnnotationIds() { return annotationIds; }
    public void setAnnotationIds(List<Long> annotationIds) { this.annotationIds = annotationIds; }
    public Boolean getApprovedOnly() { return approvedOnly; }
    public void setApprovedOnly(Boolean approvedOnly) { this.approvedOnly = approvedOnly; }
}
