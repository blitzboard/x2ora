package x2ora;

import io.javalin.http.Handler;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static x2ora.Main.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.pg.rdbms.pgql.PgqlResultSet;
import oracle.pg.rdbms.pgql.PgqlPreparedStatement;
import oracle.pgql.lang.PgqlException;
import oracle.pgql.lang.ResultSetMetaData;

public class RetrievalController {

  public static String countNodes() throws Exception {
    long timeStart = System.nanoTime();
    String result = "";
    try {
      PgqlPreparedStatement ps = pgqlConn.prepareStatement("SELECT COUNT(v) FROM MATCH (v) ON " + strPgv);
      PgqlResultSet rs = ps.executeQuery();
      if (rs.first()){
        result = "Test query succeeded.";
      }
      rs.close();
      ps.close();
    } catch (PgqlException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler list = ctx -> {
    long timeStart = System.nanoTime();
    LinkedList<String> response = new LinkedList<String>();
    try {
      String query = "SELECT id FROM " + strPgvGraph;
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        response.add(rs.getString("id"));
      }
      rs.close();
      ps.close();
    } catch (SQLException e) {
      response.add(printException(e));
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (list)");
  };

  public static Handler get = ctx -> {
    long timeStart = System.nanoTime();
    String strGraph = ctx.queryParam("graph");
    String strResponse = ctx.queryParam("response");
    String response = "";
    if (strResponse == null) {
      strResponse = "properties";
    }
    if (strResponse == "properties") {
      try {
        String query = "SELECT props FROM " + strPgvGraph + " WHERE id = '" + strGraph + "'";
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) { 
          response = rs.getString("props");
        } else {
          response = "Graph " + strGraph + " does not exist.";
        }
        rs.close();
        ps.close();
      } catch (SQLException e) {
        response = printException(e);
      }
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms ( get/ )");
  };

  public static Handler query = ctx -> {
    long timeStart = System.nanoTime();

    String strGraph = ctx.queryParam("graph");
    String strMatch = ctx.queryParam("query");
    System.out.println("INFO: A request is received: " + strMatch);

    // SELECT and WHERE
    String strSelect = "\nSELECT ";
    String strWhere = "\nWHERE ";
    int cntNode = 0; 
    Matcher matcherNode = Pattern.compile("\\((\\w+)\\)").matcher(strMatch);
    while (matcherNode.find()) {
      String v = matcherNode.group(1);
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      strWhere = strWhere + v + ".graph = '" + strGraph + "' AND ";
      cntNode++;
    }
    int cntEdge = 0; 
    Matcher matcherEdge = Pattern.compile("\\[(\\w+)\\]").matcher(strMatch);
    while (matcherEdge.find()) {
      String v = matcherEdge.group(1);
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".src AS " + v + "_src, " + v + ".dst AS " + v + "_dst, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      strWhere = strWhere + v + ".graph = '" + strGraph + "' AND ";
      cntEdge++;
    }
    strSelect = strSelect + "1 ";
    strWhere = strWhere + " 1 = 1";

    // Complete PGQL query
    strMatch = strSelect + "\nFROM " + strMatch + " ON " + strPgv + strWhere;
    System.out.println("INFO: Query is modified:" + strMatch);

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
    System.out.println("INFO: Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (query)");
  };

  public static Handler queryTable = ctx -> {
    long timeStart = System.nanoTime();

    String strQuery = ctx.queryParam("query");
    System.out.println("INFO: A request is received: " + strQuery);
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
      response.put("error", printException(e));
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (queryTable)");
    ctx.contentType("application/json");
    ctx.json(response);
  };

	private static PgGraph getResultPG(PgqlResultSet rs, int cntNode, int cntEdge) {
		PgGraph pg = new PgGraph();
		try {
			while (rs.next()) {

				int lengthNode = 3; // ID + Label + JSON Props
				int lengthEdge = 4; // ID + Src Node ID + Dst Node ID + Label + JSON Props

				int offsetEdge = cntNode * lengthNode; // Edge Offset
				int offsetNodeList = offsetEdge + (cntEdge * lengthEdge); // Node List Offset
        
				// Nodes
				for (int i = 1; i <= offsetEdge; i = i + lengthNode) {
					Object id = rs.getObject(i);
          if (!pg.hasNodeId(id)) {
            String label = rs.getString(i + 1);
            String props = rs.getString(i + 2);
            PgNode node = new PgNode(id, label, props);
            pg.addNode(node);
          }
				}
				// Edges
				for (int i = offsetEdge + 1; i <= offsetNodeList; i = i + lengthEdge) {
					String idSrc = rs.getString(i + 1);
          String idDst = rs.getString(i + 2);
          boolean undirected = false;
          String label = rs.getString(i + 3);
          String props = rs.getString(i + 4);
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