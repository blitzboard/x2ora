package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    String strId = ctx.queryParam("id");

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();;
    try {
      PreparedStatement ps;
      ResultSet rs;

      ps = conn.prepareStatement("SELECT v.id, LABEL(v), v.json FROM MATCH (v) ON " + strGraph + " WHERE v.id = ?");
      ps.setString(1, strId);
      ps.execute();
      rs = ps.getResultSet();
      result = "Node(s) with ID = " + strId + " are retrieved.";
      pg = getResultPG(rs, 1, 0);
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result);
    ctx.contentType("application/json");
    ctx.json(pg);
  };

  public static Handler edgeMatch = ctx -> {

    String strLabels = ctx.queryParam("labels").toLowerCase();

    long timeStart = System.nanoTime();
    String result = "";
    PgGraph pg = new PgGraph();
    try {
      PreparedStatement ps;
      ResultSet rs;

      ps = conn.prepareStatement("SELECT v1.id AS v1_id, LABEL(v1) AS v1_label, v1.json AS v1_json, v2.id AS v2_id, LABEL(v2) AS v2_label, v2.json AS v2_json, ID(e), v1.id AS src, v2.id AS dst, LABEL(e), e.json FROM MATCH (v1)-[e:" + strLabels + "]->(v2) ON " + strGraph);
      ps.execute();
      rs = ps.getResultSet();
      result = "Edge(s) with Label = " + strLabels + " are retrieved.";
      pg = getResultPG(rs, 2, 1);
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result);
    ctx.contentType("application/json");
    ctx.json(pg);
  };

	private static PgGraph getResultPG(ResultSet rs, int countNode, int countEdge) {
		PgGraph pg = new PgGraph();
		pg.setName("x2_response");
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

  /*
	private static void addNodeById(PgGraph pg, Object id, String label, String props) {
		//PgNode node = new PgNode(id);
		//node.addLabel(label);
    PgNode node = new PgNode(id, label, props);
		pg.addNode(node);
	}
	private static void addEdgeByIds(PgGraph pg, Object id, Object idSrc, Object idDst, String label) {
		PgEdge edge = new PgEdge(idSrc, idDst, false);
		edge.addLabel(label);
		pg.addEdge(id, edge);
	}
  */
}