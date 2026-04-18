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

@Service
public class Sam3Client {

    private final RestTemplate restTemplate;

    @Value("${sam3.base-url}")
    private String baseUrl;

    public Sam3Client(@Qualifier("sam3RestTemplate") RestTemplate restTemplate) {
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

    public Sam3Response annotate(byte[] imageBytes,
                                 String fileName,
                                 String contentType,
                                 String prompt,
                                 String mode,
                                 double confThreshold,
                                 boolean largestComponent,
                                 boolean returnImages) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        final String effectiveName = (fileName == null || fileName.isBlank()) ? "image.jpg" : fileName;
        final MediaType effectiveContentType;
        try {
            effectiveContentType = contentType == null || contentType.isBlank()
                    ? MediaType.IMAGE_JPEG
                    : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            throw new RuntimeException("Invalid content type: " + contentType, e);
        }

        ByteArrayResource fileAsResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() { return effectiveName; }
            @Override
            public long contentLength() { return imageBytes.length; }
        };

        HttpHeaders filePartHeaders = new HttpHeaders();
        filePartHeaders.setContentType(effectiveContentType);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileAsResource, filePartHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        body.add("prompt", prompt);
        body.add("mode", mode);
        body.add("conf_thresh", confThreshold);
        body.add("largest_component", largestComponent);
        body.add("return_images", returnImages);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Sam3Response> response =
                restTemplate.postForEntity(baseUrl + "/annotate", request, Sam3Response.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("SAM3 /annotate failed: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public List<Sam3Response> annotateBatch(List<byte[]> imageBytesList,
                                            List<String> fileNames,
                                            List<String> contentTypes,
                                            String prompt,
                                            String mode,
                                            double confThreshold,
                                            boolean largestComponent,
                                            boolean returnImages) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (int i = 0; i < imageBytesList.size(); i++) {
            byte[] imageBytes = imageBytesList.get(i);
            final String effectiveName = (fileNames.get(i) == null || fileNames.get(i).isBlank())
                    ? "image" + i + ".jpg" : fileNames.get(i);
            final MediaType effectiveContentType;
            try {
                effectiveContentType = contentTypes.get(i) == null || contentTypes.get(i).isBlank()
                        ? MediaType.IMAGE_JPEG
                        : MediaType.parseMediaType(contentTypes.get(i));
            } catch (Exception e) {
                throw new RuntimeException("Invalid content type: " + contentTypes.get(i), e);
            }

            ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                @Override public String getFilename() { return effectiveName; }
                @Override public long contentLength() { return imageBytes.length; }
            };

            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(effectiveContentType);
            body.add("files", new HttpEntity<>(resource, partHeaders));
        }

        body.add("prompt", prompt);
        body.add("mode", mode);
        body.add("conf_thresh", confThreshold);
        body.add("largest_component", largestComponent);
        body.add("return_images", returnImages);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<BatchResponseWrapper> response =
                restTemplate.postForEntity(baseUrl + "/annotate_batch", request, BatchResponseWrapper.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("SAM3 /annotate_batch failed: " + response.getStatusCode());
        }

        return response.getBody().getResults();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchResponseWrapper {
        @JsonProperty("results")
        private List<Sam3Response> results;
        public List<Sam3Response> getResults() { return results; }
        public void setResults(List<Sam3Response> results) { this.results = results; }
    }
}
