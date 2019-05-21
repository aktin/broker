CREATE TEMPORARY TABLE temp_analysis(name VARCHAR(255), value VARCHAR(255));
INSERT INTO temp_analysis(name,value) VALUES('version','0.3');
INSERT INTO temp_analysis(name,value) VALUES('pat_total', (
 SELECT COUNT(*) FROM i2b2crcdata.patient_dimension
));
INSERT INTO temp_analysis(name,value) VALUES('vis_total', (
 SELECT COUNT(*) FROM i2b2crcdata.visit_dimension
));
-- Anzahl Fälle mit zugeordneten Daten
INSERT INTO temp_analysis(name,value) VALUES('vis_w_facts', (
 SELECT COUNT(DISTINCT f.encounter_num) 
 FROM i2b2crcdata.observation_fact f 
  INNER JOIN i2b2crcdata.visit_dimension v 
  ON f.encounter_num=v.encounter_num
));
-- Anzahl Patienten mit zugeordneten Daten
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

-- Dublette: Patient mit bereits vergebenem Encounter
CREATE TEMPORARY TABLE dub AS (
SELECT DISTINCT sourcesystem_cd
	FROM i2b2crcdata.observation_fact ob
	WHERE sourcesystem_cd in (
		-- Komplementärmenge wählen
		SELECT sourcesystem_cd 
		FROM i2b2crcdata.observation_fact ob
		JOIN (
			-- die Goldstandard-Werte von visit_dimension holen
			SELECT encounter_num, patient_num
			FROM i2b2crcdata.visit_dimension
			WHERE encounter_num in (
				-- Table mit encounter-Dubletten
				SELECT encounter_num
				FROM i2b2crcdata.observation_fact
				WHERE concept_cd LIKE 'AKTIN:IPVI:%'  
				GROUP BY encounter_num
				HAVING count(encounter_num) > 1
			) 
		) as del
		ON ob.encounter_num = del.encounter_num AND ob.patient_num <> del.patient_num
	)
);
	
-- Anzahl der Dubletten
INSERT INTO temp_analysis(name,value) VALUES('fact_dupl_vis', (
SELECT COUNT(*) FROM dub
));

-- Importdatum der ersten Dublette
INSERT INTO temp_analysis(name,value) VALUES('fact_dupl_vis_import_first', (
SELECT MIN(import_date) FROM i2b2crcdata.observation_fact ob, dub d
WHERE ob.sourcesystem_cd IN(d.sourcesystem_cd)
));

-- Importdatum der letzten Dublette
INSERT INTO temp_analysis(name,value) VALUES('fact_dupl_vis_import_last', (
SELECT MAX(import_date) FROM i2b2crcdata.observation_fact ob, dub d
WHERE ob.sourcesystem_cd IN(d.sourcesystem_cd)
));

-- Patient ohne Visit
INSERT INTO temp_analysis(name,value) VALUES('pat_noVisit', (
SELECT COUNT(*) FROM i2b2crcdata.patient_dimension pd
WHERE pd.patient_num NOT IN (
	SELECT vd.patient_num
	FROM i2b2crcdata.visit_dimension vd
)
));

-- Visit ohne Patient
INSERT INTO temp_analysis(name,value) VALUES('visit_noPat', (
SELECT COUNT(*) FROM i2b2crcdata.visit_dimension vd
WHERE vd.patient_num NOT IN (
	SELECT pd.patient_num
	FROM i2b2crcdata.patient_dimension pd
)
));

-- Patient mit abweichender sourcesystem_cd in pat_dim (!= vis_dim.sourcesystem_cd)
INSERT INTO temp_analysis(name,value) VALUES('pat_srcsys_false', (
SELECT COUNT(DISTINCT pd.sourcesystem_cd) FROM i2b2crcdata.patient_dimension pd
WHERE pd.sourcesystem_cd NOT IN (
	SELECT vd.sourcesystem_cd 
	FROM i2b2crcdata.visit_dimension vd
)
));

-- Anzahl Datensätze mit mehr als einem 'AKTIN:IPVI:%'
INSERT INTO temp_analysis(name,value) VALUES('fact_ipvi>1', (
SELECT COUNT(count) FROM (
	SELECT ob.sourcesystem_cd, COUNT(ob.concept_cd) FROM i2b2crcdata.observation_fact ob
	WHERE ob.concept_cd LIKE 'AKTIN:IPVI:%'
	GROUP BY(ob.sourcesystem_cd)
) AS tmp
WHERE count > 1
));

