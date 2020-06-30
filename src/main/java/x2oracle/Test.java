package x2oracle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
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

		ServerInstance instance = Pgx.getInstance("http://localhost:7007");
		PgxSession session = instance.createSession("my-session");

		app.get("/node_match/", ctx -> ctx.result(
				runNodeMatch(session, ctx.queryParam("labels"), ctx.queryParam("order_by"), ctx.queryParam("limit"))));
		app.get("/custom/recommendation/",
				ctx -> ctx.result(runCustomRecommendation(session, ctx.queryParam("node_ids"), ctx.queryParam("labels"),
						ctx.queryParam("order_by"), ctx.queryParam("limit"))));
		app.get("/traversal/", ctx -> ctx.result(runTraversal(session, ctx.queryParam("node_ids"))));
		app.get("/cycle/", ctx -> ctx.result(runCycle(session, ctx.queryParam("node_ids"))));
		app.get("/path/shortest/", ctx -> ctx
				.result(runPathShortest(session, ctx.queryParam("src_node_ids"), ctx.queryParam("dst_node_ids"))));
		app.get("/compute/personalized_pagerank", ctx -> ctx.result(runComputePersonalizedPagerank(session, ctx.queryParam("node_ids"))));
	}

	private static String runTraversal(PgxSession session, String strNodeID) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Cycle");

			Set<VertexProperty<?, ?>> vertexProperties = graph.getVertexProperties();
			List<VertexProperty<?, ?>> list = new ArrayList<VertexProperty<?, ?>>(vertexProperties);

			String query = "";
			query = "SELECT DISTINCT" + " ID(n0), LABEL(n0)," + " ID(n1), LABEL(n1)," + " ID(n2), LABEL(n2),"
					+ " ID(n3), LABEL(n3)," + " ID(n4), LABEL(n4)," + " ID(n5), LABEL(n5),"
					+ " ID(n6), LABEL(n6)," + " ID(e1), ID(n0) AS e1s, ID(n1) AS e1d,"
					+ " ID(e2), ID(n1) AS e2s, ID(n2) AS e2d," + " ID(e3), ID(n2) AS e3s, ID(n3) AS e3d,"
					+ " ID(e4), ID(n3) AS e4s, ID(n4) AS e4d," + " ID(e5), ID(n4) AS e5s, ID(n5) AS e5d,"
					+ " ID(e6), ID(n5) AS e6s, ID(n6) AS e6d"
					+ " MATCH (n0)-[e1]->(n1)-[e2]->(n2)-[e3]->(n3)-[e4]->(n4)-[e5]->(n5)-[e6]->(n6)"
					+ " WHERE ID(n0) = " + strNodeID;
			PgqlResultSet rs = graph.queryPgql(query);
			result = getResultPG(rs, 7, 6, 0, 0, strNodeID, list);

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String runCycle(PgxSession session, String strNodeID) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Cycle");

			Set<VertexProperty<?, ?>> vertexProperties = graph.getVertexProperties();
			List<VertexProperty<?, ?>> list = new ArrayList<VertexProperty<?, ?>>(vertexProperties);

			String query = "";
			query = query.concat("SELECT " + strNode("n", list) + "\n");
			query = query.concat("       ARRAY_AGG(ID(m)), ARRAY_AGG(LABEL(m))," + "\n");
			query = query.concat("       ARRAY_AGG(ID(e)), ARRAY_AGG(LABEL(e))" + "\n");
			query = query.concat(" MATCH TOP 2 SHORTEST ((n) (-[e:transfer]->(m))* (n))" + "\n");
			query = query.concat(" WHERE ID(n) = " + strNodeID + "\n");
			System.out.println("INFO: Query: \n\n" + query + "\n");
			PgqlResultSet rs = graph.queryPgql(query);
			result = getResultPG(rs, 1, 0, 1, 1, strNodeID, list);

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String runComputePersonalizedPagerank(PgxSession session, String strNodeID) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Online Retail");

			Analyst analyst = session.createAnalyst();
			System.out.println("INFO: node_ids: " + strNodeID);
			PgxVertex<String> vertex = graph.getVertex(strNodeID);
			VertexSet<String> vertexSet = graph.createVertexSet();
			vertexSet.add(vertex);
			graph.destroyVertexPropertyIfExists("pagerank");
			analyst.personalizedPagerank(graph, vertexSet);
			System.out.println("INFO: personalizedPagerank executed");

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		result = "{\"status\":\"success\"}";
		return result;
	}

	private static String runNodeMatch(PgxSession session, String strLabel, String strOrderBy, String strLimit) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Online Retail");

			Set<VertexProperty<?, ?>> vertexProperties = graph.getVertexProperties();
			List<VertexProperty<?, ?>> list = new ArrayList<VertexProperty<?, ?>>(vertexProperties);

			String query = "";
			query = query.concat("SELECT " + strNode("n", list) + "\n");
			query = query.concat(" MATCH (n)" + "\n");
			query = query.concat(" WHERE LABEL(n) = '" + strLabel + "'" + "\n");
			query = query.concat(" ORDER BY n." + strOrderBy + " DESC" + "\n");
			query = query.concat(" LIMIT " + strLimit);
			System.out.println("INFO: Query: \n\n" + query + "\n");

			PgqlResultSet rs = graph.queryPgql(query);
			result = getResultPG(rs, 1, 0, 0, 0, "", list);

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String runCustomRecommendation(PgxSession session, String strNodeID, String strLabel, String strOrderBy,
			String strLimit) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Online Retail");

			Set<VertexProperty<?, ?>> vertexProperties = graph.getVertexProperties();
			List<VertexProperty<?, ?>> list = new ArrayList<VertexProperty<?, ?>>(vertexProperties);

			String query = "";
			query = query.concat("SELECT " + strNode("n", list) + "\n");
			query = query.concat(" MATCH (n)" + "\n");
			query = query.concat(" WHERE LABEL(n) = '" + strLabel + "'" + "\n");
			query = query.concat("   AND NOT EXISTS (" + "	  SELECT *" + "   MATCH (n)-[:purchased_by]->(a)"
					+ "   WHERE ID(a) = '" + strNodeID + "'" + "   )");
			query = query.concat(" ORDER BY n." + strOrderBy + " DESC" + "\n");
			query = query.concat(" LIMIT " + strLimit);
			System.out.println("INFO: Query: \n\n" + query + "\n");

			PgqlResultSet rs = graph.queryPgql(query);
			result = getResultPG(rs, 1, 0, 0, 0, "", list);

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String runPathShortest(PgxSession session, String strNodeSrcID, String strNodeDstIDs) {
		long timeStart = System.nanoTime();
		String result = "";
		try {
			PgxGraph graph = session.getGraph("Online Retail");

			Set<VertexProperty<?, ?>> vertexProperties = graph.getVertexProperties();
			List<VertexProperty<?, ?>> list = new ArrayList<VertexProperty<?, ?>>(vertexProperties);

			String query = "";
			query = query.concat("SELECT " + strNode("src", list) + "\n");
			query = query.concat("     , " + strNode("dst", list) + "\n");
			query = query.concat("     , ARRAY_AGG(ID(m)), ARRAY_AGG(LABEL(m)), ARRAY_AGG(m.pagerank), ARRAY_AGG(m.description)" + "\n");
			query = query.concat("     , ARRAY_AGG(ID(e)), ARRAY_AGG(LABEL(e))" + "\n");
			query = query.concat(" MATCH TOP 10 SHORTEST ((src) (-[e]->(m))* (dst))" + "\n");
			query = query.concat(" WHERE ID(src) = '" + strNodeSrcID + "'" + "\n");
			query = query.concat("   AND ID(dst) IN (" + strList(strNodeDstIDs) + ")");
			System.out.println("INFO: Query: \n\n" + query + "\n");

			PgqlResultSet rs = graph.queryPgql(query);
			result = getResultPG(rs, 2, 0, 1, 1, strNodeSrcID, list);

		} catch (ExecutionException e) {
			result = printException(e);
		} catch (InterruptedException e) {
			result = printException(e);
		} finally {
		}
		long timeEnd = System.nanoTime();
		System.out.println("INFO: Execution Time: " + (timeEnd - timeStart) / 1000 / 1000 + "ms");
		return result;
	}

	private static String getResultPG(PgqlResultSet rs, int countNode, int countEdge, int countNodeList, int countEdgeList, String strNodeID,
			List<VertexProperty<?, ?>> listVertexProperty) {
		PgGraph pg = new PgGraph();
		pg.setName("test_graph");
		try {
			while (rs.next()) {

				int lengthNode = listVertexProperty.size() + 2; // Properties + ID + Label
				int lengthEdge = 3; // ID + Src Node ID + Dst Node ID
				int lengthNodeList = 4; // ID + Label + pagerank

				int offsetEdge = countNode * lengthNode; // Edge Offset
				int offsetNodeList = offsetEdge + (countEdge * lengthEdge); // Node List Offset

				// Nodes
				for (int i = 1; i <= offsetEdge; i = i + lengthNode) {
					Object id = rs.getObject(i);
					String label = rs.getString(i + 1);
					addNodeById(pg, id, label);
					PgNode node = pg.getNode(id);
					for (int j = 0; j < listVertexProperty.size(); j++) {
						String name = listVertexProperty.get(j).getName();
						String type = listVertexProperty.get(j).getType().name();
						if (type == "STRING") {
							if (rs.getString(i + 2 + j) != null) {
								node.addProperty(name, rs.getString(i + 2 + j));
							}
						}
						if (type == "DOUBLE") {
							if (rs.getDouble(i + 2 + j) != null) {
								node.addProperty(name, rs.getDouble(i + 2 + j));
							}
						}
					}
				}
				// Edges
				for (int i = offsetEdge + 1; i <= offsetNodeList; i = i + lengthEdge) {
					addEdgeByIds(pg, rs.getObject(i), rs.getObject(i + 1), rs.getObject(i + 2), "transfer");
				}
				// Node List + Edge List
				for (int i = offsetNodeList + 1; i <= offsetNodeList + countNodeList; i++) {
					if (rs.getList(i) != null) {
						Object nodeSrcID = strNodeID;
						Object nodeDstID;
						String nodeDstLabel;
						Object edge;
						String edgeLabel;
						for (int j = 0; j < rs.getList(i).size(); j++) {

							nodeDstID = rs.getList(i).get(j);
							nodeDstLabel = (String) rs.getList(i + 1).get(j);
							Double nodeDstPagerank = (Double) rs.getList(i + 2).get(j);
							
							addNodeById(pg, nodeDstID, nodeDstLabel);
							PgNode node = pg.getNode(nodeDstID);
							node.addProperty("pagerank", nodeDstPagerank);

							/*
							System.out.println(rs.getList(i + 2).size());
							System.out.println(rs.getList(i + 3).size());
							*/

							/*
							if (rs.getList(i + 3).get(j) != null) {
								String nodeDstDescription = (String) rs.getList(i + 3).get(j);
								node.addProperty("description", nodeDstDescription);							
							}
							*/

							edge = rs.getList(i + countNodeList * lengthNodeList).get(j);
							edgeLabel = (String) rs.getList(i + countNodeList * lengthNodeList + 1).get(j);
							addEdgeByIds(pg, edge, nodeSrcID, nodeDstID, edgeLabel);

							nodeSrcID = nodeDstID;
						}
					}
				}
			}
		} catch (PgqlException e) {
			e.printStackTrace();
		}
		return pg.exportJSON();
	}

	private static String strNode(String symbol, List<VertexProperty<?, ?>> list) {
		String strID = "ID(" + symbol + ")";
		String strLabel = "LABEL(" + symbol + ")";
		List<String> listProperties = new ArrayList<String>();
		for (int i = 0; i < list.size(); i++) {
			listProperties.add(symbol + "." + list.get(i).getName());
		}
		String strProperties = String.join(", ", listProperties);
		return strID + ", " + strLabel + ", " + strProperties;
	}

	private static String strList(String elements) {
		List<String> listElement = new ArrayList<String>();
		for (String element : elements.split(",")) {
			listElement.add("'" + element + "'");
		}
		return String.join(",", listElement);
	}

	private static String printException(Exception e) {
		e.printStackTrace();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	private static void addNodeById(PgGraph pg, Object id, String label) {
		PgNode node = new PgNode(id);
		node.addLabel(label);
		pg.addNode(id, node);
	}

	private static void addEdgeByIds(PgGraph pg, Object id, Object idSrc, Object idDst, String label) {
		PgEdge edge = new PgEdge(idSrc, idDst, false);
		edge.addLabel(label);
		pg.addEdge(id, edge);
	}
}