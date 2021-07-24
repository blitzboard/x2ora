package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static x2oracle.Main.*;

public class UpdateController {

  public static Handler mergeNode = ctx -> {
    long timeStart = System.nanoTime();

    String strGraph = ctx.formParam("graph", strGraphPreset);
    String strId = ctx.formParam("id");
    String strLabel = ctx.formParam("label");
    String strProps = ctx.formParam("props");

    String result = mergeNode(strGraph, strId, strLabel, strProps);
    conn.commit();
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result + "\n");
  };

  public static Handler mergeEdge = ctx -> {
    long timeStart = System.nanoTime();

    String strGraph = ctx.formParam("graph", strGraphPreset);
    String strSrcId = ctx.formParam("src_id");
    String strDstId = ctx.formParam("dst_id");
    String strLabel = ctx.formParam("label");
    String strProps = ctx.formParam("props");

    String result = mergeEdge(strGraph, strSrcId, strDstId, strLabel, strProps);
    conn.commit();
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result + "\n");
  };

  public static Handler mergeGraph = ctx -> {
    long timeStart = System.nanoTime();
    
    PgGraph pg = ctx.bodyAsClass(PgGraph.class);
    System.out.println("INFO: Graph received (" + pg.countNodes() + " nodes, " + pg.countEdges() + " edges).");

    for (PgNode node : pg.getNodes()) {
      String result = mergeNode(strGraphPreset, (String)node.getId(), node.getLabel(), node.getPropertiesJSON());
      System.out.println(result);
    }

    for (PgEdge edge : pg.getEdges()) {
      String result = mergeEdge(strGraphPreset, (String)edge.getFrom(), (String)edge.getTo(), edge.getLabel(), edge.getPropertiesJSON());
      System.out.println(result);
    }
    
    String result = "";
    conn.commit();
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result + "\n");
  };

  private static String mergeNode(String strGraph, String strId, String strLabel, String strProps) {
    String result = "";
    try {
      Boolean able = true;
      PreparedStatement ps;
      ResultSet rs;

      // Check if the node exists
      if (able) {
        ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON " + strGraph + " WHERE LABEL(v) = ? AND v.id = ?");
        ps.setString(1, strLabel.toUpperCase());
        ps.setString(2, strId);
        ps.execute();
        rs = ps.getResultSet();
        if (rs.first()){
          able = false;
          result = "Node " + strLabel + " " + strId + " exists.";
        }
      }

      // Insert the node if not exists
      if (able) {
        String query = "INSERT INTO " + strGraph + " VERTEX v LABELS (" + strLabel + ") PROPERTIES (v.id = ?, v.json = ?)";			
        ps = conn.prepareStatement(query);
        ps.setString(1, strId);
        ps.setString(2, strProps);
        ps.execute();
        result = "Node " + strLabel + " " + strId + " is added.";
      } else {
        // The update query below gets PgqlToSqlException so this operation is skipped now
        /*
        String query = "UPDATE v SET (v.json = ?) FROM MATCH (v) ON " + strLabel + " WHERE LABEL(v) = ? AND v.id = ?";			
        System.out.println(query);
        ps = conn.prepareStatement(query);
        ps.setString(1, strProps);
        ps.setString(2, strLabel.toUpperCase());
        ps.setString(3, strId);
        ps.execute();
        result = "Node " + strLabel + " " + strId + " is update.";
        */
      }
      
    } catch (SQLException e) {
      result = printException(e);
    }
    return result;
  };

  private static String mergeEdge(String strGraph, String strSrcId, String strDstId, String strLabel, String strProps) {
    String result = "";
    try {
      
      Boolean able = true;
      PreparedStatement ps;
      ResultSet rs;

      // Check if the node exists
      if (able) {
        ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON " + strGraph + " WHERE v.id = ?");
        ps.setString(1, strSrcId);
        ps.execute();
        rs = ps.getResultSet();
        if (!rs.first()){
          able = false;
          result = "Node " + strSrcId + " does not exist.";
        }
      }

      // Check if the node exists
      if (able) {
        ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON " + strGraph + " WHERE v.id = ?");
        ps.setString(1, strDstId);
        ps.execute();
        rs = ps.getResultSet();
        if (!rs.first()){
          able = false;
          result = "Node " + strDstId + " does not exist.";
        }
      }

      // Check if the edge exists
      if (able) {
        ps = conn.prepareStatement("SELECT ID(e) FROM MATCH (src)-[e]->(dst) ON " + strGraph + " WHERE LABEL(e) = ? AND src.id = ? AND dst.id = ?");
        ps.setString(1, strLabel.toUpperCase());
        ps.setString(2, strSrcId);
        ps.setString(3, strDstId);
        ps.execute();
        rs = ps.getResultSet();
        if (rs.first()){
          able = false;
          result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " exists.";
        }
      }

      // Insert the edge if not exists
      if (able) {
        String query = "INSERT INTO " + strGraph + " EDGE e BETWEEN src AND dst LABELS (" + strLabel + ") PROPERTIES (e.json = ?)"
                     + "FROM MATCH (src) ON " + strGraph + ", MATCH (dst) ON " + strGraph + " WHERE src.id = ? AND dst.id = ?";			
        ps = conn.prepareStatement(query);
        ps.setString(1, strProps);
        ps.setString(2, strSrcId);
        ps.setString(3, strDstId);
        ps.execute();
        result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " is added.";
      }
      
    } catch (SQLException e) {
      result = printException(e);
    }
    return result;
  };
}