package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary counts across a user's dataset")
public class DatasetStatsResponse {
    private long totalImages;
    private long pending;
    private long annotated;
    private long unreviewed;
    private long approved;
    private long rejected;
    private long transferred;

    public DatasetStatsResponse() {}

    public long getTotalImages() { return totalImages; }
    public void setTotalImages(long totalImages) { this.totalImages = totalImages; }
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public long getAnnotated() { return annotated; }
    public void setAnnotated(long annotated) { this.annotated = annotated; }
    public long getUnreviewed() { return unreviewed; }
    public void setUnreviewed(long unreviewed) { this.unreviewed = unreviewed; }
    public long getApproved() { return approved; }
    public void setApproved(long approved) { this.approved = approved; }
    public long getRejected() { return rejected; }
    public void setRejected(long rejected) { this.rejected = rejected; }
    public long getTransferred() { return transferred; }
    public void setTransferred(long transferred) { this.transferred = transferred; }
}
