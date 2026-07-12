package com.voiceassess.backend.service;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates PDF reports — class grid, school-wide summary, compliance.
 * Uses Apache PDFBox for direct PDF output (no templating).
 */
@Service
public class PdfReportService {

    private final AudioAssessmentRepository audioRepo;
    private final AssessmentRecordRepository recordRepo;
    private final ClassRoomRepository classRepo;
    private final StudentEnrollmentRepository enrollmentRepo;
    private final SchoolRepository schoolRepo;
    private final SubjectRepository subjectRepo;
    private final TeacherRepository teacherRepo;

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 10;
    private static final float LINE_HEIGHT = 14;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PdfReportService(AudioAssessmentRepository audioRepo,
                            AssessmentRecordRepository recordRepo,
                            ClassRoomRepository classRepo,
                            StudentEnrollmentRepository enrollmentRepo,
                            SchoolRepository schoolRepo,
                            SubjectRepository subjectRepo,
                            TeacherRepository teacherRepo) {
        this.audioRepo = audioRepo;
        this.recordRepo = recordRepo;
        this.classRepo = classRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.schoolRepo = schoolRepo;
        this.subjectRepo = subjectRepo;
        this.teacherRepo = teacherRepo;
    }

    /**
     * Class Assessment Report — student x assessment matrix.
     */
    public byte[] generateClassReport(UUID classId) throws Exception {
        var cls = classRepo.findById(classId).orElseThrow(
                () -> new IllegalArgumentException("Class not found"));

        var assessments = audioRepo.findByClassRoomAndStatus(cls, "COMPLETED");
        var enrollments = enrollmentRepo.findByClassRoom(cls);

        var doc = new PDDocument();
        // landscape for wide grid
        var landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        var page = new PDPage(landscape);
        doc.addPage(page);

        float y = page.getMediaBox().getHeight() - MARGIN;
        var cs = new PDPageContentStream(doc, page);

        try {
            // title
            drawText(cs, "Class Assessment Report — " + clsDisplay(cls), MARGIN, y, FONT_BOLD, 14);
            y -= LINE_HEIGHT * 2;

            drawText(cs, "Generated: " + LocalDateTime.now().format(DATE_FMT), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            if (assessments.isEmpty()) {
                drawText(cs, "No completed assessments for this class.", MARGIN, y, FONT, FONT_SIZE);
                // let finally block handle close
            } else {
                // column widths
                float nameW = 140;
                float cellW = 80;
                float avgW = 50;

                // header row
                float x = MARGIN;
                drawCell(cs, x, y - LINE_HEIGHT, x + nameW, y, "Student", FONT_BOLD, FONT_SIZE);
                x += nameW;
                for (var a : assessments) {
                    var label = truncate(a.getSubject().getSubjectName() + "\n" + a.getTopic(), 30);
                    drawCell(cs, x, y - LINE_HEIGHT, x + cellW, y, label, FONT_BOLD, 7);
                    x += cellW;
                }
                drawCell(cs, x, y - LINE_HEIGHT, x + avgW, y, "Avg", FONT_BOLD, FONT_SIZE);
                y -= LINE_HEIGHT;

                // data rows
                for (var enr : enrollments) {
                    if (y < MARGIN + LINE_HEIGHT) {
                        cs.close();
                        page = new PDPage(landscape);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                    }

                    var student = enr.getStudent();
                    x = MARGIN;
                    drawCell(cs, x, y - LINE_HEIGHT, x + nameW, y, student.getFullName(), FONT, FONT_SIZE);
                    x += nameW;

                    float total = 0;
                    int count = 0;
                    for (var a : assessments) {
                        var recs = recordRepo.findByAudioAssessment(a);
                        String val = "-";
                        for (var r : recs) {
                            if (r.getStudent().getStudentId().equals(student.getStudentId())) {
                                val = String.format("%.1f", r.getScore());
                                total += r.getScore();
                                count++;
                                break;
                            }
                        }
                        drawCell(cs, x, y - LINE_HEIGHT, x + cellW, y, val, FONT, FONT_SIZE);
                        x += cellW;
                    }
                    var avg = count > 0 ? String.format("%.1f", total / count) : "-";
                    drawCell(cs, x, y - LINE_HEIGHT, x + avgW, y, avg, FONT_BOLD, FONT_SIZE);
                    y -= LINE_HEIGHT;
                }

                // footer
                y -= LINE_HEIGHT;
                var schoolName = cls.getSchool() != null ? cls.getSchool().getSchoolName() : "VoiceAssess";
                drawText(cs, "VoiceAssess — " + schoolName, MARGIN, y, FONT, 8);
            }
        } finally {
            cs.close();
        }

        return toBytes(doc);
    }

    /**
     * School-wide summary report — per-class and per-subject breakdowns.
     */
    public byte[] generateSchoolReport(UUID schoolId) throws Exception {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));
        var classes = classRepo.findBySchool(school);
        var subjects = subjectRepo.findBySchool(school);

