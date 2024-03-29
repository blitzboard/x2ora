package x2ora;

import static x2ora.Main.*;
import io.javalin.http.Handler;
import java.util.HashMap;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgql.lang.PgqlException;

public class RetrievalControllerPgx {

  public static String countNodes() throws Exception {
    long timeStart = System.nanoTime();
    String result = "";
    
    try {
      PgqlResultSet rs = pgxGraph.queryPgql("SELECT COUNT(v) FROM MATCH (v)");
      if (rs.first()){
        result = "Test query succeeded. PGX nodes: " + rs.getInteger(1).toString();
      }
    } catch (PgqlException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler queryPath = ctx -> {
    logger.info("/query_path");
    long timeStart = System.nanoTime();

    //String strGraph = ctx.queryParam("graph");
    String strMatch = ctx.queryParam("match");
    String strWhere = ctx.queryParam("where");
    logger.info("A request is received: " + strMatch);

    // SELECT and WHERE
    String strSelect = "\nSELECT DISTINCT ";
    if (strWhere == null || strWhere.equals("")) {
      strWhere = "1 = 1";
    }
    String strWhereGraph = "";
    int cntNode = 2; 
    String[] nodes = {"path_src", "path_dst"};
    for (String v : nodes) {
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
    }
    int cntEdge = 1; 
    String[] edges = {"path_edge"};
    for (String v : edges) {
      strSelect = strSelect + v + ".src AS " + v + "_src, " + v + ".dst AS " + v + "_dst, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
    }
    strSelect = strSelect + "1 ";

    // Complete PGQL query
    strMatch = strSelect + "\nFROM MATCH " + strMatch + " ONE ROW PER STEP (path_src, path_edge, path_dst)\nWHERE " + strWhereGraph + strWhere;
    logger.info("Query is complemented:\n" + strMatch + "\n");

    // Run the PGQL query and get the result in PG-JSON
    HashMap<String, Object> response = new HashMap<>();
    try {
      PgqlResultSet rs = pgxGraph.queryPgql(strMatch);
      PgGraph pg = getResultPG(rs, cntNode, cntEdge);
      rs.close();
      response.put("request", ctx.fullUrl());
      response.put("pg", pg);
    } catch (Exception e) {
      response.put("error", "Exception");
      logger.info("error", e);
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (query)");
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
            if (label.equals("")) { // PGX returns "" for null
              label = null;
            }
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
          if (label.equals("")) { // PGX returns "" for null
            label = null;
          }
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