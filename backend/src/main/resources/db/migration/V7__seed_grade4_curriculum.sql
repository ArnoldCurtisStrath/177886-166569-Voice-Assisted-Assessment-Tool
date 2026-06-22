-- V7 — Seed Grade 4 CBC curriculum data
-- English Language, Kiswahili, Science and Technology
-- Sources: 2024 PWPER rationalized curriculum designs (KICD)
-- All rubrics use the standard KNEC 4-level rating scale

-- widen rating_scale — the full 4-level KNEC scale is ~90 chars, V1 had VARCHAR(50)
ALTER TABLE knec_rubrics ALTER COLUMN rating_scale TYPE VARCHAR(150);

-- fix the existing English subject name to match official KICD naming
UPDATE subjects SET subject_name = 'English Language'
WHERE subject_id = 'f1000000-0000-0000-0000-000000000002';

-- insert Kiswahili and Science & Technology subjects
INSERT INTO subjects (subject_id, subject_name, grade_level, school_id) VALUES
    ('f1000000-0000-0000-0000-000000000003', 'Kiswahili', 4, 'a1000000-0000-0000-0000-000000000001'),
    ('f1000000-0000-0000-0000-000000000004', 'Science and Technology', 4, 'a1000000-0000-0000-0000-000000000001');

-- ── ENGLISH LANGUAGE (10 rubrics) ─────────────────────────────────────────────

INSERT INTO knec_rubrics (rubric_id, subject_id, competency_desc, strand, sub_strand, rating_scale) VALUES

    -- Listening and Speaking
    ('a2030000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can accurately pronounce target sounds, vowel blends, and thematic vocabulary, using them contextually in spoken sentences.',
     'Listening and Speaking', 'Pronunciation and Vocabulary',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2040000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can speak and recite texts using appropriate word stress, pitch, and intonation to convey exact grammatical intent and emotional nuance.',
     'Listening and Speaking', 'Word Stress and Intonation',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2050000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can verbally retell a trickster narrative or fable in the correct chronological sequence, describing characters and the central plot.',
     'Listening and Speaking', 'Oral Narratives',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Reading
    ('a2060000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can read a grade-appropriate text aloud and verbally articulate answers to both direct and inferential comprehension questions.',
     'Reading', 'Intensive Reading',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2070000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can verbally summarize the main ideas, characters, and plot points from independently read reference materials or storybooks.',
     'Reading', 'Extensive Reading',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2080000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can read poems and short passages aloud with correct pace, rhythm, and natural conversational expression.',
     'Reading', 'Fluency',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Language Use
    ('a2090000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can correctly identify and use regular/irregular nouns, personal pronouns, verbs in the simple present/past tense, and descriptive adjectives in spoken sentences.',
     'Language Use', 'Word Classes',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2100000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can verbally formulate correct interrogative questions and properly use determiners in spoken phrases.',
     'Language Use', 'Language Patterns',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Writing (voice-adapted)
    ('a2110000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can verbally spell commonly misspelt thematic words and dictate the correct placement of capitalization and terminal punctuation in a spoken sentence.',
     'Writing', 'Mechanics of Writing',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2120000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000002',
     'Can verbally outline and dictate a structured narrative composition or a functional message with a clear beginning, body, and conclusion.',
     'Writing', 'Creative and Functional Writing',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations');

-- ── KISWAHILI (9 rubrics) ─────────────────────────────────────────────────────

INSERT INTO knec_rubrics (rubric_id, subject_id, competency_desc, strand, sub_strand, rating_scale) VALUES

    -- Kusikiliza na Kuzungumza
    ('a2130000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can accurately pronounce complex Kiswahili syllables and tongue-twisters, demonstrating auditory discrimination and oral fluency.',
     'Kusikiliza na Kuzungumza', 'Matamshi Bora na Vitanza Ndimi',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2140000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally initiate and respond to appropriate greetings, farewells, and polite expressions based on context and age.',
     'Kusikiliza na Kuzungumza', 'Maamkuzi, Maagano na Maneno ya Upole',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2150000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally solve riddles, complete proverbs, and construct similes to enrich spoken language.',
     'Kusikiliza na Kuzungumza', 'Vitendawili, Methali na Tashbihi',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2160000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can sustain a contextual conversation and verbally narrate past events or personal experiences logically and coherently.',
     'Kusikiliza na Kuzungumza', 'Mazungumzo ya Kimuktadha na Masimulizi',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Kusoma
    ('a2170000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can read a Kiswahili passage aloud fluently and accurately answer verbal comprehension questions regarding the main ideas and inferred meanings.',
     'Kusoma', 'Kusoma kwa Ufahamu na Kina',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Kuandika (voice-adapted)
    ('a2180000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally dictate the structural components of an explanatory essay or a friendly letter in the correct sequence.',
     'Kuandika', 'Insha za Maelezo na Barua ya Kirafiki',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Sarufi
    ('a2190000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally construct sentences ensuring correct grammatical agreement across subject prefixes, object infixes, and verbs for various noun classes.',
     'Sarufi', 'Ngeli za Nomino',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2200000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally conjugate verbs into past, present, and future tenses, and correctly negate these verbs in spoken sentences.',
     'Sarufi', 'Nyakati na Hali',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2210000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000003',
     'Can verbally transform standard nouns into their augmentative and diminutive forms and use them contextually in speech.',
     'Sarufi', 'Ukubwa na Udogo wa Nomino',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations');

-- ── SCIENCE AND TECHNOLOGY (7 rubrics) ────────────────────────────────────────

INSERT INTO knec_rubrics (rubric_id, subject_id, competency_desc, strand, sub_strand, rating_scale) VALUES

    -- Living Things and Their Environment
    ('a2220000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally identify living versus non-living things, describe core biological characteristics, and distinguish vertebrates from invertebrates.',
     'Living Things and Their Environment', 'Plants and Animals',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2230000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally identify the primary organs of the human digestive tract, sequence the flow of food, and recommend hygienic practices for digestive health.',
     'Living Things and Their Environment', 'Human Digestive System',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Matter
    ('a2240000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally identify the three states of matter and logically explain the scientific principles determining why objects float or sink.',
     'Matter', 'Properties of Matter',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2250000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally categorize solid waste into decomposable and non-decomposable types, and explain practical methods for community waste management and water conservation.',
     'Matter', 'Management of Solid Waste and Water Conservation',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    -- Force and Energy
    ('a2260000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally define force as a push or pull, describe its effects on objects, and explain the concept of friction.',
     'Force and Energy', 'Force and Its Effects',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2270000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally identify sources of light and heat, explain the linear travel of light, and distinguish between materials that are good or bad conductors of heat.',
     'Force and Energy', 'Light and Heat',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations'),

    ('a2280000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000004',
     'Can verbally identify the main physical components of digital devices, explain their functions, and define coding through practical analogies of sequential patterns.',
     'Force and Energy', 'Digital Devices and Coding',
     'Below Expectations / Approaching Expectations / Meeting Expectations / Exceeding Expectations');
