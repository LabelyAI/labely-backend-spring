package com.labely.app.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sam3Response {

    private String prompt;
    private String mode;

    @JsonProperty("num_instances")
    private Integer numInstances;

    private List<Sam3Detection> detections;

    @JsonProperty("image_size")
    private Sam3ImageSize imageSize;

    @JsonProperty("mask_png_b64")
    private String maskPngB64;

    @JsonProperty("overlay_png_b64")
    private String overlayPngB64;

    private String filename;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getNumInstances() { return numInstances; }
    public void setNumInstances(Integer numInstances) { this.numInstances = numInstances; }
    public List<Sam3Detection> getDetections() { return detections; }
    public void setDetections(List<Sam3Detection> detections) { this.detections = detections; }
    public Sam3ImageSize getImageSize() { return imageSize; }
    public void setImageSize(Sam3ImageSize imageSize) { this.imageSize = imageSize; }
    public String getMaskPngB64() { return maskPngB64; }
    public void setMaskPngB64(String maskPngB64) { this.maskPngB64 = maskPngB64; }
    public String getOverlayPngB64() { return overlayPngB64; }
    public void setOverlayPngB64(String overlayPngB64) { this.overlayPngB64 = overlayPngB64; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sam3Detection {
        private Integer x1;
        private Integer y1;
        private Integer x2;
        private Integer y2;
        private Double score;

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sam3ImageSize {
        private Integer w;
        private Integer h;

        public Integer getW() { return w; }
        public void setW(Integer w) { this.w = w; }
        public Integer getH() { return h; }
        public void setH(Integer h) { this.h = h; }
    }
}
