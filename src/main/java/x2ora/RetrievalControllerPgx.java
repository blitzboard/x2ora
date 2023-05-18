package x2ora;

import io.javalin.http.Handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import static x2ora.Main.*;

import oracle.pgx.api.GraphSource;
import oracle.pgx.api.PgqlResultSet;
import oracle.pgql.lang.PgqlException;
import oracle.pg.rdbms.GraphServer;

public class RetrievalControllerPgx {

  public static String countNodes() throws Exception {
    loadGraphIntoPgx();
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

  private static void loadGraphIntoPgx() {
    logger.info("loadGraphIntoPgx()");
    long timeStart = System.nanoTime();
    ResourceBundle rb = ResourceBundle.getBundle("common");
    String strPgxGraph = rb.getString("pgx_graph");
    try {
      pgxGraph = pgxSession.getGraph(strPgxGraph.toUpperCase());
      logger.info("The graph exists in memory. Attached the graph.");
    } catch (Exception e) {
      logger.info("The graph does not exist in memory");
      try {
        logger.info("Connect to PGX");
        pgxInstance = GraphServer.getInstance(
          rb.getString("base_url"),
          rb.getString("username"),
          rb.getString("password").toCharArray()
        );
        try {
          logger.info("Load the graph into PGX");
          pgxSession = pgxInstance.createSession("x2ora");
          pgxGraph = pgxSession.readGraphByName(strPgxGraph.toUpperCase(), GraphSource.PG_VIEW);
        } catch (ExecutionException | InterruptedException e2) {
          e2.printStackTrace();
          logger.error("Exception", e2);
        }
      } catch (IOException e1) {
        logger.error("Exception", e1);
      }
    }
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
  }

  public static Handler queryPath = ctx -> {
    logger.info("/query_path");
    long timeStart = System.nanoTime();

    String strGraph = ctx.queryParam("graph");
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
      if (!(strGraph == null || strGraph.equals(""))) {
        //strWhereGraph = strWhereGraph + v + ".graph = '" + strGraph + "' AND ";
      }
    }
    int cntEdge = 1; 
    String[] edges = {"path_edge"};
    for (String v : edges) {
      strSelect = strSelect + v + ".src AS " + v + "_src, " + v + ".dst AS " + v + "_dst, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      if (!(strGraph == null || strGraph.equals(""))) {
        //strWhereGraph = strWhereGraph + v + ".graph = '" + strGraph + "' AND ";
      }
    }
    strSelect = strSelect + "1 ";

    // Complete PGQL query
    strMatch = strSelect + "\nFROM MATCH " + strMatch + " ONE ROW PER STEP (path_src, path_edge, path_dst)\nWHERE " + strWhereGraph + strWhere;
    logger.info("Query is complemented:\n" + strMatch + "\n");

    //getGraph();

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
            //String props = rs.getString(i + 2);
            PgNode node = new PgNode(id, label, "{}");
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
          //String props = rs.getString(i + 3);
          PgEdge edge = new PgEdge(idSrc, idDst, undirected, label, "{}");
          pg.addEdge(edge);
				}
			}
		} catch (PgqlException e) {
			e.printStackTrace();
		}
		return pg;
	}

}