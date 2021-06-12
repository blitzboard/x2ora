package x2oracle;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// members are public for jackson to access and export as JSON

class PgResponse {
  public String request;
  public PgGraph pg;
  public void setRequest(String request) {
    this.request = request;
  }
  public void setPg(PgGraph pg) {
    this.pg = pg;
  }
}

class PgRequest {
  public String option;
  public PgGraph pg;
  public void setOption(String option) {
    this.option = option;
  }
  public void setPg(PgGraph pg) {
    this.pg = pg;
  }
}

public class PgGraph {
  private HashSet<PgNode> nodes = new HashSet<PgNode>();
  private HashSet<PgEdge> edges = new HashSet<PgEdge>();

  public PgGraph() {
  }
  public PgGraph(HashSet<PgNode> nodes, HashSet<PgEdge> edges) {
    this.nodes = nodes;
    this.edges = edges;
  }
  
  public HashSet<PgNode> getNodes() {
    return nodes;
  }
  public HashSet<PgEdge> getEdges() {
    return edges;
  }
  public void setNodes(HashSet<PgNode> nodes) {
      this.nodes = nodes;
  }
  public void setEdges(HashSet<PgEdge> edges) {
    this.edges = edges;
  }
  
  public Integer countNodes() {
    System.out.println(this.nodes.size());
    return this.nodes.size();
  }
  public void addNode(PgNode node) {
    nodes.add(node);
  }
  public void addEdge(PgEdge edge) {
    edges.add(edge);
  }
  /*
  public String exportJSON() {
    ObjectMapper mapper = new ObjectMapper();
    String json = "";
    try {
      json = mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    return json;
  }
  */
}

class PgNode {
  public Object id;
  public HashSet<String> labels = new HashSet<String>();
  public HashMap<String, List<Object>> properties = new HashMap<>();
  /*
  public PgNode(Object id) {
    this.id = id;
  }
  */
  public PgNode(Object id, String label, String props) {
    this.id = id;
    this.addLabel(label);
    this.addProperties(props);
  }
  public void addLabel(String label) {
    labels.add(label.toLowerCase());
  }
  /*
  public void addProperty(String key, Object value) {
    properties.put(key, value);
  }
  */
  public void addProperties(String json) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, List<Object>>> typeRef = new TypeReference<HashMap<String, List<Object>>>(){};
    try {
      properties = mapper.readValue(json, typeRef);
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}

class PgEdge {
  public Object from;
  public Object to;
  public boolean undirected = false;
  public HashSet<String> labels = new HashSet<String>();
  public HashMap<String, List<Object>> properties = new HashMap<>();
  /*
  public PgEdge(Object from, Object to, boolean undirected) {
    this.from = from;
    this.to = to;
    this.undirected = undirected;
  }
  */
  public PgEdge(Object from, Object to, boolean undirected, String label, String props) {
    this.from = from;
    this.to = to;
    this.undirected = undirected;
    this.addLabel(label);
    this.addProperties(props);
  }
  public void addLabel(String label) {
    labels.add(label.toLowerCase());
  }
  /*
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }
  */
  public void addProperties(String json) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, List<Object>>> typeRef = new TypeReference<HashMap<String, List<Object>>>(){};
    try {
      properties = mapper.readValue(json, typeRef);
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}