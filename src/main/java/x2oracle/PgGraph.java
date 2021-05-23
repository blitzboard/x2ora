package x2oracle;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// members are public for jackson to access and export as JSON

class PgGraph {
  public String name;
  //public Map<Object, PgNode> nodes = new HashMap<>();
  //public Map<Object, PgEdge> edges = new HashMap<>();
  public HashSet<PgNode> nodes = new HashSet<PgNode>();
  public HashSet<PgEdge> edges = new HashSet<PgEdge>();
  public void setName(String name) {
    this.name = name;
  }
  /*
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
  */
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