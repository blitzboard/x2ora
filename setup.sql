
CREATE TABLE x2pgv_node (
  id VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2pgv_node_pk PRIMARY KEY (id)
, CONSTRAINT x2pgv_node_check CHECK (props IS JSON)
);

CREATE TABLE x2pgv_edge (
  id VARCHAR2(255)
, src VARCHAR2(255)
, dst VARCHAR2(255)
, label VARCHAR2(255)
, props VARCHAR2(4000)
, CONSTRAINT x2pgv_edge_pk PRIMARY KEY (id)
, CONSTRAINT x2pgv_edge_fk_src FOREIGN KEY (src) REFERENCES graph1_node(id)
, CONSTRAINT x2pgv_edge_fk_dst FOREIGN KEY (dst) REFERENCES graph1_node(id)
, CONSTRAINT x2pgv_edge_check CHECK (props IS JSON)
);

INSERT INTO x2pgv_node VALUES ('1', 'VEHICLE', '{"ID":["1"], "JC08":["33.8"], "FRONT_TREAD":["1465"], "FUEL_TANK_CAPACITY":["36"], "GRADE":["X-URBAN"], "HEIGHT":["1490"], "LENGTH":["4030"], "MINIMUM_GROUND_CLEARANCE":["160"], "MINIMUM_TURNING_RADIUS":["5.4"], "NAME":["DAA-NHP10-AHXXB"], "NUM_OF_BATTERY":["20"], "REAR_TREAD":["1460"], "RIDING_CAPACITY":["5"], "ROOM_HEIGHT":["1175"], "ROOM_LENGTH":["2015"], "ROOM_WIDTH":["1395"], "TOTAL_WEIGHT":["1365"], "VEHICLE_ENG_NAME":["AQUA"], "VEHICLE_NAME":["アクア"], "VEHICLE_WEIGHT":["1090"], "VERSION":["1"], "WHEEL_BASE":["2550"], "WIDTH":["1695"]}');
INSERT INTO x2pgv_node VALUES ('201', 'ENGINE', '{"ID":["201"], "COMPRESSION_RATIO":["13.4"], "ENGINE_FAMILY":["1NZ"], "ENGINE_TYPE":["直列4気筒DOHC"], "FUEL":["無鉛レギュラーガソリン"], "FUEL_SUPPLY_DEVICE":["電子制御式燃料噴射装置（EFI）"], "INNER_DIAMETER":["75"], "MAXIMUM_OUTPUT":["54_4,800"], "MAXIMUM_OUTPUT_NET":["74_4,800"], "MAXIMUM_TORQUE":["111_3,600-4,400"], "MAXIMUM_TORQUE_NET":["11.3_3,600-4,400"], "NAME":["1NZ-FXE"], "STROKE":["84.7"], "TOTAL_DISPLACEMENT":["1.496"], "TYPE":["parts"], "VERSION":["1"]}');
INSERT INTO x2pgv_edge VALUES ('73da3dd7-2518-459a-9c36-c2104a95fc7f', '1', '201', 'HASENGINE', '{"FROM":["1"], "TO":["201"], "VERSION":["1"]}');

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









