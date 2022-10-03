package x2ora;

import io.javalin.Javalin;
import java.sql.Connection;
import java.util.ResourceBundle;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.pg.rdbms.GraphServer;
import oracle.pg.rdbms.pgql.PgqlConnection;
import oracle.pgx.api.*;

public class Main {

	public static Connection conn;
	public static PgqlConnection pgqlConn;
	public static PgxSession pgxSession;
	public static String strPgv;
	public static String strPgvGraph;
	public static String strPgvNode;
	public static String strPgvEdge;

	public static void main(String[] args) throws Exception {

		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logger.setLevel(Level.WARN);

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
		pgqlConn = PgqlConnection.getConnection(conn);
		strPgv = rb.getString("pgview");
		strPgvGraph = strPgv + "graph";
		strPgvNode = strPgv + "node";
		strPgvEdge = strPgv + "edge";

		// Run a test query at startup
		RetrievalController.countNodes();

		System.out.println("INFO: Ready to accept requests");
		//app.post("/merge_node/", UpdateController.mergeNode); 
		//app.post("/merge_edge/", UpdateController.mergeEdge);
		//app.post("/merge_graph/", UpdateController.mergeGraph);
		app.post("/create/", UpdateController.create);
		app.post("/drop/", UpdateController.drop);
		app.get("/query/", RetrievalController.query);
		app.get("/query_table/", RetrievalController.queryTable);
		app.get("/list/", RetrievalController.list);
		app.get("/get/", RetrievalController.get);
	}
	
	private static SslContextFactory getSslContextFactory() {
		SslContextFactory sslContextFactory = new SslContextFactory.Server();
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