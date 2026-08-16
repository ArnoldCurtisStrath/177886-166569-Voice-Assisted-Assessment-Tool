package com.voiceassess.backend;

import com.voiceassess.backend.controller.TeacherController;
import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReviewAppealTest {

    @Autowired private TeacherController controller;
    @Autowired private UserRepository userRepo;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private TeacherRepository teacherRepo;
    @Autowired private ClassRoomRepository classRepo;
    @Autowired private SubjectRepository subjectRepo;
    @Autowired private KnecRubricRepository rubricRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private StudentEnrollmentRepository enrollmentRepo;
    @Autowired private AudioAssessmentRepository audioRepo;
    @Autowired private StagingAssessmentRepository stagingRepo;
    @Autowired private AssessmentRecordRepository recordRepo;
    @Autowired private FeedbackRepository feedbackRepo;
    @Autowired private AssessmentVersionRepository versionRepo;
    @Autowired private AssessmentAppealRepository appealRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    private User teacherUser;
    private Teacher teacher;
    private Student student;
    private StagingAssessment staging;
    private AssessmentRecord record;
    private AssessmentAppeal appeal;
    private UUID stagingId;

    @BeforeEach
    void setUp() {
        var school = schoolRepo.save(new School("KNEC-WP5-TEST", "WP5 Test School"));

        teacherUser = new User();
        teacherUser.setEmail("teacher-wp5@test.com");
        teacherUser.setPasswordHash(passwordEncoder.encode("password123"));
        teacherUser.setRole(User.Role.TEACHER);
        teacherUser.setActive(true);
        teacherUser = userRepo.save(teacherUser);

        teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacher.setSchool(school);
        teacher.setFullName("WP5 Teacher");
        teacher = teacherRepo.save(teacher);

        var cls = new ClassRoom();
        cls.setSchool(school);
        cls.setGradeLevel(4);
        cls.setStreamName("East");
        cls = classRepo.save(cls);

        var subject = new Subject();
        subject.setSchool(school);
        subject.setSubjectName("Mathematics");
        subject.setGradeLevel(4);
        subject = subjectRepo.save(subject);

        var rubric = new KnecRubric();
        rubric.setSubject(subject);
        rubric.setCompetencyDesc("Can solve word problems");
        rubric.setStrand("Numbers");
        rubric.setSubStrand("Operations");
        rubric.setRatingScale("1-4");
        rubric = rubricRepo.save(rubric);

        var studentUser = new User();
        studentUser.setEmail("student-wp5@test.com");
        studentUser.setPasswordHash(passwordEncoder.encode("password123"));
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setActive(true);
        studentUser = userRepo.save(studentUser);

        student = new Student();
        student.setUser(studentUser);
        student.setSchool(school);
        student.setFullName("WP5 Student");
        student.setDateOfBirth(LocalDate.of(2015, 3, 10));
        student = studentRepo.save(student);

        var enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setClassRoom(cls);
        enrollmentRepo.save(enrollment);

        var audio = new AudioAssessment();
        audio.setTeacher(teacher);
        audio.setClassRoom(cls);
        audio.setSubject(subject);
        audio.setRubric(rubric);
        audio.setTopic("Fractions word problems");
        audio.setDate(LocalDate.now());
        audio = audioRepo.save(audio);

        staging = new StagingAssessment();
        staging.setAudioAssessment(audio);
        staging.setRubric(rubric);
        staging.setTranscriptSnippet("Student solved the problem correctly");
        staging.setFullTranscript("Full transcript here");
        staging.setStatus("PENDING_REVIEW");
        staging = stagingRepo.save(staging);
        stagingId = staging.getStagingId();

        record = new AssessmentRecord();
        record.setAudioAssessment(audio);
        record.setStudent(student);
        record.setRubric(rubric);
        record.setScore(3.0f);
        record.setRatingLevel("Meeting Expectations");
        record.setConfidence("medium");
        record.setEvidence("Solved the problem in the transcript");
        record = recordRepo.save(record);

        var feedback = new Feedback();
        feedback.setAssessmentRecord(record);
        feedback.setQualitativeFeedback("Strengths: Good working\nAreas to improve: Check units");
        feedbackRepo.save(feedback);

        appeal = new AssessmentAppeal();
        appeal.setAudioAssessment(audio);
        appeal.setStudent(student);
        appeal.setReason("I think I deserve a higher score");
        appeal.setStatus("PENDING");
        appeal = appealRepo.save(appeal);
    }

    private Map<String, Object> editPayloadFor(String rating, String strengths, String areas) {
        return Map.of("assessment", List.of(Map.of(
            "studentId", student.getStudentId().toString(),
            "studentName", student.getFullName(),
            "ratingLevel", rating,
            "confidence", "medium",
            "evidence", "Solved the problem in the transcript",
            "strengths", strengths,
            "areasForImprovement", areas)));
    }

    @Test void testEditNoopDoesNotCreateVersion() {
        // identical data — should not churn versions or flip the status
        var resp = controller.editReview(teacherUser, stagingId, editPayloadFor("Meeting Expectations", "Good working", "Check units"));
        assertEquals(200, resp.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) resp.getBody();
        assertEquals(0, body.get("recordsUpdated"));
        assertEquals("PENDING_REVIEW", stagingRepo.findById(stagingId).get().getStatus());
        assertTrue(versionRepo.findByAssessmentRecordOrderByVersionNumberDesc(record).isEmpty());
    }

    @Test void testEditChangedRatingCreatesVersion() {
        var resp = controller.editReview(teacherUser, stagingId, editPayloadFor("Exceeding Expectations", "Good working", "Check units"));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("EDITED", stagingRepo.findById(stagingId).get().getStatus());
        var versions = versionRepo.findByAssessmentRecordOrderByVersionNumberDesc(record);
        assertEquals(1, versions.size());
        // record now reflects the new rating; the version snapshot holds the old score
        assertEquals(4.0f, recordRepo.findById(record.getRecordId()).get().getScore());
        assertTrue(versions.get(0).getJsonData().contains("\"score\":3.0"));
    }

    @Test void testEditEmptyListRejected() {
        var resp = controller.editReview(teacherUser, stagingId, Map.of("assessment", List.of()));
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test void testAppealAdjustRejectsOutOfRangeScore() {
        var body = Map.of("action", "ADJUSTED",
            "editedData", Map.of("score", 99, "ratingLevel", "Exceeding Expectations"));
        var resp = controller.resolveAppeal(teacherUser, appeal.getAppealId(), body);
        assertEquals(400, resp.getStatusCode().value());
        assertEquals("PENDING", appealRepo.findById(appeal.getAppealId()).get().getStatus());
    }

    @Test void testAppealAdjustRejectsNanScore() {
        // a string can't be parsed as a number — treated like NaN/absent
        var body = Map.of("action", "ADJUSTED",
            "editedData", Map.of("score", "abc", "ratingLevel", "Exceeding Expectations"));
        var resp = controller.resolveAppeal(teacherUser, appeal.getAppealId(), body);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test void testAppealAdjustAcceptsValidScore() {
        var body = Map.of("action", "ADJUSTED",
            "editedData", Map.of("score", 3.5, "ratingLevel", "Exceeding Expectations", "feedback", "Improved work"),
            "resolutionNote", "Gave credit for method");
        var resp = controller.resolveAppeal(teacherUser, appeal.getAppealId(), body);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("ADJUSTED", appealRepo.findById(appeal.getAppealId()).get().getStatus());
        assertEquals(3.5f, recordRepo.findById(record.getRecordId()).get().getScore());
    }

    @Test void testAppealUpholdWithoutScore() {
        var resp = controller.resolveAppeal(teacherUser, appeal.getAppealId(), Map.of("action", "UPHELD"));
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("UPHELD", appealRepo.findById(appeal.getAppealId()).get().getStatus());
    }

    @Test void testListAppealsIncludesResolved() {
        controller.resolveAppeal(teacherUser, appeal.getAppealId(), Map.of("action", "DISMISSED", "resolutionNote", "Evidence supports score"));
        var resp = controller.listAppeals(teacherUser);
        assertEquals(200, resp.getStatusCode().value());
        var list = (List<?>) resp.getBody();
        assertEquals(1, list.size());
        Map<?, ?> item = (Map<?, ?>) list.get(0);
        assertEquals("DISMISSED", item.get("status"));
        assertEquals("Evidence supports score", item.get("resolutionNote"));
        assertEquals("WP5 Teacher", item.get("resolvedBy"));
    }
}
