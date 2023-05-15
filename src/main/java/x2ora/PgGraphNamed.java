package x2ora;

import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PgGraphNamed {

  private String id;
  private PgGraph pg = new PgGraph();
  private HashMap<String, List<Object>> properties = new HashMap<>();

  public PgGraphNamed() {
  }

  public PgGraphNamed(String id, PgGraph pg, String properties) {
    this.id = id;
    this.pg = pg;
    this.addProperties(properties);
  }
  
  public String getId() {
    return id;
  }
  public HashMap<String, List<Object>> getProperties() {
    return properties;
  }
  public PgGraph getPg() {
    return pg;
  }

  public void setId(String id) {
    this.id = id;
  }
  public void setPg(PgGraph pg) {
    this.pg = pg;
  }
  public void setProperties(HashMap<String, List<Object>> properties) {
    this.properties = properties;
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

  public String getPropertiesJSON() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(properties);
    return json;
  }
}
