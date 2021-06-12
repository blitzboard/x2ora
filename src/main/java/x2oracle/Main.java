package x2oracle;

import io.javalin.Javalin;
import oracle.pg.rdbms.pgql.jdbc.PgqlJdbcRdbmsDriver;
import java.sql.DriverManager;
import java.sql.Connection;
import java.util.ResourceBundle;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Main {

	public static Connection conn;
	public static String strGraph;

	public static void main(String[] args) throws Exception {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
			config.server(() -> {
				Server server = new Server();
				ServerConnector connector = new ServerConnector(server);
				ServerConnector sslConnector = new ServerConnector(server, getSslContextFactory());
				connector.setPort(7000);
				sslConnector.setPort(7001);
				server.setConnectors(new Connector[]{sslConnector, connector});
				return server;
			});
		}).start();
		
		ResourceBundle rb = ResourceBundle.getBundle("common");
		DriverManager.registerDriver(new PgqlJdbcRdbmsDriver());
		conn = DriverManager.getConnection(
			rb.getString("jdbc_url"),
			rb.getString("username"),
			rb.getString("password")
			);
		conn.setAutoCommit(false);
		strGraph = rb.getString("graph");

		// Run a test query at startup
		RetrievalController.countNodes();

		System.out.println("INFO: Ready to accept requests");
		app.post("/merge_node/", UpdateController.mergeNode); 
		app.post("/merge_edge/", UpdateController.mergeEdge);
		app.post("/merge_graph/", UpdateController.mergeGraph);
		app.get("/node_match/", RetrievalController.nodeMatch);
		app.get("/edge_match/", RetrievalController.edgeMatch);
	}
	
	private static SslContextFactory getSslContextFactory() {
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(System.getProperty("user.dir") + "/src/main/resources/keystore.jks");
		sslContextFactory.setKeyStorePassword("welcome1");
		return sslContextFactory;
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