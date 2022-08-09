package x2oracle;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PgGraph {

  private HashSet<PgNode> nodes = new HashSet<>();
  private HashSet<PgEdge> edges = new HashSet<>();

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
    return nodes.size();
  }
  public Integer countEdges() {
    return edges.size();
  }

  public void addNode(PgNode node) {
    nodes.add(node);
  }
  public void addEdge(PgEdge edge) {
    edges.add(edge);
  }

  public boolean hasNodeId(Object id) {
    for(PgNode node : nodes) {
      if (node.getId().equals(id)) return true;
    }
    return false;
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

  private Object id;
  private HashSet<String> labels = new HashSet<>();
  private HashMap<String, List<Object>> properties = new HashMap<>();

  public PgNode() {
  }

  public PgNode(Object id, String label, String props) {
    this.id = id;
    this.addLabel(label);
    this.addProperties(props);
  }

  public Object getId() {
    return id;
  }
  public HashSet<String> getLabels() {
    return labels;
  }
  public HashMap<String, List<Object>> getProperties() {
    return properties;
  }

  public void setId(String id) {
    this.id = id;
  }
  public void setLabels(HashSet<String> labels) {
    this.labels = labels;
  }
  public void setProperties(HashMap<String, List<Object>> properties) {
    this.properties = properties;
  }

  public void addLabel(String label) {
    this.labels.add(label.toLowerCase());
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

  public String getLabel() { // Get the first label only
    String label = "";
    if (labels.size() > 0) {
      Iterator<String> iterator = labels.iterator();
      label = (String)iterator.next();
    }
    return label;
  }

  public String getPropertiesJSON() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(properties);
    return json;
  }
}

class PgEdge {

  private Object from;
  private Object to;
  private boolean undirected = false;
  private HashSet<String> labels = new HashSet<>();
  private HashMap<String, List<Object>> properties = new HashMap<>();

  public PgEdge() {
  }

  public PgEdge(Object from, Object to, boolean undirected, String label, String props) {
    this.from = from;
    this.to = to;
    this.undirected = undirected;
    this.addLabel(label);
    this.addProperties(props);
  }

  public Object getFrom() {
    return from;
  }
  public Object getTo() {
    return to;
  }
  public boolean getUndirected() {
    return undirected;
  }
  public HashSet<String> getLabels() {
    return labels;
  }
  public HashMap<String, List<Object>> getProperties() {
    return properties;
  }

  public void setFrom(String from) {
    this.from = from;
  }
  public void setTo(String to) {
    this.to = to;
  }
  public void setUndirected(boolean undirected) {
    this.undirected = undirected;
  }
  public void setLabels(HashSet<String> labels) {
    this.labels = labels;
  }
  public void setProperties(HashMap<String, List<Object>> properties) {
    this.properties = properties;
  }

  public void addLabel(String label) {
    labels.add(label.toLowerCase());
  }

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

  public String getLabel() {
    String label = "";
    if (labels.size() > 0) {
      Iterator<String> iterator = labels.iterator();
      label = (String)iterator.next();
    }
    return label;
  }

  public String getPropertiesJSON() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(properties);
    return json;
  }
}