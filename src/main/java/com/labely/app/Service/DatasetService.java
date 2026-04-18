package com.labely.app.Service;

import com.labely.app.DTO.DatasetStatsResponse;
import com.labely.app.Entity.Annotation;
import com.labely.app.Entity.AnnotationStatus;
import com.labely.app.Entity.Detection;
import com.labely.app.Entity.ImageMetadata;
import com.labely.app.Entity.ImageStatus;
import com.labely.app.Entity.User;
import com.labely.app.Repository.AnnotationRepository;
import com.labely.app.Repository.ImageMetadataRepository;
import com.labely.app.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DatasetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageMetadataRepository imageMetadataRepository;

    @Autowired
    private AnnotationRepository annotationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public DatasetStatsResponse getStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DatasetStatsResponse s = new DatasetStatsResponse();
        s.setTotalImages(imageMetadataRepository.countByUser(user));
        s.setPending(imageMetadataRepository.countByUserAndStatus(user, ImageStatus.PENDING));
        s.setAnnotated(imageMetadataRepository.countByUserAndStatus(user, ImageStatus.ANNOTATED));
        s.setUnreviewed(annotationRepository.countByUserAndStatus(user, AnnotationStatus.UNREVIEWED));
        s.setApproved(annotationRepository.countByUserAndStatus(user, AnnotationStatus.APPROVED));
        s.setRejected(annotationRepository.countByUserAndStatus(user, AnnotationStatus.REJECTED));
        s.setTransferred(annotationRepository
                .findByUserAndStatusOrderByCreatedAtDesc(user, AnnotationStatus.APPROVED)
                .stream().filter(a -> Boolean.TRUE.equals(a.getTransferred())).count());
        return s;
    }

    @Transactional(readOnly = true)
    public byte[] export(String userEmail, String format, List<Long> annotationIds, boolean approvedOnly)
            throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Annotation> annotations;
        if (annotationIds != null && !annotationIds.isEmpty()) {
            annotations = annotationRepository.findByUserAndIdIn(user, annotationIds);
        } else if (approvedOnly) {
            annotations = annotationRepository.findByUserAndStatusOrderByCreatedAtDesc(user, AnnotationStatus.APPROVED);
        } else {
            annotations = annotationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        if (annotations.isEmpty()) {
            throw new RuntimeException("No annotations to export");
        }

        String fmt = format == null ? "yolo" : format.toLowerCase();
        switch (fmt) {
            case "yolo": return exportYolo(annotations);
            case "coco": return exportCoco(annotations);
            case "pascal": return exportPascal(annotations);
            case "csv": return exportCsv(annotations);
            case "tfrecord": return exportCsv(annotations);
            default: throw new RuntimeException("Unsupported format: " + format);
        }
    }

    private byte[] exportYolo(List<Annotation> annotations) throws IOException {
        Map<String, Integer> classMap = new LinkedHashMap<>();
        for (Annotation a : annotations) {
            classMap.putIfAbsent(a.getPrompt(), classMap.size());
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            StringBuilder classes = new StringBuilder();
            for (Map.Entry<String, Integer> e : classMap.entrySet()) {
                classes.append(e.getKey()).append("\n");
            }
            addEntry(zip, "classes.txt", classes.toString().getBytes());

            for (Annotation a : annotations) {
                int w = a.getImageWidth() == null ? 1 : Math.max(a.getImageWidth(), 1);
                int h = a.getImageHeight() == null ? 1 : Math.max(a.getImageHeight(), 1);
                StringBuilder sb = new StringBuilder();
                int classId = classMap.get(a.getPrompt());
                for (Detection d : a.getDetections()) {
                    double cx = ((d.getX1() + d.getX2()) / 2.0) / w;
                    double cy = ((d.getY1() + d.getY2()) / 2.0) / h;
                    double bw = Math.abs(d.getX2() - d.getX1()) / (double) w;
                    double bh = Math.abs(d.getY2() - d.getY1()) / (double) h;
                    sb.append(String.format("%d %.6f %.6f %.6f %.6f%n", classId, cx, cy, bw, bh));
                }
                String baseName = stripExtension(a.getImage().getFileName());
                addEntry(zip, "labels/" + baseName + ".txt", sb.toString().getBytes());
            }
        }
        return baos.toByteArray();
    }

    private byte[] exportCoco(List<Annotation> annotations) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("info", Map.of(
                "description", "LabelyAI export",
                "date_created", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
        root.put("licenses", List.of());

        Map<String, Integer> categoryMap = new LinkedHashMap<>();
        List<Map<String, Object>> categories = new ArrayList<>();

        List<Map<String, Object>> images = new ArrayList<>();
        List<Map<String, Object>> cocoAnnotations = new ArrayList<>();
        long annotationIdCounter = 1;

        for (Annotation a : annotations) {
            categoryMap.computeIfAbsent(a.getPrompt(), k -> {
                int id = categoryMap.size() + 1;
                categories.add(Map.of("id", id, "name", k, "supercategory", "object"));
                return id;
            });

            images.add(Map.of(
                    "id", a.getImage().getId(),
                    "file_name", a.getImage().getFileName(),
                    "width", a.getImageWidth() == null ? 0 : a.getImageWidth(),
                    "height", a.getImageHeight() == null ? 0 : a.getImageHeight(),
                    "coco_url", a.getImage().getFileUrl()
            ));

            int catId = categoryMap.get(a.getPrompt());
            for (Detection d : a.getDetections()) {
                int x = d.getX1();
                int y = d.getY1();
                int w = Math.abs(d.getX2() - d.getX1());
                int h = Math.abs(d.getY2() - d.getY1());
                Map<String, Object> ann = new LinkedHashMap<>();
                ann.put("id", annotationIdCounter++);
                ann.put("image_id", a.getImage().getId());
                ann.put("category_id", catId);
                ann.put("bbox", List.of(x, y, w, h));
                ann.put("area", w * h);
                ann.put("iscrowd", 0);
                ann.put("score", d.getScore());
                cocoAnnotations.add(ann);
            }
        }

        root.put("categories", categories);
        root.put("images", images);
        root.put("annotations", cocoAnnotations);

        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            addEntry(zip, "annotations/instances.json", json);
        }
        return baos.toByteArray();
    }

    private byte[] exportPascal(List<Annotation> annotations) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (Annotation a : annotations) {
                StringBuilder xml = new StringBuilder();
                xml.append("<annotation>\n");
                xml.append("  <filename>").append(escapeXml(a.getImage().getFileName())).append("</filename>\n");
                xml.append("  <size>\n");
                xml.append("    <width>").append(a.getImageWidth()).append("</width>\n");
                xml.append("    <height>").append(a.getImageHeight()).append("</height>\n");
                xml.append("    <depth>3</depth>\n");
                xml.append("  </size>\n");
                for (Detection d : a.getDetections()) {
                    xml.append("  <object>\n");
                    xml.append("    <name>").append(escapeXml(a.getPrompt())).append("</name>\n");
                    xml.append("    <score>").append(d.getScore()).append("</score>\n");
                    xml.append("    <bndbox>\n");
                    xml.append("      <xmin>").append(d.getX1()).append("</xmin>\n");
                    xml.append("      <ymin>").append(d.getY1()).append("</ymin>\n");
                    xml.append("      <xmax>").append(d.getX2()).append("</xmax>\n");
                    xml.append("      <ymax>").append(d.getY2()).append("</ymax>\n");
                    xml.append("    </bndbox>\n");
                    xml.append("  </object>\n");
                }
                xml.append("</annotation>\n");
                String baseName = stripExtension(a.getImage().getFileName());
                addEntry(zip, "annotations/" + baseName + ".xml", xml.toString().getBytes());
            }
        }
        return baos.toByteArray();
    }

    private byte[] exportCsv(List<Annotation> annotations) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("image_id,image_file_name,image_url,label,x1,y1,x2,y2,score,annotation_status\n");
        for (Annotation a : annotations) {
            for (Detection d : a.getDetections()) {
                sb.append(a.getImage().getId()).append(',')
                  .append(csv(a.getImage().getFileName())).append(',')
                  .append(csv(a.getImage().getFileUrl())).append(',')
                  .append(csv(a.getPrompt())).append(',')
                  .append(d.getX1()).append(',')
                  .append(d.getY1()).append(',')
                  .append(d.getX2()).append(',')
                  .append(d.getY2()).append(',')
                  .append(d.getScore()).append(',')
                  .append(a.getStatus().name())
                  .append('\n');
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            addEntry(zip, "annotations.csv", sb.toString().getBytes());
        }
        return baos.toByteArray();
    }

    private void addEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private String stripExtension(String filename) {
        if (filename == null) return "unknown";
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? filename : filename.substring(0, dot);
    }

    private String csv(String s) {
        if (s == null) return "";
        boolean needsQuoting = s.contains(",") || s.contains("\"") || s.contains("\n");
        String escaped = s.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
