package com.labely.app.DTO;

import java.util.List;

public class ManualAnnotationRequest {
    private List<ManualBox> boxes;
    private String label;

    public List<ManualBox> getBoxes() { return boxes; }
    public void setBoxes(List<ManualBox> boxes) { this.boxes = boxes; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public static class ManualBox {
        private Integer x1;
        private Integer y1;
        private Integer x2;
        private Integer y2;

        public Integer getX1() { return x1; }
        public void setX1(Integer x1) { this.x1 = x1; }
        public Integer getY1() { return y1; }
        public void setY1(Integer y1) { this.y1 = y1; }
        public Integer getX2() { return x2; }
        public void setX2(Integer x2) { this.x2 = x2; }
        public Integer getY2() { return y2; }
        public void setY2(Integer y2) { this.y2 = y2; }
    }
}
