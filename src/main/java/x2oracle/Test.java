package x2oracle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import io.javalin.Javalin;
import oracle.pgql.lang.PgqlException;
import oracle.pgx.api.*;

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
			ctx.queryParam("node_ids")//,
			//ctx.queryParam("min_hops"),
			//ctx.queryParam("max_hops")
			)));
		app.get("/cycle/", ctx -> ctx.result(getResult(
			"cycle",
			ctx.queryParam("node_ids"))));
		app.get("/path/shortest/", ctx -> ctx.result(getResult(
			"path-shortest",
			ctx.queryParam("src_node_ids"),
			ctx.queryParam("dst_node_ids"))));	
		app.get("/compute/random_walk", ctx -> ctx.result(getResult(
			"compute-random_walk",
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
			PgxGraph graph = session.getGraph("Online Retail");
			
			
			String query;
			PgqlResultSet rs;
			
			switch (endpoint) {
			
			case "node_match":
				query = "SELECT n, m, e MATCH (n)-[e]->(m)"
						+ " WHERE ID(n) = " + node_ids
						+ " ORDER BY n.pagerank DESC";
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 2, 1, 0, 0, node_ids);
				break;
			
			case "traversal":
				query = "SELECT DISTINCT"
						+ " ID(n0), LABEL(n0),"
						+ " ID(n1), LABEL(n1),"
						+ " ID(n2), LABEL(n2),"
						+ " ID(n3), LABEL(n3),"
						+ " ID(n4), LABEL(n4),"
						+ " ID(n5), LABEL(n5),"
						+ " ID(n6), LABEL(n6),"
						+ " ID(e1), ID(n0) AS e1s, ID(n1) AS e1d,"
						+ " ID(e2), ID(n1) AS e2s, ID(n2) AS e2d,"
						+ " ID(e3), ID(n2) AS e3s, ID(n3) AS e3d,"
						+ " ID(e4), ID(n3) AS e4s, ID(n4) AS e4d,"
						+ " ID(e5), ID(n4) AS e5s, ID(n5) AS e5d,"
						+ " ID(e6), ID(n5) AS e6s, ID(n6) AS e6d"
						+ " MATCH (n0)-[e1]->(n1)-[e2]->(n2)-[e3]->(n3)-[e4]->(n4)-[e5]->(n5)-[e6]->(n6)"
						+ " WHERE ID(n0) = " + node_ids;
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 7, 6, 0, 0, node_ids);
				break;

			case "cycle":
				query = "SELECT ID(n), LABEL(n), ARRAY_AGG(ID(m)), ARRAY_AGG(ID(e))"
				+ " MATCH TOP 2 SHORTEST ((n) (-[e:transfer]->(m))* (n)) WHERE ID(n) = "
				+ node_ids;
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 1, 0, 1, 1, node_ids);
				break;

			case "compute-random_walk":
				Analyst analyst = session.createAnalyst();
				System.out.println("node_ids: " + node_ids);
				PgxVertex<String> vertex = graph.getVertex(node_ids);
				VertexSet<String> vertexSet = graph.createVertexSet();
				vertexSet.add(vertex);
				analyst.personalizedPagerank(graph, vertexSet);
				query = "SELECT ID(n), LABEL(n), n.country, n.description"
				        + " MATCH (n)"
						+ " WHERE LABEL(n) = 'Product'"
						+ " ORDER BY n.pagerank DESC LIMIT 10";
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 1, 0, 0, 0, node_ids);
				break;
			}
			session.destroy();
			
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

	private static String getResult(String endpoint, String src_node_ids, String dst_node_ids) {
		long time_start = System.nanoTime();
		String result = "";
		try {
			ServerInstance instance = Pgx.getInstance("http://localhost:7007");
			PgxSession session = instance.createSession("my-session");
			PgxGraph graph = session.getGraph("Online Retail");
			
			System.out.println(graph.getVertexProperties());
			Set<VertexProperty<?,?>> vpl = graph.getVertexProperties();
			System.out.println(vpl.size());
			System.out.println(vpl.toArray());

			List<VertexProperty<?,?>> list = new ArrayList<VertexProperty<?,?>>(vpl);

			//String[] node_props = new String[];
			
			//Iterator<VertexProperty<?,?>> it = vpl.iterator();

			//while(it.hasNext()){
			//	System.out.println(it.next());
			//}

			System.out.println(list.get(0));

			vpl.forEach((VertexProperty<?,?> vp) -> {
				System.out.println(vp);
				System.out.println(vp.getName());
				System.out.println(vp.getType().name());
				//node_props[0][0] = vp.getName();
				//node_props[0][1] = vp.getType();
			});
			
			String query;
			PgqlResultSet rs;
			
			switch (endpoint) {
						
			case "path-shortest":
				query = "SELECT"
				+ " ID(src), LABEL(src)," + " src." + list.get(0).getName() + "," + " src." + list.get(1).getName() + ","
				+ " ID(dst), LABEL(dst)," + " dst." + list.get(0).getName() + "," + " dst." + list.get(1).getName() + ","
				+ " ARRAY_AGG(ID(m)), ARRAY_AGG(LABEL(m)),"
				+ " ARRAY_AGG(ID(e)), ARRAY_AGG(LABEL(e))"
				+ " MATCH TOP 10 SHORTEST ((src) (-[e]->(m))* (dst))"
				+ " WHERE ID(src) = '" + src_node_ids + "'"
				+ "   AND ID(dst) = '" + dst_node_ids + "'";
				rs = graph.queryPgql(query);
				result = getResultPG(rs, 2, 0, 1, 1, src_node_ids);
				break;
			}
			session.destroy();

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
	
	private static String getResultPG(PgqlResultSet rs, int cnt_n, int cnt_e, int cnt_nl, int cnt_el, String node_ids) {
		PgGraph pg = new PgGraph();
		pg.setName("test_graph");
		try {
			while (rs.next()) {

				int length_n = 4;  // ID + Label
				int length_e = 3;  // ID + Src Node ID + Dst Node ID
				int length_nl = 2; // ID + Label

				int offset_e = cnt_n * length_n;                  // Edge Offset
				int offset_nl = offset_e + (cnt_e * length_e);    // Node List Offset
				
				// Nodes
				for (int i = 1; i <= offset_e; i = i + length_n) {
					String id = rs.getString(i);
					String label = rs.getString(i + 1);
					addNodeById(pg, id, label);
					PgNode node = pg.getNode(id);
					if (rs.getString(i + 2) != null) {
						node.addProperty("country", rs.getString(i + 2));
					}
					if (rs.getString(i + 3) != null) {
						node.addProperty("description", rs.getString(i + 3));
					}
				}
				// Edges
				for (int i = offset_e + 1; i <= offset_nl; i = i + length_e) {
					addEdgeByIds(pg, rs.getString(i), rs.getString(i + 1), rs.getString(i + 2), "transfer");
				}
				// Node List + Edge List
				for (int i = offset_nl + 1; i <= offset_nl + cnt_nl; i++) {
					if(rs.getList(i) != null) {
						String node_src = node_ids;
						String node_dst;
						String node_dst_label;
						Long edge;
						String edge_label;
						for (int j = 0; j < rs.getList(i).size(); j++) {
							node_dst = (String) rs.getList(i).get(j);
							node_dst_label = (String) rs.getList(i + 1).get(j);
							edge = (Long) rs.getList(i + cnt_nl * length_nl).get(j);
							edge_label = (String) rs.getList(i + cnt_nl * length_nl + 1).get(j);
							addNodeById(pg, node_dst, node_dst_label);
							addEdgeByIds(pg, edge.toString(), node_src, node_dst, edge_label);
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

	private static void addNodeById(PgGraph pg, String id, String label) {
		PgNode node = new PgNode(id);
		node.addLabel(label);
		pg.addNode(id, node);
	}

	private static void addEdgeByIds(PgGraph pg, String id, String id_s, String id_d, String label) {
		PgEdge edge = new PgEdge(
				id_s,
				id_d,
				false
				);
		edge.addLabel(label);
		pg.addEdge(id, edge);
	}
}