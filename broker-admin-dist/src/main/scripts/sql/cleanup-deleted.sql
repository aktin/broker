-- remove requests without definitions
DELETE FROM requests WHERE id NOT IN (SELECT request_id FROM request_definitions);

-- remove results and status information for removed requests
DELETE FROM request_node_results WHERE request_id NOT IN (SELECT request_id FROM request_definitions);
DELETE FROM request_node_status WHERE request_id NOT IN (SELECT request_id FROM request_definitions);

-- compact database
COMMIT;
CHECKPOINT DEFRAG
