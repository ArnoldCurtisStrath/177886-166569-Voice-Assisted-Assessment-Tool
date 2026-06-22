package com.voiceassess.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Talks to Groq's Whisper API for speech-to-text.
 */
@Service
public class TranscriptionService {

    private final HttpClient httpClient;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String GROQ_URL =
        "https://api.groq.com/openai/v1/audio/transcriptions";

    public TranscriptionService(@Value("${app.groq.api-key:dummy}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Sends an audio file to Groq for transcription and returns the text.
     * Uses whisper-large-v3 with verbose_json format.
     */
    public String transcribe(File audioFile) throws Exception {
        var boundary = "----VoiceAssess" + System.currentTimeMillis();
        var body = buildMultipartBody(boundary, audioFile);

        var request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq returned " + response.statusCode() + ": " + response.body());
        }

        var json = mapper.readValue(response.body(), Map.class);
        if (json == null || !json.containsKey("text")) {
            throw new RuntimeException("Groq response missing 'text' field: " + response.body());
        }

        return (String) json.get("text");
    }

    private byte[] buildMultipartBody(String boundary, File audioFile) throws IOException {
        var sb = new StringBuilder();
        var mimeType = Files.probeContentType(audioFile.toPath());
        if (mimeType == null) mimeType = "audio/webm";

        // file part
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(audioFile.getName()).append("\"\r\n");
        sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

        var header = sb.toString().getBytes();
        var fileBytes = Files.readAllBytes(audioFile.toPath());

        // other fields
        var fields = new StringBuilder();
        appendField(fields, boundary, "model", "whisper-large-v3");
        appendField(fields, boundary, "temperature", "0");
        appendField(fields, boundary, "response_format", "verbose_json");
        // closing boundary
        fields.append("--").append(boundary).append("--\r\n");
        var fieldsBytes = fields.toString().getBytes();

        // combine: header + file bytes + fields
        var result = new byte[header.length + fileBytes.length + fieldsBytes.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(fileBytes, 0, result, header.length, fileBytes.length);
        System.arraycopy(fieldsBytes, 0, result, header.length + fileBytes.length, fieldsBytes.length);
        return result;
    }

    private void appendField(StringBuilder sb, String boundary, String name, String value) {
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        sb.append(value).append("\r\n");
    }
}
