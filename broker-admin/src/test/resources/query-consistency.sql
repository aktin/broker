CREATE TEMPORARY TABLE temp_analysis(name VARCHAR(20), value VARCHAR(255));
INSERT INTO temp_analysis(name,value) VALUES('version','0.2');
INSERT INTO temp_analysis(name,value) VALUES('pat_total', (
 SELECT COUNT(*) FROM i2b2crcdata.patient_dimension
));
INSERT INTO temp_analysis(name,value) VALUES('vis_total', (
 SELECT COUNT(*) FROM i2b2crcdata.visit_dimension
));
-- Anzahl Patienten mit zugeordneten Daten
INSERT INTO temp_analysis(name,value) VALUES('vis_w_facts', (
 SELECT COUNT(DISTINCT f.encounter_num) 
 FROM i2b2crcdata.observation_fact f 
  INNER JOIN i2b2crcdata.visit_dimension v 
  ON f.encounter_num=v.encounter_num
));
-- Anzahl Fälle mit zugeordneten Daten
INSERT INTO temp_analysis(name,value) VALUES('pat_w_facts', (
 SELECT COUNT(DISTINCT f.patient_num) 
 FROM i2b2crcdata.observation_fact f 
  INNER JOIN i2b2crcdata.patient_dimension p 
  ON f.patient_num=p.patient_num
));
-- Duplikate Fälle: Gleicher Patient, andere Fallnummer aber gleicher Aufnahmezeitpunkt
INSERT INTO temp_analysis(name,value) VALUES('vis_dupl_adm', (
SELECT COUNT(*) 
 FROM i2b2crcdata.visit_dimension v1 
  INNER JOIN i2b2crcdata.visit_dimension v2 
   ON v1.patient_num=v2.patient_num
    AND v1.encounter_num!=v2.encounter_num
    AND v1.start_date=v2.start_date
));

-- Duplikate Daten: Gleicher Patient, gleiche Fallnummer aber mehrfache Einträge (DB korrupt)
INSERT INTO temp_analysis(name,value) VALUES('vis_dupl_fact', (
SELECT COUNT(c) FROM (SELECT f.patient_num, f.encounter_num, COUNT(*) AS c
 FROM i2b2crcdata.observation_fact f
  WHERE f.concept_cd LIKE 'AKTIN:IPVI:%'
 GROUP BY f.patient_num, f.encounter_num
 ) AS sub WHERE c > 1
));


-- Anzahl CDAs nach Software-Version
INSERT INTO temp_analysis(name,value) SELECT concept_cd, count(*)
from i2b2crcdata.observation_fact 
where concept_cd LIKE 'AKTIN:IPVI:%'
 group by concept_cd;

-- Anzahl CDAs nach Template-Version
INSERT INTO temp_analysis(name,value) SELECT concept_cd, count(*)
from i2b2crcdata.observation_fact 
where concept_cd LIKE 'AKTIN:ITTI:%'
 group by concept_cd;


INSERT INTO temp_analysis(name,value) VALUES('icd10_invalid', (
SELECT COUNT(*)
 FROM i2b2crcdata.observation_fact
 WHERE concept_cd LIKE 'ICD10GM:%' 
  AND modifier_cd = '@'
  AND concept_cd !~* '^icd10gm:[a-z][0-9][0-9]'
));

INSERT INTO temp_analysis(name,value) VALUES('icd10_total', (
SELECT COUNT(*)
 FROM i2b2crcdata.observation_fact
 WHERE concept_cd LIKE 'ICD10GM:%' 
  AND modifier_cd = '@'
));

INSERT INTO temp_analysis(name,value) VALUES('fact_start_first', (
SELECT MIN(start_date) FROM i2b2crcdata.observation_fact
));

INSERT INTO temp_analysis(name,value) VALUES('fact_start_last', (
SELECT MAX(start_date) FROM i2b2crcdata.observation_fact
));


-- count facts in far future
INSERT INTO temp_analysis(name,value) VALUES('fact_far_future', (
SELECT COUNT(*) FROM i2b2crcdata.observation_fact
 WHERE EXTRACT(YEAR FROM start_date) > EXTRACT(YEAR FROM CURRENT_DATE) + 1
));
-- count facts in far past
INSERT INTO temp_analysis(name,value) VALUES('fact_far_past', (
SELECT COUNT(*) FROM i2b2crcdata.observation_fact
 WHERE EXTRACT(YEAR FROM start_date) < EXTRACT(YEAR FROM CURRENT_DATE) - 15
));


-- TODO count visits with birth-date > start-date
INSERT INTO temp_analysis(name,value) VALUES('adm_before_birth', (
SELECT COUNT(*) 
 FROM i2b2crcdata.visit_dimension v 
  INNER JOIN i2b2crcdata.patient_dimension p 
   ON p.patient_num=v.patient_num
  WHERE p.birth_date > v.start_date
));

-- timestamp
INSERT INTO temp_analysis(name,value) VALUES('current_timestamp', (
SELECT NOW()
));

-- Postgres software version
INSERT INTO temp_analysis(name,value) VALUES('pg_version', (
SELECT version()
));

