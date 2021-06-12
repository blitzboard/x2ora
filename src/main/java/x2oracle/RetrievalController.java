package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import static x2oracle.Main.*;

public class RetrievalController {

  public static String countNodes() {

    long timeStart = System.nanoTime();
    String result = "";
    try {
      PreparedStatement ps;
      ResultSet rs;

      // Check if the node exists
      ps = conn.prepareStatement("SELECT COUNT(v) FROM MATCH (v) ON " + strGraph);
      ps.execute();
      rs = ps.getResultSet();
      if (rs.first()){
        result = "Test query succeeded.";
      }
    
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler nodeMatch = ctx -> {

    String strIds = ctx.queryParam("node_ids[]", "");
    String strLabels = ctx.queryParam("node_labels[]", "").toUpperCase();
    String strLimit = ctx.queryParam("limit", "1000");
    
    String strWhere = " WHERE 1 = 1";
    if (!strIds.isEmpty()) {
      strWhere = strWhere + " AND v.id = '" + strIds + "'"; // Should be replaced to IN () in 21.3 
    }
    if (!strLabels.isEmpty()) {
      strWhere = strWhere + " AND LABEL(v) = '" + strLabels + "'"; // Should be replaced to IN () in 21.3
    }
    String clauseLimit = " LIMIT " + strLimit;
    String strQuery = "SELECT v.id, LABEL(v), v.json FROM MATCH (v) ON " + strGraph + strWhere + clauseLimit;    
    System.out.println("INFO: A request is received: " + strQuery);

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();
    try {
      PreparedStatement ps = conn.prepareStatement(strQuery);
      ps.execute();
      ResultSet rs = ps.getResultSet();
      result = "Nodes with ID [" + strIds + "] are retrieved.";
      pg = getResultPG(rs, 1, 0);
    } catch (SQLException e) {
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

  public static Handler edgeMatch = ctx -> {

    String strLabels = ctx.queryParam("edge_labels[]", "").toUpperCase();
    String strLimit = ctx.queryParam("limit", "1000");

    String strWhere = " WHERE 1 = 1";
    if (!strLabels.isEmpty()) {
      strWhere = strWhere + " AND LABEL(e) = '" + strLabels + "'"; // Should be replaced to IN () in 21.3
    }
    String clauseLimit = " LIMIT " + strLimit;
    String strQuery = "SELECT v1.id AS v1_id, LABEL(v1) AS v1_label, v1.json AS v1_json, v2.id AS v2_id, LABEL(v2) AS v2_label, v2.json AS v2_json, ID(e), v1.id AS src, v2.id AS dst, LABEL(e), e.json FROM MATCH (v1)-[e]->(v2) ON " + strGraph + strWhere + clauseLimit;
    System.out.println("INFO: A request is received: " + strQuery);

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();
    try {
      PreparedStatement ps = conn.prepareStatement(strQuery);
      ps.execute();
      ResultSet rs = ps.getResultSet();
      result = "Edge(s) with Label = " + strLabels + " are retrieved.";
      pg = getResultPG(rs, 2, 1);
    } catch (SQLException e) {
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

	private static PgGraph getResultPG(ResultSet rs, int countNode, int countEdge) {
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
					String label = rs.getString(i + 1);
          String props = rs.getString(i + 2);
          PgNode node = new PgNode(id, label, props);
		      pg.addNode(node);
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
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pg;
	}
}