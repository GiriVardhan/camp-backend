BEGIN;
SET search_path  TO client_template,public;
INSERT INTO USERS VALUES('rod','a564de63c2d0da68cf47586ee05984d7',TRUE);
INSERT INTO USERS VALUES('dianne','65d15fe9156f9c4bbffd98085992a44e',TRUE);
INSERT INTO USERS VALUES('scott','2b58af6dddbd072ed27ffc86725d7d3a',TRUE);
INSERT INTO USERS VALUES('peter','22b5c9accc6e1ba628cedc63a72d57f8',FALSE);
INSERT INTO USERS VALUES('bill','2b58af6dddbd072ed27ffc86725d7d3a',TRUE);
INSERT INTO USERS VALUES('bob','2b58af6dddbd072ed27ffc86725d7d3a',TRUE);
INSERT INTO USERS VALUES('jane','2b58af6dddbd072ed27ffc86725d7d3a',TRUE);
INSERT INTO AUTHORITIES VALUES('rod','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('rod','ROLE_SUPERVISOR');
INSERT INTO AUTHORITIES VALUES('dianne','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('scott','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('peter','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('bill','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('bob','ROLE_USER');
INSERT INTO AUTHORITIES VALUES('jane','ROLE_USER');

INSERT INTO audit_action("action") VALUES ('Create');
INSERT INTO audit_action("action") VALUES ('Updated');
INSERT INTO audit_action("action") VALUES ('Deleted');
COMMIT;