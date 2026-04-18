package com.labely.app.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single detection (bounding box + score) from SAM3")
public class DetectionDTO {
    private Long id;
    private String label;
    private Integer x1;
    private Integer y1;
    private Integer x2;
    private Integer y2;
    private Double score;

    public DetectionDTO() {}

    public DetectionDTO(Long id, String label, Integer x1, Integer y1, Integer x2, Integer y2, Double score) {
        this.id = id;
        this.label = label;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getX1() { return x1; }
    public void setX1(Integer x1) { this.x1 = x1; }
    public Integer getY1() { return y1; }
    public void setY1(Integer y1) { this.y1 = y1; }
    public Integer getX2() { return x2; }
    public void setX2(Integer x2) { this.x2 = x2; }
    public Integer getY2() { return y2; }
    public void setY2(Integer y2) { this.y2 = y2; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
