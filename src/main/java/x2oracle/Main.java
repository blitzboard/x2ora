package x2oracle;

import io.javalin.Javalin;
import java.sql.Connection;
import java.util.ResourceBundle;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.pg.rdbms.GraphServer;
import oracle.pgx.api.*;

public class Main {

	public static Connection conn;
	public static PgxSession pgxSession;
	public static String strPgview;

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

		// Connection for PGX
		ServerInstance instance = GraphServer.getInstance(
			rb.getString("base_url"),
			rb.getString("username"),
			rb.getString("password").toCharArray()
		);
		pgxSession = instance.createSession("x2ora");
		
	  // Connection for PGV
		PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
		pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
		pds.setURL(rb.getString("jdbc_url"));
		pds.setUser(rb.getString("username"));
		pds.setPassword(rb.getString("password"));
		conn = pds.getConnection();
		conn.setAutoCommit(false);
		strPgview = rb.getString("pgview");

		// Run a test query at startup
		RetrievalController.countNodes();

		System.out.println("INFO: Ready to accept requests");
		app.post("/merge_node/", UpdateController.mergeNode); 
		app.post("/merge_edge/", UpdateController.mergeEdge);
		app.post("/merge_graph/", UpdateController.mergeGraph);
		app.post("/drop/", UpdateController.drop);
		app.get("/query/", RetrievalController.query);
		app.get("/list/", RetrievalController.list);
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