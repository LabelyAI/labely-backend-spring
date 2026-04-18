package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Approve or reject an annotation")
public class ReviewDecisionRequest {
    @Schema(description = "Review decision", example = "APPROVED", allowableValues = {"APPROVED", "REJECTED", "UNREVIEWED"})
    private String decision;

    public ReviewDecisionRequest() {}

    public ReviewDecisionRequest(String decision) {
        this.decision = decision;
    }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
