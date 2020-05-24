package x2oracle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

//import org.strategoxt.stratego_lib.int_0_0;

import io.javalin.Javalin;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;

// For testing - http://localhost:7000/node_match?node_ids=1

public class Test {
	public static void main(String[] szArgs) throws Exception {
		Javalin app = Javalin.create(config -> {
			config.enableCorsForAllOrigins();
		}).start(7000);
		app.get("/node_match/", ctx -> ctx.result(getResult(
			"node_match",
			ctx.queryParam("node_ids"))));
		app.get("/traversal/", ctx -> ctx.result(getResult(
			"traversal",
			ctx.queryParam("node_ids"))));
		app.get("/cycle/", ctx -> ctx.result(getResult(
			"cycle",
			ctx.queryParam("node_ids"))));
		app.get("/query/", ctx -> ctx.result(getResult(
			"query",
			ctx.queryParam("node_ids"))));
	}
  
	private static String getResult(String endpoint, String node_ids) {
		long time_start = System.nanoTime();
		String result = "";
		try {
			ServerInstance instance = Pgx.getInstance("http://localhost:7007");
			PgxSession session = instance.createSession("my-session");
			PgxGraph graph = session.getGraph("Cycle");
			
			String query;
			PgqlResultSet rs;

			switch (endpoint) {
			
			case "node_match":
				query = "SELECT n, m, e MATCH (n)-[e]->(m) WHERE ID(n) = " + node_ids;
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 2, 1, 0, 0, node_ids);
				break;
			
			case "traversal":
				query = "SELECT ID(n), ID(n1), ID(n2), ID(n3), ID(n4), ID(n5), ID(n6),"
						+ " ID(n) AS e1s, ID(n1) AS e1d,"
						+ " ID(n1) AS e2s, ID(n2) AS e2d,"
						+ " ID(n2) AS e3s, ID(n3) AS e3d,"
						+ " ID(n3) AS e4s, ID(n3) AS e4d,"
						+ " ID(n4) AS e5s, ID(n4) AS e5d,"
						+ " ID(n5) AS e6s, ID(n5) AS e6d"
						+ " MATCH (n)-[e1]->(n1)-[e2]->(n2)-[e3]->(n3)-[e4]->(n4)-[e5]->(n5)-[e6]->(n6)"
						+ " WHERE ID(n) = " + node_ids;
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 7, 6, 0, 0, node_ids);
				break;

			case "cycle":
				query = "SELECT ID(n), ARRAY_AGG(ID(m)), ARRAY_AGG(ID(e))"
				+ " MATCH TOP 2 SHORTEST ((n) (-[e:transfer]->(m))* (n)) WHERE ID(n) = "
				+ node_ids;
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 1, 0, 1, 1, node_ids);
				break;
			
			case "query":
				break;
			}
			
		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long time_end = System.nanoTime();
		System.out.println("Execution Time: " + (time_end - time_start)/1000/1000 + "ms");
		return result;		
	}
	
	private static String getResultPG(PgqlResultSet rs, int cnt_v, int cnt_e, int cnt_vl, int cnt_el, String node_ids) {
		PgGraph pg = new PgGraph();
		pg.setName("test_graph");
		try {
			while (rs.next()) {
				for (int i = 1; i <= cnt_v; i++) {
					addNodeById(pg, rs.getInteger(i).toString());
				}
				for (int i = cnt_v + 1; i <= cnt_v + (cnt_e * 2); i = i + 2) {
					//addEdge(pg, rs.getEdge(i));
					addEdgeByIds(pg, rs.getInteger(i).toString(), rs.getInteger(i + 1).toString(), "transfer");
				}
				for (int i = cnt_v + (cnt_e * 2) + 1; i <= cnt_v + (cnt_e * 2) + cnt_vl; i++) {
					if(rs.getList(i) != null) {
						String node_src = node_ids;
						for (Object nodeId : rs.getList(i)) {
							String node_dst = nodeId.toString();
							addNodeById(pg, nodeId.toString());
							addEdgeByIds(pg, node_src, node_dst, "transfer");
							node_src = node_dst;
						}
					}
				}
			}
		} catch (PgqlException e) {
			e.printStackTrace();
		}
		return pg.exportJSON();
	}
  
	private static String printException(Exception e) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}
  
	private static void addNode(PgGraph pg, PgxVertex<?> v) {
		String id = v.getId().toString();
		PgNode node = new PgNode(id);
		pg.addNode(id, node);
	}

	private static void addNodeById(PgGraph pg, String id) {
		PgNode node = new PgNode(id);
		pg.addNode(id, node);
	}

	private static void addEdge(PgGraph pg, PgxEdge e) {
		PgEdge edge = new PgEdge(
				e.getSource().getId().toString(),
				e.getDestination().getId().toString(),
				false
				);
		edge.addLabel(e.getLabel());
		pg.addEdge(edge);
	}
	private static void addEdgeByIds(PgGraph pg, String id_s, String id_d, String label) {
		PgEdge edge = new PgEdge(
				id_s,
				id_d,
				false
				);
		edge.addLabel(label);
		pg.addEdge(edge);
	}
}