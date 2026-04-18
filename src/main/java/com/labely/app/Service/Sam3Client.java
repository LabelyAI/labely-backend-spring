package com.labely.app.Service;

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
}
