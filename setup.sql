-- v1 tables (using CLOB and VARCHAR2)

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
, CONSTRAINT x2node_fk_graph FOREIGN KEY (graph) REFERENCES x2graph(id)
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
, CONSTRAINT x2edge_fk_graph FOREIGN KEY (graph) REFERENCES x2graph(id)
, CONSTRAINT x2edge_fk_src FOREIGN KEY (graph, src) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_fk_dst FOREIGN KEY (graph, dst) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_check CHECK (props IS JSON)
);

-- v1 tables (using BLOB)

CREATE TABLE x2graph (
  id VARCHAR2(255)
, props BLOB
, CONSTRAINT x2graph_pk PRIMARY KEY (id)
, CONSTRAINT x2graph_check CHECK (props IS JSON FORMAT OSON)
);

CREATE TABLE x2node (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, label VARCHAR2(255)
, props BLOB
, CONSTRAINT x2node_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2node_fk_graph FOREIGN KEY (graph) REFERENCES x2graph(id)
, CONSTRAINT x2node_check CHECK (props IS JSON FORMAT OSON)
);

CREATE TABLE x2edge (
  graph VARCHAR2(255)
, id VARCHAR2(255)
, src VARCHAR2(255)
, dst VARCHAR2(255)
, label VARCHAR2(255)
, props BLOB
, CONSTRAINT x2edge_pk PRIMARY KEY (graph, id)
, CONSTRAINT x2edge_fk_graph FOREIGN KEY (graph) REFERENCES x2graph(id)
, CONSTRAINT x2edge_fk_src FOREIGN KEY (graph, src) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_fk_dst FOREIGN KEY (graph, dst) REFERENCES x2node(graph, id)
, CONSTRAINT x2edge_check CHECK (props IS JSON FORMAT OSON)
);

INSERT INTO x2node VALUES ('TEST', '1', 'PERSON', '{"AGE":[37]}');
INSERT INTO x2node VALUES ('TEST', '2', 'PERSON', '{"AGE":[36]}');
INSERT INTO x2edge VALUES ('TEST', '73da3dd7-2518-459a-9c36-c2104a95fc7f', '1', '2', 'KNOWS', '{"SINCE":[2017]}');

-- v1 pgv graph

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

-- v1 pgx graph

CREATE VIEW x2metanode AS
SELECT id, MAX(label) AS label, MAX(props) AS props FROM x2node GROUP BY id;

CREATE VIEW x2metaedge AS
SELECT MAX(id) AS id, src, dst, label, MAX(props) AS props FROM x2edge GROUP BY src, dst, label;

CREATE PROPERTY GRAPH x2meta
  VERTEX TABLES (
    x2metanode
      KEY (id)
      LABEL node
      PROPERTIES (id, label, props)
  )
  EDGE TABLES (
    x2metaedge
      KEY (id)
      SOURCE KEY(src) REFERENCES x2metanode
      DESTINATION KEY(dst) REFERENCES x2metanode
      LABEL edge
      PROPERTIES (id, label, src, dst, props)
  )
  OPTIONS (PG_VIEW);