-- Maximale Anzahl von 'AKTIN:IPVI:%' pro Datensatz
INSERT INTO temp_analysis(name,value) VALUES('fact_ipvi_max', (
SELECT MAX(count) FROM (
	SELECT ob.sourcesystem_cd, COUNT(ob.concept_cd) FROM i2b2crcdata.observation_fact ob
	WHERE ob.concept_cd LIKE 'AKTIN:IPVI:%'
	GROUP BY ob.sourcesystem_cd
	HAVING COUNT(ob.concept_cd)>1
) AS tmp
));

-- Minimale Anzahl von 'AKTIN:IPVI:%' pro Datensatz
INSERT INTO temp_analysis(name,value) VALUES('fact_ipvi_min', (
SELECT MIN(count) FROM (
	SELECT ob.sourcesystem_cd, COUNT(ob.concept_cd) FROM i2b2crcdata.observation_fact ob
	WHERE ob.concept_cd LIKE 'AKTIN:IPVI:%'
	GROUP BY ob.sourcesystem_cd
) AS tmp
));

-- Daten ohne 'AKTIN:ZIPCODE'
INSERT INTO temp_analysis(name,value) VALUES('fact_noZipcode', (
SELECT SUM (
	(SELECT COUNT(DISTINCT ob.sourcesystem_cd) FROM i2b2crcdata.observation_fact ob) -
	(SELECT COUNT(DISTINCT ob.sourcesystem_cd) FROM i2b2crcdata.observation_fact ob WHERE ob.concept_cd = 'AKTIN:ZIPCODE')
)
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

-- Datum und Zeitpunkt der ersten Aufnahme
INSERT INTO temp_analysis(name,value) VALUES('fact_start_first', (
SELECT MIN(start_date) FROM i2b2crcdata.observation_fact
));

-- Datum und Zeitpunkt der (aktuell) letzten Aufnahme
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

-- Anzahl Einträge im Consent-Manager
INSERT INTO temp_analysis(name,value) VALUES('cm_total', (
SELECT COUNT(*) FROM i2b2crcdata.optinout_patients
));

-- Anzahl Matches CM-Patient
INSERT INTO temp_analysis(name,value) VALUES('cm_matches_pat', (
SELECT COUNT(*) FROM i2b2crcdata.optinout_patients opt
JOIN i2b2crcdata.patient_mapping pat
ON opt.pat_psn = pat.patient_ide
WHERE opt.pat_ref='PAT'
));

-- Anzahl Matches CM-Encounter
INSERT INTO temp_analysis(name,value) VALUES('cm_matches_enc', (
SELECT COUNT(*) FROM i2b2crcdata.optinout_patients opt
JOIN i2b2crcdata.encounter_mapping enc
ON opt.pat_psn = enc.encounter_ide
WHERE opt.pat_ref='ENC'
));

-- Anzahl Matches CM-Fallnummer
INSERT INTO temp_analysis(name,value) VALUES('cm_matches_bil', (
SELECT COUNT(*) FROM i2b2crcdata.optinout_patients opt
JOIN (SELECT DISTINCT patient_num, tval_char FROM i2b2crcdata.observation_fact
WHERE concept_cd = 'AKTIN:Fallkennzeichen') pat
ON opt.pat_psn = pat.tval_char
WHERE opt.pat_ref='BIL'
));

-- Anzahl Matches (ignore pat_ref)
INSERT INTO temp_analysis(name,value) VALUES('cm_matches_any', (
SELECT COUNT(*) FROM i2b2crcdata.optinout_patients opt
WHERE opt.pat_psn IN (
	SELECT patient_ide FROM i2b2crcdata.patient_mapping
) OR opt.pat_psn IN (
	SELECT encounter_ide FROM i2b2crcdata.encounter_mapping
) OR opt.pat_psn IN (
	SELECT tval_char FROM (
		SELECT DISTINCT bil.patient_num, bil.tval_char FROM i2b2crcdata.observation_fact bil, i2b2crcdata.optinout_patients opt
		WHERE concept_cd = 'AKTIN:Fallkennzeichen' AND opt.pat_psn = bil.tval_char
	) AS tmp
)
));


-- timestamp
INSERT INTO temp_analysis(name,value) VALUES('current_timestamp', (
SELECT NOW()
));

-- Postgres software version
INSERT INTO temp_analysis(name,value) VALUES('pg_version', (
SELECT version()
));

DROP TABLE IF EXISTS dub;
