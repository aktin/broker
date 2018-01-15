INSERT INTO nodes(id, client_key, subject_dn, last_contact) VALUES(-1,'X','X',NOW());
INSERT INTO request_definitions(request_id,media_type,query_def) VALUES( -1,'X','X');
INSERT INTO request_node_status(request_id,node_id,message) VALUES( -1,-1,'X');

COMMIT;
CHECKPOINT;
DELETE FROM request_node_status WHERE request_id=-1;
DELETE FROM request_definitions WHERE request_id=-1;
DELETE FROM nodes WHERE id=-1;

COMMIT;
