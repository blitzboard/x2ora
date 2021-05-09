package x2oracle;

import io.javalin.Javalin;
import oracle.pg.rdbms.pgql.jdbc.PgqlJdbcRdbmsDriver;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.ResourceBundle;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main {

	public static Connection conn;
	public static String strGraph;

	public static void main(String[] args) throws Exception {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
		}).start(7000);
		
		ResourceBundle rb = ResourceBundle.getBundle("common");
		DriverManager.registerDriver(new PgqlJdbcRdbmsDriver());
		conn = DriverManager.getConnection(
			rb.getString("jdbc_url"),
			rb.getString("username"),
			rb.getString("password")
			);
		conn.setAutoCommit(false);
		strGraph = rb.getString("graph");

		RetrievalController.test();
		System.out.println("INFO: Ready to accept requests");
		app.get("/merge_node/", UpdateController.mergeNode); 
		app.get("/merge_edge/", UpdateController.mergeEdge);
	}

	public static String printException(Exception e) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

}