package x2oracle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import io.javalin.Javalin;
import oracle.pgql.lang.PgqlException;
import oracle.pg.rdbms.pgql.jdbc.PgqlJdbcRdbmsDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Test {
	public static void main(String[] szArgs) throws Exception {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
		}).start(7000);
		
		String jdbcUrl = "jdbc:oracle:pgql:@168.138.212.125:1521/dbcs03_pdb1.sub02270615590.ryota.oraclevcn.com";

		DriverManager.registerDriver(new PgqlJdbcRdbmsDriver());
		Connection conn = DriverManager.getConnection(jdbcUrl, "graph_dev", "WELcome123##");
		conn.setAutoCommit(false);
		app.get("/merge_node/", ctx -> ctx.result(runMergeNode(conn, ctx.queryParam("label"), ctx.queryParam("id"))));
		app.get("/merge_edge/", ctx -> ctx.result(runMergeEdge(conn, ctx.queryParam("label"), ctx.queryParam("src_id"), ctx.queryParam("dst_id"))));
	}

	public static String runMergeNode(Connection conn, String strLabel, String strId) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			Boolean able = true;
			PreparedStatement ps;
			ResultSet rs;

			// Check if the node exists
			if (able) {
				ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON graph1 WHERE LABEL(v) = ? AND v.id = ?");
				ps.setString(1, strLabel);
				ps.setString(2, strId);
				ps.execute();
				rs = ps.getResultSet();
				if (rs.first()){
					able = false;
					result = "Node " + strLabel + " " + strId + " exists.";
				}
			}

			// Insert the node if not exists
			if (able) {
				String query = "INSERT INTO graph1 VERTEX v LABELS (" + strLabel + ") PROPERTIES (v.id = ?)";			
				ps = conn.prepareStatement(query);
				ps.setString(1, strId);
				ps.execute();
				conn.commit();
				result = "Node " + strLabel + " " + strId + " is added.";
			}
			
		} catch (SQLException e) {
			result = printException(e);
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	public static String runMergeEdge(Connection conn, String strLabel, String strSrcId, String strDstId) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			
			Boolean able = true;
			PreparedStatement ps;
			ResultSet rs;

			// Check if the node exists
			if (able) {
				ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON graph1 WHERE v.id = ?");
				ps.setString(1, strSrcId);
				ps.execute();
				rs = ps.getResultSet();
				if (!rs.first()){
					able = false;
					result = "Node " + strSrcId + " does not exist.";
				}
			}

			// Check if the node exists
			if (able) {
				ps = conn.prepareStatement("SELECT ID(v) FROM MATCH (v) ON graph1 WHERE v.id = ?");
				ps.setString(1, strDstId);
				ps.execute();
				rs = ps.getResultSet();
				if (!rs.first()){
					able = false;
					result = "Node " + strDstId + " does not exist.";
				}
			}

			// Check if the edge exists
			if (able) {
				ps = conn.prepareStatement("SELECT ID(e) FROM MATCH (src)-[e]->(dst) ON graph1 WHERE LABEL(e) = ? AND src.id = ? AND dst.id = ?");
				ps.setString(1, strLabel);
				ps.setString(2, strSrcId);
				ps.setString(3, strDstId);
				ps.execute();
				rs = ps.getResultSet();
				if (rs.first()){
					able = false;
					result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " exists.";
				}
			}

			// Insert the edge if not exists
			if (able) {
				String query = "INSERT INTO graph1 EDGE e BETWEEN src AND dst LABELS (" + strLabel + ") "
										 + "FROM MATCH (src) ON graph1, MATCH (dst) ON graph1 WHERE src.id = ? AND dst.id = ?";			
				ps = conn.prepareStatement(query);
				ps.setString(1, strSrcId);
				ps.setString(2, strDstId);
				ps.execute();
				conn.commit();
				result = "Edge " + strLabel + " " + strSrcId + " -> " + strDstId + " is added.";
			}
			
		} catch (SQLException e) {
			result = printException(e);
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String printException(Exception e) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

}