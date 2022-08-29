package x2ora;

import io.javalin.http.Handler;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.PgqlResultSet;

import static x2ora.Main.*;

import java.util.HashMap;

public class RetrievalControllerPgx {

  public static String countNodes() throws Exception {
    long timeStart = System.nanoTime();
    String result = "";
    try {
      PgqlResultSet rs = pgxSession.queryPgql("SELECT COUNT(v) FROM MATCH (v) ON " + strPgv);
      if (rs.first()){
        result = "Test query succeeded.";
      }
    } catch (PgqlException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler nodeMatch = ctx -> {

    String strGraph = ctx.queryParam("graph");
    String strIds = ctx.queryParam("node_ids[]");
    String strLabels = ctx.queryParam("node_labels[]").toUpperCase();
    String strLimit = ctx.queryParam("limit");
    
    String strWhere = " WHERE 1 = 1";
    if (!strIds.isEmpty()) {
      strWhere = strWhere + " AND v.id = '" + strIds + "'"; // Should be replaced to IN () in 21.3 
    }
    if (!strLabels.isEmpty()) {
      strWhere = strWhere + " AND LABEL(v) = '" + strLabels + "'"; // Should be replaced to IN () in 21.3
    }
    String clauseLimit = " LIMIT " + strLimit;
    String strQuery = "SELECT v.id, LABEL(v), v.props FROM MATCH (v) ON " + strGraph + strWhere + clauseLimit;    
    System.out.println("INFO: A request is received: " + strQuery);

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();
    try {
      PgqlResultSet rs = pgxSession.queryPgql(strQuery);
      result = "Nodes with ID [" + strIds + "] are retrieved.";
      pg = getResultPG(rs, 1, 0);
    } catch (Exception e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result);
    ctx.contentType("application/json");
    HashMap<String, Object> response = new HashMap<>();
    response.put("request", ctx.fullUrl());
    response.put("pg", pg);
    ctx.json(response);
  };

	private static PgGraph getResultPG(PgqlResultSet rs, int countNode, int countEdge) {
		PgGraph pg = new PgGraph();
		try {
			while (rs.next()) {

				int lengthNode = 3; // ID + Label + JSON Props
				int lengthEdge = 4; // ID + Src Node ID + Dst Node ID + Label + JSON Props

				int offsetEdge = countNode * lengthNode; // Edge Offset
				int offsetNodeList = offsetEdge + (countEdge * lengthEdge); // Node List Offset
        
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