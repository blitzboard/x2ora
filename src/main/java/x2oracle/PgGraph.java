package x2oracle;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// members are public for jackson to access and export as JSON

class PgGraph {
  public String name;
  public Map<String, PgNode> nodes = new HashMap<>();
  //public List<PgEdge> edges = new ArrayList<PgEdge>();
  public Map<String, PgEdge> edges = new HashMap<>();
  public void setName(String name) {
    this.name = name;
  }
  public void addNode(String id, PgNode node) {
    nodes.put(id, node);
  }
  public PgNode getNode(String id) {
    return nodes.get(id);
  }
  public void addEdge(String id, PgEdge edge) {
    edges.put(id, edge);
  }
  public PgEdge getEdge(String id) {
    return edges.get(id);
  }
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
}

class PgNode {
  public String id;
  public Set<String> labels = new HashSet<String>();
  public Map<String, String> properties = new HashMap<>();
  public PgNode(String id) {
    this.id = id;
  }
  public void addLabel(String label) {
    labels.add(label);
  }
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }
}

class PgEdge {
  public String id1;
  public String id2;
  public boolean undirected = false;
  public Set<String> labels = new HashSet<String>();
  public Map<String, String> properties = new HashMap<>();
  public PgEdge(String id1, String id2, boolean undirected) {
    this.id1 = id1;
    this.id2 = id2;
    this.undirected = undirected;
  }
  public void addLabel(String label) {
    labels.add(label);
  }
  public void addProperty(String key, String value) {
    properties.put(key, value);
  }
}