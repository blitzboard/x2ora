package x2ora;

import io.javalin.http.Handler;

import java.util.LinkedList;
import java.util.HashMap;

import static x2ora.Main.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public class RetrievalController {

  public static String countNodes() throws Exception {
    long timeStart = System.nanoTime();
    String result = "";
    try {
      String query = "SELECT COUNT(*) FROM " + strPgvNode;
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        result = "Test query succeeded. X2 nodes: " + rs.getInt(1);
      }
      rs.close();
      ps.close();
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    logger.info("Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler list = ctx -> {
    logger.info("/list");
    long timeStart = System.nanoTime();
    LinkedList<HashMap<String, String>> response = new LinkedList<HashMap<String, String>>();
    try {
      String query = "SELECT id, JSON_VALUE(props, '$.name[0]') AS name" +
          " FROM " + strPgvGraph +
          " ORDER BY JSON_VALUE(props, '$.lastUpdate[0]') DESC;";
      logger.info(query);
      PreparedStatement ps = conn.prepareStatement(query);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("id", rs.getString("id"));
        item.put("name", rs.getString("name"));
        response.add(item);
      }
      rs.close();
      ps.close();
    } catch (SQLException e) {
      HashMap<String, String> item = new HashMap<String, String>();
      item.put("error", printException(e));
      response.add(item);
      logger.info("error", e);
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    logger.info("Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
  };

  public static Handler get = ctx -> {
    logger.info("/get");
    long timeStart = System.nanoTime();
    String strGraph = ctx.queryParam("graph");
    String strResponse = ctx.queryParam("response");
    HashMap<String, Object> response = new HashMap<>();
    if (strResponse == null || strResponse.equals("properties")) {
      response = getProperties(strGraph);
    } else if (strResponse.equals("pg")) {
      response = getPG(strGraph);
    }
    ctx.contentType("application/json");
    ctx.json(response);
    long timeEnd = System.nanoTime();
    logger.info("Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
  };

  private static HashMap<String, Object> getProperties(String strGraph) {
    HashMap<String, Object> response = new HashMap<>();
    try {
      String query = "SELECT props FROM " + strPgvGraph + " WHERE id = ?";
      logger.info(query + " [" + strGraph + "]");
      PreparedStatement ps = conn.prepareStatement(query);
      ps.setString(1, strGraph);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        response.put("id", strGraph);
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, List<Object>>> typeRef = new TypeReference<HashMap<String, List<Object>>>() {
        };
        HashMap<String, List<Object>> properties = mapper.readValue(rs.getString("props"), typeRef);
        response.put("properties", properties);
      } else {
        response.put("id", strGraph);
        response.put("error", "Graph " + strGraph + " does not exist.");
      }
      rs.close();
      ps.close();
    } catch (SQLException | JsonProcessingException e) {
      response.put("error", printException(e));
      logger.info("error", e);
    }
    return response;
  };

  private static HashMap<String, Object> getPG(String strGraph) {
    HashMap<String, Object> response = new HashMap<>();
    PgGraph pg = new PgGraph();
    try {
      // Nodes
      String query = "SELECT * FROM " + strPgvNode + " WHERE graph = ?";
      logger.info(query + " [" + strGraph + "]");
      PreparedStatement ps = conn.prepareStatement(query);
      ps.setString(1, strGraph);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        PgNode node = new PgNode(
            rs.getObject("id"),
            rs.getString("label"),
            rs.getString("props"));
        pg.addNode(node);
      }
      // Edges
      query = "SELECT * FROM " + strPgvEdge + " WHERE graph = ?";
      logger.info(query + " [" + strGraph + "]");
      ps = conn.prepareStatement(query);
      ps.setString(1, strGraph);
      rs = ps.executeQuery();
      while (rs.next()) {
        PgEdge edge = new PgEdge(
            rs.getObject("src"),
            rs.getObject("dst"),
            false, // undirected
            rs.getString("label"),
            rs.getString("props"));
        pg.addEdge(edge);
      }
      rs.close();
      ps.close();
      response.put("id", strGraph);
      response.put("pg", pg);
    } catch (SQLException e) {
      response.put("error", printException(e));
      logger.info("error", e);
    }
    return response;
  };
}
