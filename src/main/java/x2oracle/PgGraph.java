package x2oracle;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// members are public for jackson to access and export as JSON

class PgGraph {
  public String name;
  public Map<Long, PgNode> nodes = new HashMap<>();
  public List<PgEdge> edges = new ArrayList<PgEdge>();
  public void setName(String name) {
    this.name = name;
  }
  public void addNode(long id, PgNode node) {
    nodes.put(id, node);
  }
  public void addEdge(PgEdge edge) {
    edges.add(edge);
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
  public long id;
  public Set<String> labels = new HashSet<String>();
  public Map<String, String> properties = new HashMap<>();
  public PgNode(long id) {
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
  public long id1;
  public long id2;
  public boolean undirected = false;
  public Set<String> labels = new HashSet<String>();
  public Map<String, String> properties = new HashMap<>();
  public PgEdge(long id1, long id2, boolean undirected) {
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