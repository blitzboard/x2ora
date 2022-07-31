package x2oracle;

public class PgGraphNamed {

  private String name;
  private PgGraph pg = new PgGraph();

  public PgGraphNamed() {
  }

  public PgGraphNamed(String name, PgGraph pg) {
    this.name = name;
    this.pg = pg;
  }
  
  public String getName() {
    return name;
  }
  public PgGraph getPg() {
    return pg;
  }

  public void setName(String name) {
    this.name = name;
  }
  public void setPg(PgGraph pg) {
    this.pg = pg;
  }
}
