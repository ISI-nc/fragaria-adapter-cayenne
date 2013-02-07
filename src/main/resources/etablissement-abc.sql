CREATE OR REPLACE VIEW abc AS 
 SELECT etablissement.id,etablissement.name
   FROM etablissement;

ALTER TABLE abc
  OWNER TO dev;