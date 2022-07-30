
CREATE TABLE x2pgv_node (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2pgv_node_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2pgv_node_check CHECK (props IS JSON)
);

CREATE TABLE x2pgv_edge (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, src VARCHAR2(255)
, dst VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2pgv_edge_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2pgv_edge_fk_src FOREIGN KEY (graph, src) REFERENCES graph1_node(graph, id)
, CONSTRAINT x2pgv_edge_fk_dst FOREIGN KEY (graph, dst) REFERENCES graph1_node(graph, id)
, CONSTRAINT x2pgv_edge_check CHECK (props IS JSON)
);

INSERT INTO x2pgv_node VALUES ('TEST', '1', 'PERSON', '{"AGE":[37]}');
INSERT INTO x2pgv_node VALUES ('TEST', '2', 'PERSON', '{"AGE":[36]}');
INSERT INTO x2pgv_edge VALUES ('TEST', '73da3dd7-2518-459a-9c36-c2104a95fc7f', '1', '2', 'KNOWS', '{"SINCE":[2017]}');

-- FYI
CREATE VIEW graph1_node_v AS
SELECT
  id
, CASE label WHEN 'VEHICLE' THEN JSON_VALUE(props, '$.VEHICLE_NAME[0]') ELSE JSON_VALUE(props, '$.NAME[0]') END AS name
, label
, props
FROM graph1_node;

PGQL AUTO ON

CREATE PROPERTY GRAPH x2pgv
  VERTEX TABLES (
    x2pgv_node
      KEY (id)
      LABEL node
      PROPERTIES (id, label, props)
  )
  EDGE TABLES (
    x2pgv_edge
      KEY (id)
      SOURCE KEY(src) REFERENCES x2pgv_node
      DESTINATION KEY(dst) REFERENCES x2pgv_node
      LABEL edge
      PROPERTIES (id, label, props)
  )
  OPTIONS (PG_VIEW);









