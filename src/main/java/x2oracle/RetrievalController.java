package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import static x2oracle.Main.*;

import oracle.pg.rdbms.pgql.PgqlConnection;
import oracle.pg.rdbms.pgql.PgqlResultSet;
import oracle.pg.rdbms.pgql.PgqlPreparedStatement;
import oracle.pgql.lang.PgqlException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RetrievalController {

  public static String countNodes() throws Exception {
    long timeStart = System.nanoTime();
    String result = "";
    try {
      PgqlConnection pgqlConn = PgqlConnection.getConnection(conn);
      PgqlPreparedStatement ps = pgqlConn.prepareStatement("SELECT COUNT(v) FROM MATCH (v) ON " + strGraphPreset);
      PgqlResultSet rs = ps.executeQuery();
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

  public static Handler query = ctx -> {

    String strGraph = ctx.queryParam("graph", strGraphPreset);
    String strQuery = ctx.queryParam("query", "");
    
    strQuery = strQuery + "ON " + strGraph;
    System.out.println("INFO: A request is received: " + strQuery);

    // Count numbers of nodes and edges
    String strSelect = "SELECT ";
    int cntNode = 0; 
    Matcher matcherNode = Pattern.compile("\\((\\w+)\\)").matcher(strQuery);
    while (matcherNode.find()) {
      System.out.println("\n\n\nNode: " + matcherNode.group(1) + "\n\n\n");
      String v = matcherNode.group(1);
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      cntNode++;
    }
    int cntEdge = 0; 
    Matcher matcherEdge = Pattern.compile("\\[(\\w+)\\]").matcher(strQuery);
    while (matcherEdge.find()) {
      System.out.println("\n\n\nEdge: " + matcherEdge.group(1) + "\n\n\n");
      String v = matcherEdge.group(1);
      strSelect = strSelect + v + ".id AS " + v + "_id, " + v + ".src AS " + v + "_src, " + v + ".dst AS " + v + "_dst, " + v + ".label AS " + v + "_label, " + v + ".props AS " + v + "_props, ";
      cntEdge++;
    }
    strSelect = strSelect + "1 ";    

    strQuery = strSelect + "FROM " + strQuery;
    System.out.println("INFO: Query is modified: " + strQuery);

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();
    try {
      PgqlConnection pgqlConn = PgqlConnection.getConnection(conn);
      PgqlPreparedStatement ps = pgqlConn.prepareStatement(strQuery);
      ps.execute();
      PgqlResultSet rs = ps.getResultSet();
      result = "Query result is retrieved.";
      pg = getResultPG(rs, cntNode, cntEdge);
    } catch (PgqlException e) {
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