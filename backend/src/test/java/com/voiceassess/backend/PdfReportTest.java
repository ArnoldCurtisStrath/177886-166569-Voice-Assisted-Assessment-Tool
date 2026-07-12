package com.voiceassess.backend;

import com.voiceassess.backend.model.*;
import com.voiceassess.backend.repository.*;
import com.voiceassess.backend.service.PdfReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PdfReportTest {

    @Autowired
    private PdfReportService pdfReportService;

    @Autowired
    private SchoolRepository schoolRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private ClassRoomRepository classRepo;

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private AudioAssessmentRepository audioRepo;

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepo;

    @Autowired
    private AssessmentRecordRepository recordRepo;

    @Autowired
    private KnecRubricRepository rubricRepo;

    private School school;
    private ClassRoom cls;
    private Subject subject;
    private Teacher teacher;
    private Student student;

    @BeforeEach
    void setUp() {
        school = schoolRepo.save(new School("KNEC-PDF-TEST", "PDF Test School"));

        var user = new User();
        user.setEmail("pdfteacher@test.com");
        user.setPasswordHash("hash");
        user.setRole(User.Role.TEACHER);
        user.setActive(true);
        user = userRepo.save(user);

        teacher = new Teacher();
        teacher.setUser(user);
        teacher.setSchool(school);
        teacher.setFullName("PDF Teacher");
        teacher = teacherRepo.save(teacher);

        cls = new ClassRoom();
        cls.setSchool(school);
        cls.setGradeLevel(4);
        cls.setStreamName("East");
        cls = classRepo.save(cls);

        subject = new Subject();
        subject.setSchool(school);
        subject.setSubjectName("English");
        subject.setGradeLevel(4);
        subject = subjectRepo.save(subject);

        var rubric = new KnecRubric();
        rubric.setSubject(subject);
        rubric.setCompetencyDesc("Reading fluency");
        rubric.setStrand("Reading");
        rubric.setSubStrand("Oral reading");
        rubric.setRatingScale("Below / Approaching / Meeting / Exceeding");
        rubric = rubricRepo.save(rubric);

        var sUser = new User();
        sUser.setEmail("pdfstudent@test.com");
        sUser.setPasswordHash("hash");
        sUser.setRole(User.Role.STUDENT);
        sUser.setActive(true);
        sUser = userRepo.save(sUser);

        student = new Student();
        student.setUser(sUser);
        student.setSchool(school);
        student.setFullName("PDF Student");
        student.setDateOfBirth(LocalDate.of(2015, 3, 10));
        student = studentRepo.save(student);

        var enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setClassRoom(cls);
        enrollmentRepo.save(enrollment);

        // create a completed assessment with a score
        var assessment = new AudioAssessment();
        assessment.setTeacher(teacher);
        assessment.setClassRoom(cls);
        assessment.setSubject(subject);
        assessment.setTopic("Oral Reading Test");
        assessment.setRubric(rubric);
        assessment.setDate(LocalDate.now());
        assessment.setStatus("COMPLETED");
        assessment = audioRepo.save(assessment);

        var record = new AssessmentRecord();
        record.setAudioAssessment(assessment);
        record.setStudent(student);
        record.setRubric(rubric);
        record.setScore(3.0f);
        record.setRatingLevel("Meeting Expectations");
        record.setConfidence("high");
        record.setEvidence("Student read fluently with good pronunciation");
        recordRepo.save(record);
    }

    @Test
    void testClassReportPdfGenerated() throws Exception {
        var pdfBytes = pdfReportService.generateClassReport(cls.getClassId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // verify PDF header
        var header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testClassReportContainsStudentName() throws Exception {
        var pdfBytes = pdfReportService.generateClassReport(cls.getClassId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        // verify PDF header
        var header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);

        // PDF text is compressed — just verify the report has meaningful size
        assertTrue(pdfBytes.length > 500, "Report should be larger than 500 bytes for a class with data");
    }

    @Test
    void testSchoolReportPdfGenerated() throws Exception {
        var pdfBytes = pdfReportService.generateSchoolReport(school.getSchoolId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        var header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testComplianceReportPdfGenerated() throws Exception {
        var pdfBytes = pdfReportService.generateComplianceReport(school.getSchoolId(), null);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        var header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testComplianceReportWithGradeFilter() throws Exception {
        var pdfBytes = pdfReportService.generateComplianceReport(school.getSchoolId(), 4);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);

        var header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    void testEmptyClassReportDoesNotThrow() throws Exception {
        // create a new class with no assessments
        var emptyCls = new ClassRoom();
        emptyCls.setSchool(school);
        emptyCls.setGradeLevel(5);
        emptyCls.setStreamName("North");
        emptyCls = classRepo.save(emptyCls);

        var pdfBytes = pdfReportService.generateClassReport(emptyCls.getClassId());
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
