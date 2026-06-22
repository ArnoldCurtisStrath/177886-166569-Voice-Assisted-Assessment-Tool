package com.voiceassess.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.Map;

/**
 * Talks to Groq's Whisper API for speech-to-text.
 */
@Service
public class TranscriptionService {

    private final RestClient restClient;
    private final String apiKey;

    private static final String GROQ_TRANSCRIPTION_URL =
        "https://api.groq.com/openai/v1/audio/transcriptions";

    public TranscriptionService(@Value("${app.groq.api-key:dummy}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    /**
     * Sends an audio file to Groq for transcription and returns the text.
     * Uses whisper-large-v3 with verbose_json format.
     */
    public String transcribe(File audioFile) throws Exception {
        // build the multipart form body the way Groq expects it
        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new FileSystemResource(audioFile))
            .contentType(MediaType.APPLICATION_OCTET_STREAM);
        bodyBuilder.part("model", "whisper-large-v3");
        bodyBuilder.part("temperature", "0");
        bodyBuilder.part("response_format", "verbose_json");

        var response = restClient.post()
            .uri(GROQ_TRANSCRIPTION_URL)
            .header("Authorization", "Bearer " + apiKey)
            .body(bodyBuilder.build())
            .retrieve()
            .body(Map.class);

        if (response == null || !response.containsKey("text")) {
            throw new RuntimeException("Groq returned unexpected response: " + response);
        }

        return (String) response.get("text");
    }
}
