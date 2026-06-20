const mockData = {
  // all IDs (t1, s1, st1, etc.) cross-reference across arrays below
  // e.g. teacherClassAssignments[0].teacherId = 't1' points to teachers[0].id
  /* ----- Current User (set after login) ----- */
  currentUser: null,

  /* ----- School Profile ----- */
  school: {
    id: 's1',
    name: 'Moi Primary School — Nairobi',
    knecCode: 'KNEC-2041-NAI',
    address: 'Kilimani Road, Nairobi',
    phone: '+254 712 345 678',
    email: 'admin@moiprimary.ac.ke',
    activeTermId: 'term-2026-2'
  },

  /* ----- Academic Terms ----- */
  terms: [
    { id: 'term-2026-1', name: 'Term 1, 2026', startDate: '2026-01-06', endDate: '2026-04-03', status: 'ARCHIVED' },
    { id: 'term-2026-2', name: 'Term 2, 2026', startDate: '2026-05-05', endDate: '2026-08-07', status: 'ACTIVE' },
    { id: 'term-2026-3', name: 'Term 3, 2026', startDate: '2026-09-01', endDate: '2026-11-27', status: 'UPCOMING' }
  ],

  /* ----- Users (Credentials) -----
     In a real app, these come from the database.
     Passwords here are plaintext for dev only. */
  users: [
    { id: 'u1', fullName: 'Ms. Erika Lusiola', email: 'erika.lusiola@moiprimary.ac.ke', password: 'password', role: 'TEACHER', isActive: true },
    { id: 'u2', fullName: 'Arnold M. Curtis', email: 'arnold.mbici@moiprimary.ac.ke', password: 'password', role: 'ADMIN', isActive: true },
    { id: 'u3', fullName: 'Jane Wanjiku', email: 'jane.wanjiku@moiprimary.ac.ke', password: 'password', role: 'PARENT', isActive: true },
    { id: 'u4', fullName: 'Peter Otieno', email: 'peter.otieno@moiprimary.ac.ke', password: 'password', role: 'PARENT', isActive: true },
    { id: 'u5', fullName: 'Grace Muthoni', email: 'grace.muthoni@moiprimary.ac.ke', password: 'password', role: 'TEACHER', isActive: true },
    { id: 'u6', fullName: 'James Mutua', email: 'james.mutua@moiprimary.ac.ke', password: 'password', role: 'STUDENT', isActive: true },
    { id: 'u7', fullName: 'Amina Wekesa', email: 'amina.wekesa@moiprimary.ac.ke', password: 'password', role: 'STUDENT', isActive: true },
    { id: 'u8', fullName: 'Kevin Otieno', email: 'kevin.otieno@moiprimary.ac.ke', password: 'password', role: 'STUDENT', isActive: true },
    { id: 'u9', fullName: 'Ethan Mwangi', email: 'ethan.mwangi@moiprimary.ac.ke', password: 'password', role: 'STUDENT', isActive: true },
    { id: 'u10', fullName: 'Faith Nyambura', email: 'faith.nyambura@moiprimary.ac.ke', password: 'password', role: 'STUDENT', isActive: true }
  ],

  /* ----- Teacher Profiles ----- */
  teachers: [
    { id: 't1', userId: 'u1', fullName: 'Ms. Erika Lusiola', email: 'erika.lusiola@moiprimary.ac.ke', phone: '+254 711 111 111' },
    { id: 't2', userId: 'u5', fullName: 'Grace Muthoni', email: 'grace.muthoni@moiprimary.ac.ke', phone: '+254 722 222 222' }
  ],

  /* ----- Student Profiles ----- */
  students: [
    { id: 'st1', userId: 'u6', fullName: 'James Mutua', grade: 'Grade 4', parentId: 'p1' },
    { id: 'st2', userId: 'u7', fullName: 'Amina Wekesa', grade: 'Grade 4', parentId: 'p2' },
    { id: 'st3', userId: 'u8', fullName: 'Kevin Otieno', grade: 'Grade 4', parentId: 'p1' },
    { id: 'st4', userId: 'u9', fullName: 'Ethan Mwangi', grade: 'Grade 5', parentId: 'p2' },
    { id: 'st5', userId: 'u10', fullName: 'Faith Nyambura', grade: 'Grade 5', parentId: 'p1' }
  ],

  /* ----- Parent Profiles ----- */
  parents: [
    { id: 'p1', userId: 'u3', fullName: 'Jane Wanjiku', email: 'jane.wanjiku@moiprimary.ac.ke', phone: '+254 733 333 333' },
    { id: 'p2', userId: 'u4', fullName: 'Peter Otieno', email: 'peter.otieno@moiprimary.ac.ke', phone: '+254 744 444 444' }
  ],

  /* ----- Classes ----- */
  classes: [
    { id: 'c1', name: 'Grade 4B', gradeLevel: 'Grade 4', stream: 'B', teacherId: 't1', studentCount: 32, termId: 'term-2026-2' },
    { id: 'c2', name: 'Grade 5A', gradeLevel: 'Grade 5', stream: 'A', teacherId: 't1', studentCount: 28, termId: 'term-2026-2' },
    { id: 'c3', name: 'Grade 4C', gradeLevel: 'Grade 4', stream: 'C', teacherId: 't2', studentCount: 31, termId: 'term-2026-2' },
    { id: 'c4', name: 'Grade 5B', gradeLevel: 'Grade 5', stream: 'B', teacherId: 't2', studentCount: 26, termId: 'term-2026-2' }
  ],

  /* ----- CBC Subjects ----- */
  subjects: [
    { id: 'sub1', name: 'English Language', gradeLevel: 'Grade 4' },
    { id: 'sub2', name: 'Kiswahili', gradeLevel: 'Grade 4' },
    { id: 'sub3', name: 'Mathematics', gradeLevel: 'Grade 4' },
    { id: 'sub4', name: 'English Language', gradeLevel: 'Grade 5' },
    { id: 'sub5', name: 'Kiswahili', gradeLevel: 'Grade 5' },
    { id: 'sub6', name: 'Mathematics', gradeLevel: 'Grade 5' }
  ],

  /* ----- KNEC Rubrics ----- */
  rubrics: [
    { id: 'rub1', subjectId: 'sub1', strand: 'Listening & Speaking', subStrand: 'Comprehension', ratingScale: 'Exceeding Expectations / Meeting Expectations / Approaching Expectations / Below Expectations' },
    { id: 'rub2', subjectId: 'sub1', strand: 'Listening & Speaking', subStrand: 'Oral Presentation', ratingScale: 'Exceeding Expectations / Meeting Expectations / Approaching Expectations / Below Expectations' },
    { id: 'rub3', subjectId: 'sub2', strand: 'Kusoma', subStrand: 'Sarufi', ratingScale: 'Exceeding Expectations / Meeting Expectations / Approaching Expectations / Below Expectations' },
    { id: 'rub4', subjectId: 'sub3', strand: 'Numbers', subStrand: 'Fractions', ratingScale: 'Exceeding Expectations / Meeting Expectations / Approaching Expectations / Below Expectations' },
    { id: 'rub5', subjectId: 'sub2', strand: 'Kusoma', subStrand: 'Ufahamu', ratingScale: 'Exceeding Expectations / Meeting Expectations / Approaching Expectations / Below Expectations' }
  ],

  /* ----- Teacher-Class Assignments ----- */
  teacherClassAssignments: [
    { teacherId: 't1', classId: 'c1' },
    { teacherId: 't1', classId: 'c2' },
    { teacherId: 't2', classId: 'c3' },
    { teacherId: 't2', classId: 'c4' }
  ],

  /* ----- Teacher-Subject Assignments ----- */
  teacherSubjectAssignments: [
    { teacherId: 't1', subjectId: 'sub1' },
    { teacherId: 't1', subjectId: 'sub2' },
    { teacherId: 't2', subjectId: 'sub3' },
    { teacherId: 't2', subjectId: 'sub6' }
  ],

  /* ----- Audio Assessments ----- */
  // status flows: UPLOADED -> QUEUED -> TRANSCRIBING -> STAGED -> FINALIZED (or APPEALED)
  assessments: [
    { id: 'a1', teacherId: 't1', classId: 'c1', subjectId: 'sub1', rubricId: 'rub1', topic: 'Storytelling', date: '2026-05-22', status: 'FINALIZED', uploadTimestamp: '2026-05-22T09:15:00' },
    { id: 'a2', teacherId: 't1', classId: 'c1', subjectId: 'sub2', rubricId: 'rub3', topic: 'Hadithi Fupi', date: '2026-05-20', status: 'FINALIZED', uploadTimestamp: '2026-05-20T10:30:00' },
    { id: 'a3', teacherId: 't1', classId: 'c2', subjectId: 'sub1', rubricId: 'rub2', topic: 'Public Speaking', date: '2026-05-18', status: 'STAGED', uploadTimestamp: '2026-05-18T08:45:00' },
    { id: 'a4', teacherId: 't2', classId: 'c3', subjectId: 'sub3', rubricId: 'rub4', topic: 'Fractions Quiz', date: '2026-05-15', status: 'STAGED', uploadTimestamp: '2026-05-15T11:00:00' },
    { id: 'a5', teacherId: 't1', classId: 'c1', subjectId: 'sub1', rubricId: 'rub1', topic: 'Comprehension Test', date: '2026-05-25', status: 'UPLOADED', uploadTimestamp: '2026-05-25T14:20:00' }
  ],

  /* ----- Assessment Records (Finalized scores per student) ----- */
  assessmentRecords: [
    { id: 'ar1', assessmentId: 'a1', studentId: 'st1', score: 78 },
    { id: 'ar2', assessmentId: 'a1', studentId: 'st2', score: 85 },
    { id: 'ar3', assessmentId: 'a1', studentId: 'st3', score: 62 },
    { id: 'ar4', assessmentId: 'a2', studentId: 'st1', score: 82 },
    { id: 'ar5', assessmentId: 'a2', studentId: 'st2', score: 74 },
    { id: 'ar6', assessmentId: 'a2', studentId: 'st3', score: 91 }
  ],

  /* ----- Pending Reviews (Staging assessments needing teacher review) ----- */
  reviews: [
    {
      id: 'rev1',
      studentName: 'James Mutua',
      studentInitials: 'JM',
      subject: 'English',
      strand: 'Comprehension',
      date: '2026-05-22',
      issue: 'Unmatched: "Jane" not found in roster. Did you mean Janet?',
      status: 'PENDING_REVIEW'
    },
    {
      id: 'rev2',
      studentName: 'Amina Wekesa',
      studentInitials: 'AW',
      subject: 'Kiswahili',
      strand: 'Kusoma',
      date: '2026-05-21',
      issue: 'Sub-strand mismatch: scored "Sarufi" but rubric expects "Kusoma"',
      status: 'PENDING_REVIEW'
    },
    {
      id: 'rev3',
      studentName: 'Kevin Otieno',
      studentInitials: 'KO',
      subject: 'English',
      strand: 'Grammar',
      date: '2026-05-20',
      issue: 'Name verified. Criteria match: 4/5 strands confirmed.',
      status: 'PENDING_REVIEW'
    }
  ],

  /* ----- Feedback (LLM-generated) ----- */
  feedback: [
    { id: 'fb1', recordId: 'ar1', text: 'Good comprehension of the story. James identified the main characters correctly and gave a clear summary. Work on using more descriptive words when retelling events. Keep practicing reading aloud at home to improve fluency.' },
    { id: 'fb2', recordId: 'ar2', text: 'Excellent oral presentation, Amina. Your pronunciation is clear and you spoke with confidence. Your understanding of the story is strong. Next time, try to include more details about the setting.' },
    { id: 'fb3', recordId: 'ar3', text: 'Kevin understood the basic sequence of events but needs to work on comprehension of the moral lesson in the story. Practice discussing what characters learned with a partner. Good effort on vocabulary use.' }
  ],

  /* ----- Class Grid (for a specific class — Grade 4B English) ----- */
  // denormalized view — same data as assessmentRecords but flattened for the grid UI
  classGrid: [
    { studentId: 'st1', studentName: 'James Mutua', subject: 'English', strand: 'Comprehension', score: 78, date: '2026-05-22', status: 'FINALIZED' },
    { studentId: 'st2', studentName: 'Amina Wekesa', subject: 'English', strand: 'Comprehension', score: 85, date: '2026-05-22', status: 'FINALIZED' },
    { studentId: 'st3', studentName: 'Kevin Otieno', subject: 'English', strand: 'Comprehension', score: 62, date: '2026-05-22', status: 'FINALIZED' },
    { studentId: 'st1', studentName: 'James Mutua', subject: 'Kiswahili', strand: 'Sarufi', score: 82, date: '2026-05-20', status: 'FINALIZED' },
    { studentId: 'st2', studentName: 'Amina Wekesa', subject: 'Kiswahili', strand: 'Sarufi', score: 74, date: '2026-05-20', status: 'FINALIZED' },
    { studentId: 'st3', studentName: 'Kevin Otieno', subject: 'Kiswahili', strand: 'Sarufi', score: 91, date: '2026-05-20', status: 'FINALIZED' }
  ],

  /* ----- Dashboard Stats (per role) ----- */
  stats: {
    admin: {
      totalTeachers: 2,
      totalStudents: 5,
      activeClasses: 4,
      assessmentsThisTerm: 12,
      pendingReviews: 3,
      completedAssessments: 9
    },
    teacher: {
      classes: 4,
      pendingReviews: 3,
      reviewed: 8,
      totalStudents: 117
    },
    parent: {
      linkedChildren: 3
    },
    student: {
      totalAssessments: 5,
      averageScore: 78,
      pendingAppeals: 0
    }
  },

  /* ----- Student Progress (for Ethan Mwangi — matches wireframe) ----- */
  studentProgress: {
    studentId: 'st4',
    studentName: 'Ethan Mwangi',
    grade: 'Grade 5',
    term: 'Term 2, 2026',
    // weeks 1 through 5 — scores improve over time as the student progresses
    weeklyScores: [40, 52, 64, 74, 78],
    subjectBreakdown: [
      { subject: 'English', score: 78 },
      { subject: 'Kiswahili', score: 85 },
      { subject: 'Mathematics', score: 62 }
    ],
    recentAssessments: [
      { id: 'a3', title: 'English Comprehension', score: 78, date: '2026-05-20' },
      { id: 'a4', title: 'Kiswahili Kusoma', score: 85, date: '2026-05-18' },
      { id: 'a5', title: 'Mathematics Fractions', score: 62, date: '2026-05-15' }
    ]
  },

  /* ----- System Error Logs ----- */
  errorLogs: [
    { id: 'log1', type: 'STT_TIMEOUT', message: 'Speech-to-Text API timed out after 30s for assessment a2', timestamp: '2026-05-20T10:31:00' },
    { id: 'log2', type: 'JSON_PARSE', message: 'Invalid JSON received from LLM for assessment a3 — missing closing brace', timestamp: '2026-05-18T08:47:00' },
    { id: 'log3', type: 'SCHEMA_MISMATCH', message: 'LLM output for assessment a2 does not match KNEC rubric schema', timestamp: '2026-05-20T10:32:00' }
  ]
};

// Make available globally for other scripts to use
window.mockData = mockData;
