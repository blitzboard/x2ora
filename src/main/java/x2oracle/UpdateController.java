package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static x2oracle.Main.*;

import oracle.pg.rdbms.pgql.PgqlConnection;
import oracle.pg.rdbms.pgql.PgqlResultSet;
import oracle.pgql.lang.PgqlException;
import oracle.pg.rdbms.pgql.PgqlPreparedStatement;

import java.util.UUID;

public class UpdateController {

  public static Handler create = ctx -> {
    long timeStart = System.nanoTime();
    String result = "";

    PgGraphNamed pgn = ctx.bodyAsClass(PgGraphNamed.class);
    String strGraph = pgn.getName();
    PgGraph pg = pgn.getPg();

    if (exists(strGraph)) {
      result = "Graph " + strGraph + " exists.\n";
    } else {
      System.out.println("INFO: Graph received (" + pg.countNodes() + " nodes, " + pg.countEdges() + " edges).");
      String query = "";

      query = "INSERT INTO " + strPgvNode + " VALUES (?, ?, ?, ?)";
      PreparedStatement ps = conn.prepareStatement(query);
      for (PgNode node : pg.getNodes()) {
        try {
          ps.setString(1, strGraph);
          ps.setString(2, (String)node.getId());
          ps.setString(3, node.getLabel());
          ps.setString(4, node.getPropertiesJSON());
          ps.execute();
        } catch (Exception e) {
          conn.rollback();
          System.out.println("rollback");
          result = printException(e);
          throw e;
        };
      }
      ps.close();

      query = "INSERT INTO " + strPgvEdge + " VALUES (?, ?, ?, ?, ?, ?)";
      ps = conn.prepareStatement(query);
      for (PgEdge edge : pg.getEdges()) {
        try {
          ps.setString(1, strGraph);
          ps.setString(2, UUID.randomUUID().toString());
          ps.setString(3, (String)edge.getFrom());
          ps.setString(4, (String)edge.getTo());
          ps.setString(5, edge.getLabel());
          ps.setString(6, edge.getPropertiesJSON());
          ps.execute();
        } catch (Exception e) {
          conn.rollback();
          System.out.println("rollback");
          result = result + printException(e);
          throw e;
        };
      }
      ps.close();

      conn.commit();
      result = strGraph + " is created.\n";
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    ctx.result(result);
  };

  public static Handler drop = ctx -> {
    long timeStart = System.nanoTime();
    String strGraph = ctx.formParam("graph");
    String result = "";

    if (!exists(strGraph)) {
      result = strGraph + " does not exist.\n";
    } else {
      String query = "";

      query = "DELETE FROM " + strPgvEdge + " WHERE graph = ?";
      try (PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setString(1, strGraph);
        ps.execute();
        ps.close();
        result = result + "All nodes in " + strGraph + " is deleted.\n";
      } catch (Exception e) {
        conn.rollback();
        System.out.println("INFO: rollback");
        result = printException(e);
        throw e;
      };

      query = "DELETE FROM " + strPgvNode + " WHERE graph = ?";
      try (PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setString(1, strGraph);
        ps.execute();
        ps.close();
        result = result + "All edges in " + strGraph + " is deleted.\n";
      } catch (Exception e) {
        conn.rollback();
        System.out.println("INFO: rollback");
        result = printException(e);
        throw e;
      };

      conn.commit();
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    ctx.result(result);
  };

  private static Boolean exists(String strGraph) throws SQLException {
    Boolean result = false;
    try {
      String query = "SELECT graph FROM " + strPgvNode + " WHERE graph = '" + strGraph + "' FETCH FIRST 1 ROWS ONLY";
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      System.out.println(query);
      if (rs.next()) {
        result = true;
        System.out.println(result);
      }
      rs.close();
      ps.close();
    } catch (SQLException e) {
      System.out.println(printException(e));
    }
    return result;
  };

  public static Handler mergeNode = ctx -> {
    long timeStart = System.nanoTime();

    String strGraph = ctx.formParam("graph");
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

    String strGraph = ctx.formParam("graph");
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
    
    PgGraphNamed pgn = ctx.bodyAsClass(PgGraphNamed.class);
    String strGraph = pgn.getName();
    PgGraph pg = pgn.getPg();

    System.out.println("INFO: Graph received (" + pg.countNodes() + " nodes, " + pg.countEdges() + " edges).");

    for (PgNode node : pg.getNodes()) {
      String result = mergeNode(strGraph, (String)node.getId(), node.getLabel(), node.getPropertiesJSON());
      System.out.println(result);
    }

    for (PgEdge edge : pg.getEdges()) {
      String result = mergeEdge(strGraph, (String)edge.getFrom(), (String)edge.getTo(), edge.getLabel(), edge.getPropertiesJSON());
      System.out.println(result);
    }
    
    String result = "";
    conn.commit();
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    ctx.result(result + "\n");
  };

  private static String mergeNode(String strGraph, String strId, String strLabel, String strProps) throws SQLException {
    String result = "";
    try {
      Boolean able = true;

      // Check if the node exists
      if (able) {
        PgqlConnection pgqlConn = PgqlConnection.getConnection(conn);
        PgqlPreparedStatement pps = pgqlConn.prepareStatement("SELECT v.id FROM MATCH (v) ON " + strPgv + " WHERE v.graph = ? AND v.label = ? AND v.id = ?");
        pps.setString(1, strGraph);
        pps.setString(2, strLabel.toUpperCase());
        pps.setString(3, strId);
        pps.execute();
        PgqlResultSet rs = pps.getResultSet();
        if (rs.first()){
          able = false;
          result = "Node " + strLabel.toUpperCase() + " " + strId + " exists.";
        }
        rs.close();
        pps.close();
      }

      // Insert the node if not exists
      if (able) {
        String query = "INSERT INTO " + strPgvNode + " VALUES (?, ?, ?, ?)";			
        try (PreparedStatement ps = conn.prepareStatement(query)) {
          ps.setString(1, strGraph);
          ps.setString(2, strId);
          ps.setString(3, strLabel.toUpperCase());
          ps.setString(4, strProps);
          ps.execute();
          result = "Node " + strLabel.toUpperCase() + " " + strId + " is added.";  
          ps.close();
        } catch (Exception e) {
          conn.rollback();
          System.out.println("rollback");
          throw e;
        };
      } else {
        // The update query below gets PgqlToSqlException so this operation is skipped now
        /*
        String query = "UPDATE v SET (v.json = ?) FROM MATCH (v) ON " + strLabel + " WHERE LABEL(v) = ? AND v.id = ?";			
        System.out.println(query);
        ps = pgqlConn.prepareStatement(query);
        ps.setString(1, strProps);
        ps.setString(2, strLabel.toUpperCase());
        ps.setString(3, strId);
        ps.execute();
        result = "Node " + strLabel + " " + strId + " is update.";
        */
      }
      
    } catch (PgqlException e) {
      result = printException(e);
    }
    return result;
  };

  private static String mergeEdge(String strGraph, String strSrcId, String strDstId, String strLabel, String strProps) throws SQLException {
    String result = "";
    try {
      
      Boolean able = true;
      PgqlConnection pgqlConn = PgqlConnection.getConnection(conn);

      // Check if the node exists
      if (able) {
        PgqlPreparedStatement pps = pgqlConn.prepareStatement("SELECT v.id FROM MATCH (v) ON " + strPgv + " WHERE v.graph = ? AND v.id = ?");
        pps.setString(1, strGraph);
        pps.setString(2, strSrcId);
        pps.execute();
        PgqlResultSet rs = pps.getResultSet();
        if (!rs.first()){
          able = false;
          result = "Node " + strSrcId + " does not exist.";
        }
      }

      // Check if the node exists
      if (able) {
        PgqlPreparedStatement pps = pgqlConn.prepareStatement("SELECT v.id FROM MATCH (v) ON " + strPgv + " WHERE v.graph = ? AND v.id = ?");
        pps.setString(1, strGraph);
        pps.setString(2, strDstId);
        pps.execute();
        PgqlResultSet rs = pps.getResultSet();
        if (!rs.first()){
          able = false;
          result = "Node " + strDstId + " does not exist.";
        }
        rs.close();
        pps.close();
      }

      // Check if the edge exists
      if (able) {
        PgqlPreparedStatement pps = pgqlConn.prepareStatement("SELECT e.id FROM MATCH (src)-[e]->(dst) ON " + strPgv + " WHERE e.graph = ? AND e.label = ? AND src.id = ? AND dst.id = ?");
        pps.setString(1, strGraph);
        pps.setString(2, strLabel.toUpperCase());
        pps.setString(3, strSrcId);
        pps.setString(4, strDstId);
        pps.execute();
        PgqlResultSet rs = pps.getResultSet();
        if (rs.first()){
          able = false;
          result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " exists.";
        }
        rs.close();
        pps.close();
      }

      // Insert the edge if not exists
      if (able) {
        String query = "INSERT INTO x2pgv_edge VALUES (?, ?, ?, ?, ?, ?)";			
        try (PreparedStatement ps = conn.prepareStatement(query)) {
          ps.setString(1, strGraph);
          ps.setString(2, UUID.randomUUID().toString());
          ps.setString(3, strSrcId);
          ps.setString(4, strDstId);
          ps.setString(5, strLabel);
          ps.setString(6, strProps);
          ps.execute();
          result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " is added.";
          ps.close();
        } catch (Exception e) {
          conn.rollback();
          System.out.println("rollback");
          throw e;
        };
      }
      
    } catch (PgqlException e) {
      result = printException(e);
    }
    return result;
  };
}