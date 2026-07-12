package com.voiceassess.backend.service;

import com.voiceassess.backend.model.AudioAssessment;
import com.voiceassess.backend.model.StagingAssessment;
import com.voiceassess.backend.model.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/**
 * Sends transcript + rubric context to DeepSeek V4 Pro for CBC assessment grading.
 * Returns raw JSON that gets stored in StagingAssessment.parsedJsonPayload.
 */
@Service
public class AssessmentService {

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String DEEPSEEK_CHAT_URL =
        "https://api.deepseek.com/chat/completions";

    public AssessmentService(
            @Value("${app.deepseek.api-key:dummy}") String apiKey,
            @Value("${app.deepseek.model:deepseek-v4-pro}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Builds the assessment prompt with session context, rubric, student roster,
     * and transcript — sends it to DeepSeek — returns the raw JSON string.
     */
    @SuppressWarnings("unchecked")
    public String assess(AudioAssessment assessment, StagingAssessment staging,
                         List<Student> classRoster) throws Exception {

        var rubric = assessment.getRubric();

        // build a simple JSON array of the class roster for the LLM
        var rosterJson = new ArrayList<Map<String, String>>();
        for (var s : classRoster) {
            var entry = new LinkedHashMap<String, String>();
            entry.put("studentId", s.getStudentId().toString());
            entry.put("studentName", s.getFullName());
            rosterJson.add(entry);
        }

        // use full transcript for LLM grading — fall back to snippet for legacy rows
        var transcript = staging.getFullTranscript();
        if (transcript == null || transcript.isBlank()) {
            transcript = staging.getTranscriptSnippet();
        }
        if (transcript == null || transcript.isBlank()) {
            throw new RuntimeException("No transcript available for assessment");
        }

        // cap at 4000 chars to avoid blowing the LLM context window on very long audio
        if (transcript.length() > 4000) {
            transcript = transcript.substring(0, 4000) + "... [truncated]";
        }

        var systemMsg = buildSystemPrompt();
        var userMsg = buildUserPrompt(assessment, rubric.getCompetencyDesc(),
            rubric.getRatingScale(), rosterJson, transcript);

        var reqBody = new LinkedHashMap<String, Object>();
        reqBody.put("model", model);
        reqBody.put("messages", List.of(
            Map.of("role", "system", "content", systemMsg),
            Map.of("role", "user", "content", userMsg)
        ));
        reqBody.put("response_format", Map.of("type", "json_object"));
        reqBody.put("temperature", 0.2);
        reqBody.put("thinking", Map.of("type", "enabled"));
        reqBody.put("reasoning_effort", "high");
        reqBody.put("stream", false);

        var jsonBody = mapper.writeValueAsString(reqBody);

        var request = HttpRequest.newBuilder()
            .uri(URI.create(DEEPSEEK_CHAT_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "DeepSeek returned " + response.statusCode() + ": " + response.body());
        }

        var respJson = mapper.readValue(response.body(), Map.class);

        @SuppressWarnings("unchecked")
        var choices = (List<Map<String, Object>>) respJson.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException(
                "DeepSeek response missing choices: " + response.body());
        }

        var message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) {
            throw new RuntimeException(
                "DeepSeek response missing message: " + response.body());
        }

        var content = (String) message.get("content");
        if (content == null || content.isBlank()) {
            throw new RuntimeException(
                "DeepSeek returned empty content: " + response.body());
        }

        return content.trim();
    }

    private String buildSystemPrompt() {
        return "You are a CBC (Competency-Based Curriculum) assessment engine for the "
            + "Kenyan education system. You evaluate student oral responses against KNEC "
            + "rubrics. You return ONLY valid JSON — no markdown, no explanation, no preamble. "
            + "The rating scale has exactly four levels: Below Expectations, Approaching "
            + "Expectations, Meeting Expectations, Exceeding Expectations.";
    }

    private String buildUserPrompt(AudioAssessment a, String competencyDesc,
                                    String ratingScale,
                                    List<Map<String, String>> roster,
                                    String transcript) throws Exception {

        var sb = new StringBuilder();

        sb.append("SESSION CONTEXT\n");
        sb.append("Subject: ").append(a.getSubject().getSubjectName()).append("\n");
        sb.append("Topic: ").append(nullToEmpty(a.getTopic())).append("\n");
        sb.append("Strand: ").append(a.getRubric().getStrand()).append("\n");
        sb.append("Sub-Strand: ").append(nullToEmpty(a.getRubric().getSubStrand())).append("\n");
        sb.append("Class: Grade ").append(a.getClassRoom().getGradeLevel())
            .append(" ").append(a.getClassRoom().getStreamName()).append("\n");
        sb.append("Date: ").append(a.getDate()).append("\n");

        var notes = a.getCuratedContext();
        if (notes != null && !notes.isBlank()) {
            sb.append("Teacher Notes: ").append(notes).append("\n");
        }

        sb.append("\nRUBRIC\n");
        sb.append("Competency: ").append(competencyDesc).append("\n");
        sb.append("Rating Scale: ").append(ratingScale).append("\n");

        sb.append("\nSTUDENT ROSTER\n");
        sb.append(mapper.writeValueAsString(roster)).append("\n");

        sb.append("\nTRANSCRIPT\n");
        sb.append(transcript).append("\n");

        sb.append("\nOUTPUT FORMAT\n");
        sb.append("Return a JSON object with this structure:\n");
        sb.append("{\n");
        sb.append("  \"session\": { \"subject\": \"...\", \"topic\": \"...\", ");
        sb.append("\"strand\": \"...\", \"subStrand\": \"...\", ");
        sb.append("\"className\": \"...\", \"date\": \"...\" },\n");
        sb.append("  \"assessment\": [\n");
        sb.append("    {\n");
        sb.append("      \"studentId\": \"exact UUID from roster\",\n");
        sb.append("      \"studentName\": \"string\",\n");
        sb.append("      \"ratingLevel\": \"one of the four scale values\",\n");
        sb.append("      \"confidence\": \"high or medium or low\",\n");
        sb.append("      \"evidence\": \"verbatim quote from transcript\",\n");
        sb.append("      \"strengths\": \"what the student did well\",\n");
        sb.append("      \"areasForImprovement\": \"what needs work\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"overallSummary\": \"brief summary of the session\"\n");
        sb.append("}\n");

        sb.append("\nRULES\n");
        sb.append("- Only include students who are actually mentioned in the transcript.\n");
        sb.append("- studentId must be an exact match from the roster above.\n");
        sb.append("- evidence must be a direct quote from the transcript.\n");
        sb.append("- If you cannot determine a rating, set confidence to \"low\" and explain why.\n");

        return sb.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
