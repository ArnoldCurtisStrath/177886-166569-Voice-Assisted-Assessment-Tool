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

        // batch-load every record for these assessments once — the old per-cell
        // findByAudioAssessment hit the DB for every student x assessment slot
        var allRecords = recordRepo.findByAudioAssessmentIn(assessments);
        var recordsByAudio = new HashMap<UUID, List<AssessmentRecord>>();
        for (var r : allRecords) {
            recordsByAudio.computeIfAbsent(r.getAudioAssessment().getAudioId(), k -> new ArrayList<>()).add(r);
        }

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
            } else {
                // column widths
                float nameW = 140;
                float cellW = 80;
                float avgW = 50;

                // header row
                var headerXs = new ArrayList<Float>();
                float hx = MARGIN;
                headerXs.add(hx);
                hx += nameW;
                for (var a : assessments) {
                    headerXs.add(hx);
                    hx += cellW;
                }
                headerXs.add(hx);

                var headerLabels = new ArrayList<String>();
                headerLabels.add("Student");
                for (var a : assessments) {
                    headerLabels.add(truncate(a.getSubject().getSubjectName() + "\n" + a.getTopic(), 30));
                }
                headerLabels.add("Avg");

                drawHeaderRow(cs, headerXs, headerLabels, y - LINE_HEIGHT, y, FONT_BOLD, 7, FONT_SIZE);
                y -= LINE_HEIGHT;

                // data rows
                for (var enr : enrollments) {
                    if (y < MARGIN + LINE_HEIGHT) {
                        cs.close();
                        page = new PDPage(landscape);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                        // repeat the header so a multi-page grid stays readable
                        drawHeaderRow(cs, headerXs, headerLabels, y - LINE_HEIGHT, y, FONT_BOLD, 7, FONT_SIZE);
                        y -= LINE_HEIGHT;
                    }

                    var student = enr.getStudent();
                    float x = MARGIN;
                    drawCell(cs, x, y - LINE_HEIGHT, x + nameW, y, student.getFullName(), FONT, FONT_SIZE);
                    x += nameW;

                    float total = 0;
                    int count = 0;
                    for (var a : assessments) {
                        var recs = recordsByAudio.getOrDefault(a.getAudioId(), List.of());
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

        // batch-load completed assessments and their records for the whole school —
        // the old code queried per class and per assessment
        var allCompleted = new ArrayList<AudioAssessment>();
        var enrollCountByClass = new HashMap<UUID, Integer>();
        for (var cls : classes) {
            enrollCountByClass.put(cls.getClassId(), enrollmentRepo.findByClassRoom(cls).size());
            allCompleted.addAll(audioRepo.findByClassRoomAndStatus(cls, "COMPLETED"));
        }
        var allRecords = recordRepo.findByAudioAssessmentIn(allCompleted);
        var recordsByAudio = new HashMap<UUID, List<AssessmentRecord>>();
        for (var r : allRecords) {
            recordsByAudio.computeIfAbsent(r.getAudioAssessment().getAudioId(), k -> new ArrayList<>()).add(r);
        }

        var doc = new PDDocument();
        var cursor = new PageCursor(doc, new PDPage(PDRectangle.A4));

        try {
            float y = cursor.y;

            // title page
            drawText(cursor.cs, school.getSchoolName(), MARGIN, y, FONT_BOLD, 18);
            y -= LINE_HEIGHT * 2;
            drawText(cursor.cs, "School-Wide Assessment Report", MARGIN, y, FONT_BOLD, 14);
            y -= LINE_HEIGHT * 2;
            drawText(cursor.cs, "Generated: " + LocalDateTime.now().format(DATE_FMT), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // summary stats
            long totalStudents = 0;
            for (var cls : classes) {
                totalStudents += enrollCountByClass.getOrDefault(cls.getClassId(), 0);
            }

            drawText(cursor.cs, "Total Classes: " + classes.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cursor.cs, "Total Students: " + totalStudents, MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cursor.cs, "Total Subjects: " + subjects.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cursor.cs, "Total Completed Assessments: " + allCompleted.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // per-class breakdown
            drawText(cursor.cs, "Per-Class Breakdown", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            for (var cls : classes) {
                var compAssessments = audioRepo.findByClassRoomAndStatus(cls, "COMPLETED");
                // page break BEFORE the class line too — a class with many subjects
                // can push the next class off the page
                if (y < 100) { cursor.breakPage(doc); y = cursor.y; }

                drawText(cursor.cs, clsDisplay(cls) + " — " + compAssessments.size() + " assessments",
                        MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;

                // average per subject for this class
                Map<String, List<Float>> subjectScores = new LinkedHashMap<>();
                for (var a : compAssessments) {
                    var name = a.getSubject().getSubjectName();
                    var recs = recordsByAudio.getOrDefault(a.getAudioId(), List.of());
                    for (var r : recs) {
                        subjectScores.computeIfAbsent(name, k -> new ArrayList<>()).add(r.getScore());
                    }
                }
                for (var entry : subjectScores.entrySet()) {
                    // break inside the subject list — this is where the old report ran off-page
                    if (y < 100) { cursor.breakPage(doc); y = cursor.y; }

                    var avg = entry.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0);
                    drawText(cursor.cs, "  " + entry.getKey() + ": " + String.format("%.1f", avg),
                            MARGIN, y, FONT, FONT_SIZE);
                    y -= LINE_HEIGHT;
                }
                y -= LINE_HEIGHT;
            }

            cursor.close();
        } finally {
            cursor.close();
        }

        return toBytes(doc);
    }

    /**
     * CBC Compliance Report — assessment counts by subject and status.
     */
    public byte[] generateComplianceReport(UUID schoolId, Integer gradeLevel) throws Exception {
        var school = schoolRepo.findById(schoolId).orElseThrow(
                () -> new IllegalArgumentException("School not found"));

        // school-scoped query instead of pulling every assessment in the platform
        var schoolClasses = classRepo.findBySchool(school);
        var schoolAssessments = new ArrayList<AudioAssessment>();
        for (var cls : schoolClasses) {
            var clsAssessments = audioRepo.findByClassRoom(cls);
            for (var a : clsAssessments) {
                if (gradeLevel == null || a.getClassRoom().getGradeLevel() == gradeLevel) {
                    schoolAssessments.add(a);
                }
            }
        }

        var doc = new PDDocument();
        var cursor = new PageCursor(doc, new PDPage(PDRectangle.A4));

        try {
            float y = cursor.y;

            var gradeLabel = gradeLevel != null ? "Grade " + gradeLevel : "All Grades";
            drawText(cursor.cs, school.getSchoolName() + " — CBC Compliance Report", MARGIN, y, FONT_BOLD, 14);
            y -= LINE_HEIGHT * 2;
            drawText(cursor.cs, "Grade Filter: " + gradeLabel, MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT;
            drawText(cursor.cs, "Generated: " + LocalDateTime.now().format(DATE_FMT), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            drawText(cursor.cs, "Total Assessments: " + schoolAssessments.size(), MARGIN, y, FONT, FONT_SIZE);
            y -= LINE_HEIGHT * 2;

            // by subject
            drawText(cursor.cs, "Assessments by Subject", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            Map<String, Long> bySubject = new LinkedHashMap<>();
            for (var a : schoolAssessments) {
                bySubject.merge(a.getSubject().getSubjectName(), 1L, Long::sum);
            }
            for (var e : bySubject.entrySet()) {
                // a school with many subjects used to run off the single page
                if (y < 100) { cursor.breakPage(doc); y = cursor.y; }

                drawText(cursor.cs, "  " + e.getKey() + ": " + e.getValue(), MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;
            }

            y -= LINE_HEIGHT;

            // by status
            drawText(cursor.cs, "Assessments by Status", MARGIN, y, FONT_BOLD, 12);
            y -= LINE_HEIGHT * 2;

            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (var a : schoolAssessments) {
                byStatus.merge(a.getStatus(), 1L, Long::sum);
            }
            for (var e : byStatus.entrySet()) {
                if (y < 100) { cursor.breakPage(doc); y = cursor.y; }

                drawText(cursor.cs, "  " + e.getKey() + ": " + e.getValue(), MARGIN, y, FONT, FONT_SIZE);
                y -= LINE_HEIGHT;
            }

            y -= LINE_HEIGHT * 2;
            drawText(cursor.cs, "VoiceAssess — " + school.getSchoolName(), MARGIN, y, FONT, 8);
            cursor.close();
        } finally {
            cursor.close();
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

    // small cursor so page breaks can swap both the page and its content stream
    private static class PageCursor {
        PDPage page;
        PDPageContentStream cs;
        float y;

        PageCursor(PDDocument doc, PDPage startPage) throws Exception {
            this.page = startPage;
            doc.addPage(startPage);
            this.cs = new PDPageContentStream(doc, startPage);
            this.y = startPage.getMediaBox().getHeight() - MARGIN;
        }

        // closes the current stream, appends a fresh page, reopens a stream on it
        void breakPage(PDDocument doc) throws Exception {
            cs.close();
            page = new PDPage(page.getMediaBox());
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void close() throws Exception {
            cs.close();
        }
    }

    private void drawHeaderRow(PDPageContentStream cs, List<Float> xs, List<String> labels,
                               float y1, float y2, PDType1Font font, float cellFont, float labelFont) throws Exception {
        for (int i = 0; i < xs.size() - 1; i++) {
            float size = (i == 0 || i == xs.size() - 1) ? labelFont : cellFont;
            drawCell(cs, xs.get(i), y1, xs.get(i + 1), y2, labels.get(i), font, size);
        }
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
