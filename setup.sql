-- SQL

CREATE TABLE x2graph (
  id VARCHAR2(255)
, props CLOB
, CONSTRAINT x2graph_pk PRIMARY KEY (id)
, CONSTRAINT x2graph_check CHECK (props IS JSON)
);

CREATE TABLE x2node (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2node_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2node_check CHECK (props IS JSON)
);

CREATE TABLE x2edge (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, src VARCHAR2(255)
, dst VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2edge_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2edge_fk_src FOREIGN KEY (graph, src) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_fk_dst FOREIGN KEY (graph, dst) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_check CHECK (props IS JSON)
);

INSERT INTO x2node VALUES ('TEST', '1', 'PERSON', '{"AGE":[37]}');
INSERT INTO x2node VALUES ('TEST', '2', 'PERSON', '{"AGE":[36]}');
INSERT INTO x2edge VALUES ('TEST', '73da3dd7-2518-459a-9c36-c2104a95fc7f', '1', '2', 'KNOWS', '{"SINCE":[2017]}');

-- PGQL

DROP PROPERTY GRAPH x2;

CREATE PROPERTY GRAPH x2
  VERTEX TABLES (
    x2node
      KEY (id)
      LABEL node
      PROPERTIES (graph, id, label, props)
  )
  EDGE TABLES (
    x2edge
      KEY (id)
      SOURCE KEY(src) REFERENCES x2node
      DESTINATION KEY(dst) REFERENCES x2node
      LABEL edge
      PROPERTIES (graph, id, label, src, dst, props)
  )
  OPTIONS (PG_VIEW);
