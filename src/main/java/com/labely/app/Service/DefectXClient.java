package com.labely.app.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.labely.app.DTO.Sam3Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DefectXClient {

    private final RestTemplate restTemplate;

    @Value("${defectx.base-url}")
    private String baseUrl;

    public DefectXClient(@Qualifier("defectxRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isHealthy() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public String health() {
        ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + "/health", String.class);
        return resp.getBody();
    }

    private static ByteArrayResource asResource(byte[] bytes, String name) {
        final String effective = (name == null || name.isBlank()) ? "image.jpg" : name;
        return new ByteArrayResource(bytes) {
            @Override public String getFilename() { return effective; }
            @Override public long contentLength() { return bytes.length; }
        };
    }

    private static HttpEntity<ByteArrayResource> filePart(byte[] bytes, String name, String contentType) {
        MediaType mt;
        try {
            mt = (contentType == null || contentType.isBlank())
                    ? MediaType.IMAGE_JPEG : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mt = MediaType.IMAGE_JPEG;
        }
        HttpHeaders h = new HttpHeaders();
        h.setContentType(mt);
        return new HttpEntity<>(asResource(bytes, name), h);
    }

    public ReferenceResponse setReference(List<byte[]> bytesList,
                                          List<String> fileNames,
                                          List<String> contentTypes,
                                          String projectId,
                                          String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < bytesList.size(); i++) {
            body.add("files", filePart(bytesList.get(i), fileNames.get(i), contentTypes.get(i)));
        }
        if (projectId != null && !projectId.isBlank()) body.add("project_id", projectId);
        if (prompt != null && !prompt.isBlank()) body.add("prompt", prompt);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<ReferenceResponse> resp =
                restTemplate.postForEntity(baseUrl + "/reference", req, ReferenceResponse.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("DefectX /reference failed: " + resp.getStatusCode());
        }
        return resp.getBody();
    }

    public void deleteReference(String projectId) {
        try {
            restTemplate.delete(baseUrl + "/reference/" + projectId);
        } catch (Exception ignored) { }
    }

    public Sam3Response detect(byte[] imageBytes, String fileName, String contentType,
                               String projectId, String prompt, double confThreshold) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart(imageBytes, fileName, contentType));
        body.add("project_id", projectId);
        if (prompt != null && !prompt.isBlank()) body.add("prompt", prompt);
        body.add("conf_thresh", confThreshold);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Sam3Response> resp =
                restTemplate.postForEntity(baseUrl + "/detect", req, Sam3Response.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("DefectX /detect failed: " + resp.getStatusCode());
        }
        return resp.getBody();
    }

    public List<Sam3Response> detectBatch(List<byte[]> bytesList,
                                          List<String> fileNames,
                                          List<String> contentTypes,
                                          String projectId,
                                          String prompt,
                                          double confThreshold) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        for (int i = 0; i < bytesList.size(); i++) {
            body.add("files", filePart(bytesList.get(i), fileNames.get(i), contentTypes.get(i)));
        }
        body.add("project_id", projectId);
        if (prompt != null && !prompt.isBlank()) body.add("prompt", prompt);
        body.add("conf_thresh", confThreshold);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<BatchWrapper> resp =
                restTemplate.postForEntity(baseUrl + "/detect_batch", req, BatchWrapper.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("DefectX /detect_batch failed: " + resp.getStatusCode());
        }
        return resp.getBody().getResults();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReferenceResponse {
        @JsonProperty("project_id") private String projectId;
        @JsonProperty("num_refs")   private Integer numRefs;
        private String prompt;

        public String getProjectId() { return projectId; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public Integer getNumRefs() { return numRefs; }
        public void setNumRefs(Integer numRefs) { this.numRefs = numRefs; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchWrapper {
        @JsonProperty("results") private List<Sam3Response> results;
        public List<Sam3Response> getResults() { return results; }
        public void setResults(List<Sam3Response> results) { this.results = results; }
    }
}
