package x2oracle;

import io.javalin.http.Handler;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static x2oracle.Main.*;

public class RetrievalController {

  public static String test() {

    long timeStart = System.nanoTime();
    String result = "";
    try {
      PreparedStatement ps;
      ResultSet rs;

      // Check if the node exists
      ps = conn.prepareStatement("SELECT COUNT(v) FROM MATCH (v) ON " + strGraph);
      ps.execute();
      rs = ps.getResultSet();
      if (rs.first()){
        result = "Test query succeeded.";
      }
    
    } catch (SQLException e) {
      result = printException(e);
    }
    long timeEnd = System.nanoTime();
    System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms (" + result + ")");
    return result;
  };

  public static Handler nodeMatch = ctx -> {
  };

}