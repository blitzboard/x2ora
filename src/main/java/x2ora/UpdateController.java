package x2ora;

import io.javalin.http.Handler;

import static x2ora.Main.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class UpdateController {

  public static Handler update = ctx -> {
    logger.info("/update");
    long timeStart = System.nanoTime();
    PgGraphNamed pgn = ctx.bodyAsClass(PgGraphNamed.class);
    String strGraphId = pgn.getId();
    String strGraphProps = pgn.getPropertiesJSON();
    PgGraph pg = pgn.getPg();
    logger.info("Graph received [" + pg.countNodes() + " nodes, " + pg.countEdges() + " edges]");

    HashMap<String, String> response = new HashMap<>();
    if (exists(strGraphId)) {
      drop(strGraphId);
      create(pg, strGraphId, strGraphProps);
      response.put("status", "success");
    } else {
      response.put("status", "not exist");
    }

    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    response.put("request", "update");
    response.put("graphId", strGraphId);
    ctx.json(response);
  };

  public static Handler create = ctx -> {
    logger.info("/create");
    long timeStart = System.nanoTime();
    PgGraphNamed pgn = ctx.bodyAsClass(PgGraphNamed.class);
    String strGraphId = UUID.randomUUID().toString();
    String strGraphProps = pgn.getPropertiesJSON();
    PgGraph pg = pgn.getPg();
    logger.info("Graph received [" + pg.countNodes() + " nodes, " + pg.countEdges() + " edges]");

    HashMap<String, String> response = new HashMap<>();
    if (exists(strGraphId)) {
      response.put("status", "already exists");
    } else {
      create(pg, strGraphId, strGraphProps);
      response.put("status", "success");
    }

    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    response.put("request", "create");
    response.put("graphId", strGraphId);
    ctx.json(response);
  };

  public static Handler drop = ctx -> {
    logger.info("/drop");
    long timeStart = System.nanoTime();
    String strGraphId = ctx.formParam("graph");

    HashMap<String, String> response = new HashMap<>();
    if (exists(strGraphId)) {
      drop(strGraphId);
      response.put("status", "success");
    } else {
      response.put("status", "not exist");
    }
    
    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    response.put("request", "drop");
    response.put("graphId", strGraphId);
    ctx.json(response);
  };
  
  public static Handler rename = ctx -> {
    logger.info("/rename");
    long timeStart = System.nanoTime();
    String strId = ctx.formParam("graph");
    String strName = ctx.formParam("name");

    HashMap<String, String> response = new HashMap<>();
    if (exists(strId)) {
      rename(strId, strName);
      response.put("status", "success");
    } else {
      response.put("status", "not exist");
    }

    long timeEnd = System.nanoTime();
    logger.info("Execution time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
    response.put("request", "rename");
    response.put("graphId", strId);
    ctx.json(response);
  };

  private static String create(PgGraph pg, String strGraphId, String strGraphProps) throws Exception {
    String result = "";
    String query = "";
    PreparedStatement ps;

    query = "INSERT INTO " + strPgvGraph + " VALUES (?, ?)";
    ps = conn.prepareStatement(query);
    try {
      ps.setString(1, strGraphId);
      ps.setString(2, strGraphProps);
      logger.info(query + " [" + strGraphId + ", <strGraphProps>]");
      ps.execute();
    } catch (Exception e) {
      conn.rollback();
      logger.info("rollback");
      result = printException(e);
      throw e;
    };
    ps.close();

    query = "INSERT INTO " + strPgvNode + " VALUES (?, ?, ?, ?)";
    ps = conn.prepareStatement(query);
    logger.info(query + " [for " + pg.getNodes().size() + " nodes]");
    for (PgNode node : pg.getNodes()) {
      try {
        ps.setString(1, strGraphId);
        ps.setString(2, (String)node.getId());
        ps.setString(3, node.getLabel());
        ps.setString(4, node.getPropertiesJSON());
        ps.execute();
      } catch (Exception e) {
        conn.rollback();
        logger.info("rollback");
        result = printException(e);
        throw e;
      };
    }
    ps.close();

    query = "INSERT INTO " + strPgvEdge + " VALUES (?, ?, ?, ?, ?, ?)";
    ps = conn.prepareStatement(query);
    logger.info(query + " [for " + pg.getEdges().size() + " edges]");
    for (PgEdge edge : pg.getEdges()) {
      try {
        ps.setString(1, strGraphId);
        ps.setString(2, UUID.randomUUID().toString());
        ps.setString(3, (String)edge.getFrom());
        ps.setString(4, (String)edge.getTo());
        ps.setString(5, edge.getLabel());
        ps.setString(6, edge.getPropertiesJSON());
        ps.execute();
      } catch (Exception e) {
        conn.rollback();
        logger.info("rollback");
        result = result + printException(e);
        throw e;
      };
    }
    ps.close();

    conn.commit();
    result = strGraphId + " is created.\n";
    return result;
  };

  private static String drop(String strGraphId) throws Exception {
    String result = "";
    String query = "";

    query = "DELETE FROM " + strPgvEdge + " WHERE graph = ?";
    logger.info(query + " [" + strGraphId + "]");
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, strGraphId);
      ps.execute();
      ps.close();
      result = result + "All edges in " + strGraphId + " is deleted.\n";
    } catch (Exception e) {
      conn.rollback();
      logger.info("rollback");
      result = printException(e);
      throw e;
    };

    query = "DELETE FROM " + strPgvNode + " WHERE graph = ?";
    logger.info(query + " [" + strGraphId + "]");
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, strGraphId);
      ps.execute();
      ps.close();
      result = result + "All nodes in " + strGraphId + " is deleted.\n";
    } catch (Exception e) {
      conn.rollback();
      logger.info("rollback");
      result = printException(e);
      throw e;
    };

    query = "DELETE FROM " + strPgvGraph + " WHERE graph = ?";
    logger.info(query + " [" + strGraphId + "]");
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, strGraphId);
      ps.execute();
      ps.close();
      result = result + "Graph " + strGraphId + " is deleted.\n";
    } catch (Exception e) {
      conn.rollback();
      logger.info("rollback");
      result = printException(e);
      throw e;
    };

    conn.commit();
    return result;
  };

  private static String rename(String strId, String strName) throws Exception {
    String result = "";
    String query = "";

    query = "UPDATE " + strPgvGraph + " SET props = JSON_TRANSFORM(props, SET '$.name[0]' = ?) WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, strName);
      ps.setString(2, strId);
      logger.info("" + query + " (" + strName + ", " + strId + ")");
      ps.execute();
      ps.close();
      result = result + "Graph " + strId + " is renamed to " + strName + ".\n";
    } catch (Exception e) {
      conn.rollback();
      logger.info("rollback");
      result = printException(e);
      throw e;
    };

    conn.commit();
    return result;
  };

  private static Boolean exists(String strGraphId) throws SQLException {
    Boolean result = false;
    try {
      String query = "SELECT id FROM " + strPgvGraph + " WHERE id = '" + strGraphId + "' FETCH FIRST 1 ROWS ONLY";
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      logger.info("" + query);
      if (rs.next()) {
        result = true;
        logger.info("Graph exists [" + strGraphId + "]");
      } else {
        logger.info("Graph does not exist [" + strGraphId + "]");
      }
      rs.close();
      ps.close();
    } catch (SQLException e) {
      logger.info(printException(e));
    }
    return result;
  };
}