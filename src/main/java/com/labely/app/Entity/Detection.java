package com.labely.app.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "detections")
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotation_id", nullable = false)
    private Annotation annotation;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Integer x1;

    @Column(nullable = false)
    private Integer y1;

    @Column(nullable = false)
    private Integer x2;

    @Column(nullable = false)
    private Integer y2;

    @Column(nullable = false)
    private Double score;

    public Detection() {}

    public Detection(Annotation annotation, String label, Integer x1, Integer y1, Integer x2, Integer y2, Double score) {
        this.annotation = annotation;
        this.label = label;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Annotation getAnnotation() { return annotation; }
    public void setAnnotation(Annotation annotation) { this.annotation = annotation; }
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
