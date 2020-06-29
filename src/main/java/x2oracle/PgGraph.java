package x2oracle;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// members are public for jackson to access and export as JSON

class PgGraph {
  public String name;
  public Map<Object, PgNode> nodes = new HashMap<>();
  //public List<PgEdge> edges = new ArrayList<PgEdge>();
  public Map<Object, PgEdge> edges = new HashMap<>();
  public void setName(String name) {
    this.name = name;
  }
  public void addNode(Object id, PgNode node) {
    nodes.put(id, node);
  }
  public PgNode getNode(Object id) {
    return nodes.get(id);
  }
  public void addEdge(Object id, PgEdge edge) {
    edges.put(id, edge);
  }
  public PgEdge getEdge(Object id) {
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
  public Object id;
  public Set<String> labels = new HashSet<String>();
  public Map<String, Object> properties = new HashMap<>();
  public PgNode(Object id) {
    this.id = id;
  }
  public void addLabel(String label) {
    labels.add(label);
  }
  public void addProperty(String key, Object value) {
    properties.put(key, value);
  }
}

class PgEdge {
  public Object id1;
  public Object id2;
  public boolean undirected = false;
  public Set<String> labels = new HashSet<String>();
  public Map<String, String> properties = new HashMap<>();
  public PgEdge(Object id1, Object id2, boolean undirected) {
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