        var doc = new PDDocument();
        var page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        try (var cs = new PDPageContentStream(doc, page)) {
            float y = page.getMediaBox().getHeight() - MARGIN;

            // title page
            drawText(cs, school.getSchoolName(), MARGIN, y, FONT_BOLD, 18);
            y -= LINE_HEIGHT * 2;
            drawText(cs, "School-Wide Assessment Report", MARGIN, y, FONT_BOLD, 14);
            y -= LINE_HEIGHT * 2;
            drawText(cs, "Generated: " + LocalDateTime.now().format(DATE_FMT), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // summary stats
            long totalStudents = 0;
            long totalAssessments = 0;
            for (var cls : classes) {
                totalStudents += enrollmentRepo.findByClassRoom(cls).size();
                totalAssessments += audioRepo.findByClassRoomAndStatus(cls, "COMPLETED").size();
            }

            drawText(cs, "Total Classes: " + classes.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cs, "Total Students: " + totalStudents, MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cs, "Total Subjects: " + subjects.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cs, "Total Completed Assessments: " + totalAssessments, MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // per-class breakdown
            drawText(cs, "Per-Class Breakdown", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            for (var cls : classes) {
                if (y < 100) { newPage(doc, page, cs); y = page.getMediaBox().getHeight() - MARGIN; }

                var compAssessments = audioRepo.findByClassRoomAndStatus(cls, "COMPLETED");
                drawText(cs, clsDisplay(cls) + " — " + compAssessments.size() + " assessments",
                        MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;

                // average per subject for this class
                Map<String, List<Float>> subjectScores = new LinkedHashMap<>();
                for (var a : compAssessments) {
                    var name = a.getSubject().getSubjectName();
                    var recs = recordRepo.findByAudioAssessment(a);
                    for (var r : recs) {
                        subjectScores.computeIfAbsent(name, k -> new ArrayList<>()).add(r.getScore());
                    }
                }
                for (var entry : subjectScores.entrySet()) {
                    var avg = entry.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    drawText(cs, "  " + entry.getKey() + ": " + String.format("%.1f", avg),
                            MARGIN, y, FONT, FONT_SIZE);
                    y -= LINE_HEIGHT;
                }
                y -= LINE_HEIGHT;
            }

            cs.close();
        }

        return toBytes(doc);
    }

    /**
     * CBC Compliance Report — assessment counts by subject and status.
     */
    public byte[] generateComplianceReport(UUID schoolId, Integer gradeLevel) throws Exception {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));

        var doc = new PDDocument();
        var page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        var allAssessments = audioRepo.findAll();
        // filter to this school
        var schoolAssessments = new ArrayList<AudioAssessment>();
        for (var a : allAssessments) {
            if (a.getTeacher() != null && a.getTeacher().getSchool() != null
                && a.getTeacher().getSchool().getSchoolId().equals(schoolId)) {
                if (gradeLevel == null || a.getClassRoom().getGradeLevel() == gradeLevel) {
                    schoolAssessments.add(a);
                }
            }
        }

        try (var cs = new PDPageContentStream(doc, page)) {
            float y = page.getMediaBox().getHeight() - MARGIN;

            var gradeLabel = gradeLevel != null ? "Grade " + gradeLevel : "All Grades";
            drawText(cs, school.getSchoolName() + " — CBC Compliance Report", MARGIN, y, FONT_BOLD, 14);
            y -= LINE_HEIGHT * 2;
            drawText(cs, "Grade Filter: " + gradeLabel, MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cs, "Generated: " + LocalDateTime.now().format(DATE_FMT), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            drawText(cs, "Total Assessments: " + schoolAssessments.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // by subject
            drawText(cs, "Assessments by Subject", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            Map<String, Long> bySubject = new LinkedHashMap<>();
            for (var a : schoolAssessments) {
                bySubject.merge(a.getSubject().getSubjectName(), 1L, Long::sum);
            }
            for (var e : bySubject.entrySet()) {
                drawText(cs, "  " + e.getKey() + ": " + e.getValue(), MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;
            }

            y -= LINE_HEIGHT;

            // by status
            drawText(cs, "Assessments by Status", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (var a : schoolAssessments) {
                byStatus.merge(a.getStatus(), 1L, Long::sum);
            }
            for (var e : byStatus.entrySet()) {
                drawText(cs, "  " + e.getKey() + ": " + e.getValue(), MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;
            }

            y -= LINE_HEIGHT * 2;
            drawText(cs, "VoiceAssess — " + school.getSchoolName(), MARGIN, y, FONT, 8);
            cs.close();
        }

        return toBytes(doc);
    }

    private String clsDisplay(ClassRoom cls) {
        return "Grade " + cls.getGradeLevel() + " " + cls.getStreamName();
    }

    private void drawText(PDPageContentStream cs, String text, float x, float y,
                          PDType1Font font, float size) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private void drawCell(PDPageContentStream cs, float x1, float y1, float x2, float y2,
                          String text, PDType1Font font, float size) throws Exception {
        // border
        cs.setLineWidth(0.5f);
        cs.addRect(x1, y1, x2 - x1, y2 - y1);
        cs.stroke();

        // text centered in cell
        var lines = text.split("\n");
        float textY = y1 + (y2 - y1) / 2 + (lines.length * size / 4);
        for (var line : lines) {
            float textW;
            try {
                textW = font.getStringWidth(line) / 1000 * size;
            } catch (Exception e) {
                textW = line.length() * size * 0.6f;
            }
            float tx = x1 + (x2 - x1 - textW) / 2;
            if (tx < x1 + 2) tx = x1 + 2;
            drawText(cs, truncate(line, 25), tx, textY, font, size);
            textY -= size + 2;
        }
    }

    private void newPage(PDDocument doc, PDPage current, PDPageContentStream cs) throws Exception {
        // this is a simplified helper — not used extensively
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 2) + "..";
    }

    private byte[] toBytes(PDDocument doc) throws Exception {
        var baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }
}
