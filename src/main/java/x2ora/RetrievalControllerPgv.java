package x2ora;

import io.javalin.http.Handler;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static x2ora.Main.*;

import oracle.pg.rdbms.pgql.PgqlResultSet;
import oracle.pg.rdbms.pgql.PgqlPreparedStatement;
import oracle.pgql.lang.PgqlException;
import oracle.pgql.lang.ResultSetMetaData;

public class RetrievalControllerPgv {

  public static Handler query = ctx -> {
    long timeStart = System.nanoTime();

    String strGraph = ctx.queryParam("graph");
    String strMatch = ctx.queryParam("match");
    String strWhere = ctx.queryParam("where");
    logger.info("A request is received: " + strMatch);

    // SELECT and WHERE
    String strSelect = "\nSELECT ";
    if (strWhere == null || strWhere.equals("")) {
      strWhere = "1 = 1";
    }
    String strWhereGraph = "";
    int cntNode = 0; 
    Matcher matcherNode = Pattern.compile("\\((\\w+)\\)").matcher(strMatch);
    while (matcherNode.find()) {
      String v = matcherNode.group(1);
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      if (!(strGraph == null || strGraph.equals(""))) {
        strWhereGraph = strWhereGraph + v + ".graph = '" + strGraph + "' AND ";
      }
      cntNode++;
    }
    int cntEdge = 0; 
    Matcher matcherEdge = Pattern.compile("\\[(\\w+)\\]").matcher(strMatch);
    while (matcherEdge.find()) {
      String v = matcherEdge.group(1);
      strSelect = strSelect + v + ".src AS " + v + "_src, " + v + ".dst AS " + v + "_dst, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      if (!(strGraph == null || strGraph.equals(""))) {
        strWhereGraph = strWhereGraph + v + ".graph = '" + strGraph + "' AND ";
      }
      cntEdge++;
    }
    strSelect = strSelect + "1 ";

    // Complete PGQL query
    strMatch = strSelect + "\nFROM MATCH " + strMatch + "\nWHERE " + strWhereGraph + strWhere;
    logger.info("Query is modified:" + strMatch);

    // Run the PGQL query and get the result in PG-JSON
    HashMap<String, Object> response = new HashMap<>();
    PgGraph pg = new PgGraph();
    try {
      PgqlPreparedStatement ps = pgqlConn.prepareStatement(strMatch);
      ps.execute();
      PgqlResultSet rs = ps.getResultSet();
      pg = getResultPG(rs, cntNode, cntEdge);
      rs.close();
      ps.close();
      response.put("request", ctx.fullUrl());
      response.put("pg", pg);
    } catch (PgqlException e) {
      response.put("error", printException(e));
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (query)");
  };

  public static Handler queryTable = ctx -> {
    logger.info("/query_table");
    long timeStart = System.nanoTime();
    String strQuery = ctx.queryParam("query");
    logger.info(strQuery);
    HashMap<String, Object> response = new HashMap<>();

    // Run the PGQL query and get the result in table
    try {
      PgqlPreparedStatement ps = pgqlConn.prepareStatement(strQuery);
      ps.execute();
      PgqlResultSet rs = ps.getResultSet();
      ResultSetMetaData md = rs.getMetaData();
      LinkedHashMap<String, Object> table = new LinkedHashMap<>();
      LinkedList<String> columns = new LinkedList<>();
      LinkedList<HashMap<String, Object>> records = new LinkedList<>();
      int columnCount = md.getColumnCount();
      for (int i=1; i<=columnCount; i++) {
        columns.add(md.getColumnName(i));
      }
      while (rs.next()) {
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        for (int i=1; i<=columnCount; i++) {
          String columnName = md.getColumnName(i);
          record.put(columnName, rs.getObject(columnName));
        }
        records.add(record);
      }
      rs.close();
      ps.close();
      table.put("columns", columns);
      table.put("records", records);
      response.put("request", ctx.fullUrl());
      response.put("table", table);
    } catch (PgqlException e) {
      response.put("error", "PgqlException");
      logger.info("error", e);
    }
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    ctx.contentType("application/json");
    ctx.json(response);
  };

	private static PgGraph getResultPG(PgqlResultSet rs, int cntNode, int cntEdge) {
		PgGraph pg = new PgGraph();
		try {
			while (rs.next()) {

				int lengthNode = 3; // ID + Label + JSON Props
				int lengthEdge = 4; // Src Node ID + Dst Node ID + Label + JSON Props

				int offsetEdge = cntNode * lengthNode; // Edge Offset
				int offsetNodeList = offsetEdge + (cntEdge * lengthEdge); // Node List Offset
        
				// Nodes
				for (int i = 1; i <= offsetEdge; i = i + lengthNode) {
					Object id = rs.getObject(i + 0);
          if (!pg.hasNodeId(id)) {
            String label = rs.getString(i + 1);
            String props = rs.getString(i + 2);
            PgNode node = new PgNode(id, label, props);
            pg.addNode(node);
          }
				}
				// Edges
				for (int i = offsetEdge + 1; i <= offsetNodeList; i = i + lengthEdge) {
					String idSrc = rs.getString(i + 0);
          String idDst = rs.getString(i + 1);
          boolean undirected = false;
          String label = rs.getString(i + 2);
          String props = rs.getString(i + 3);
          PgEdge edge = new PgEdge(idSrc, idDst, undirected, label, props);
          pg.addEdge(edge);
				}
			}
		} catch (PgqlException e) {
			e.printStackTrace();
		}
		return pg;
	}
}