package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static x2oracle.Main.*;

public class UpdateController {

  public static Handler mergeNode = ctx -> {

    String strLabel = ctx.queryParam("label");
    String strId = ctx.queryParam("id");

    long timeStart = System.nanoTime();
    String result = "";
    try {
      Boolean able = true;
      PreparedStatement ps;
      ResultSet rs;

      // Check if the node exists
      if (able) {
        ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON " + strGraph + " WHERE LABEL(v) = ? AND v.id = ?");
        ps.setString(1, strLabel);
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
        String query = "INSERT INTO " + strGraph + " VERTEX v LABELS (" + strLabel + ") PROPERTIES (v.id = ?)";			
        ps = conn.prepareStatement(query);
        ps.setString(1, strId);
        ps.execute();
        conn.commit();
        result = "Node " + strLabel + " " + strId + " is added.";
      }
      
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result);
  };

  public static Handler mergeEdge = ctx -> {

    String strLabel = ctx.queryParam("label");
    String strSrcId = ctx.queryParam("src_id");
    String strDstId = ctx.queryParam("dst_id");

    long timeStart = System.nanoTime();
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
        ps.setString(1, strLabel);
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
        String query = "INSERT INTO " + strGraph + " EDGE e BETWEEN src AND dst LABELS (" + strLabel + ") "
                     + "FROM MATCH (src) ON " + strGraph + ", MATCH (dst) ON " + strGraph + " WHERE src.id = ? AND dst.id = ?";			
        ps = conn.prepareStatement(query);
        ps.setString(1, strSrcId);
        ps.setString(2, strDstId);
        ps.execute();
        conn.commit();
        result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " is added.";
      }
      
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result);
  };

}