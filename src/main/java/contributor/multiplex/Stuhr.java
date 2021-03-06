package contributor.multiplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fr.ign.cogit.geoxygene.osm.contributor.GraphAnalysis;
import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OSMWay;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class Stuhr {
	public static void main(String[] args) throws Exception {

		LoadFromPostGIS stuhrLoader = new LoadFromPostGIS("localhost", "5432", "bremen", "postgres", "postgres");
		// Get the city boundaries
		Double[] stuhrBoundaries = stuhrLoader.getCityBoundary("Stuhr", "2013-01-01");
		String[] timespan = { "2009-01-01", "2013-01-01" };

		// Load contributions
		// Get nodes
		Set<OSMResource> nodesStuhr = stuhrLoader.getEvolutionNode(stuhrBoundaries, timespan);

		// Get ways
		Set<OSMResource> waysStuhr = stuhrLoader.getEvolutionWay(stuhrBoundaries, timespan);

		stuhrLoader.myJavaObjects.addAll(nodesStuhr);
		stuhrLoader.myJavaObjects.addAll(waysStuhr);
		System.out.println("Size Myjavaobjects :" + stuhrLoader.myJavaObjects.size());

		// Make a summary of the contributors in Katmandou
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment
				.contributorSummary(stuhrLoader.myJavaObjects);
		// ContributorAssessment.writeContributorSummary(myContributors,
		// new File("Allemagne/stuhr-contributors_2009-2013.csv"));

		// Order contributions by object
		// Nodes and ways
		HashMap<Long, OSMObject> myObjects = OSMResourceQualityAssessment.groupByOSMObject(stuhrLoader.myJavaObjects);
		System.out.println("Size myObjects " + myObjects.size());

		// Nodes objects only
		HashMap<Long, OSMObject> nodeObjects = OSMResourceQualityAssessment.groupByOSMObject(nodesStuhr);

		// Way objects only
		HashMap<Long, OSMObject> wayObjects = OSMResourceQualityAssessment.groupByOSMObject(waysStuhr);
		for (OSMObject object : wayObjects.values())
			for (OSMResource r : object.getContributions())
				object.wayComposition.add(((OSMWay) r.getGeom()).getVertices());

		// Configure Social Graph class static attribute
		SocialGraph.dbName = "bremen";

		/************************
		 * Graph creation
		 ************************/
		// Co-edition graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> coeditg = SocialGraph.createCoEditionGraph(myObjects,
				myContributors);

		System.out.println("Co-edition graph : ");
		System.out.println("vertices " + coeditg.vertexSet().size());
		System.out.println("edges " + coeditg.edgeSet().size());

		// SocialGraph.writeGraph2CSV(coeditg, new
		// File("Allemagne/stuhr-coeditiongraph_2009-2013.csv"),
		// Long.valueOf(111)); // Export to csv

		// Breadth of collaboration graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> widthg = SocialGraph.createCollaborationGraph(myObjects,
				myContributors, "width");

		System.out.println("Breadth of collaboration graph : ");
		System.out.println("vertices " + widthg.vertexSet().size());
		System.out.println("edges " + widthg.edgeSet().size());
		// SocialGraph.writeGraph2CSV(widthg, new
		// File("Allemagne/stuhr-widthgraph_2009-2013.csv"), Long.valueOf(111));

		// Depth of collaboration graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> depthg = SocialGraph.createCollaborationGraph(myObjects,
				myContributors, "depth");

		System.out.println("Depth of collaboration graph : ");
		System.out.println("vertices " + depthg.vertexSet().size());
		System.out.println("edges " + depthg.edgeSet().size());
		// SocialGraph.writeGraph2CSV(depthg, new
		// File("Allemagne/stuhr-depthgraph_2009-2013.csv"), Long.valueOf(111));
		// Export to csv
		// Use graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> useg = SocialGraph.createUseGraph2(myContributors,
				wayObjects, nodeObjects, timespan[0]);

		System.out.println("Use graph : ");
		System.out.println("vertices " + useg.vertexSet().size());
		System.out.println("edges " + useg.edgeSet().size());
		// SocialGraph.writeGraph2CSV(useg, new
		// File("Allemagne/stuhr-usegraph_2009-2013.csv"), Long.valueOf(111));

		// Suppression graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> suppressiong = SocialGraph
				.createSuppressionGraph2(myObjects, myContributors);

		System.out.println("Suppression graph : ");
		System.out.println("vertices " + suppressiong.vertexSet().size());
		System.out.println("edges " + suppressiong.edgeSet().size());
		// SocialGraph.writeGraph2CSV(suppressiong, new
		// File("Allemagne/stuhr-suppressiongraph_2009-2013.csv"),
		// Long.valueOf(111)); // Export to csv

		// Co-location graph : threshold = 10000 i.e. les zones d'activités
		// sont des polygones de côté < 10 km
		SimpleWeightedGraph<Long, DefaultWeightedEdge> colocationg = SocialGraph.createCoLocationGraph(myContributors,
				nodesStuhr, 10000, "5681");

		System.out.println("Co-location graph : ");
		System.out.println("vertices " + colocationg.vertexSet().size());
		System.out.println("edges " + colocationg.edgeSet().size());
		// SocialGraph.writeSimpleWeightedGraph2CSV(colocationg, new
		// File("Allemagne/stuhr-colocationgraph_2009-2013.csv"),
		// Long.valueOf(111));

		// Co-temporal graph
		SimpleWeightedGraph<Long, DefaultWeightedEdge> cotempg = SocialGraph.createCoTemporalGraph(myContributors,
				timespan);

		System.out.println("Co-Temporal graph : ");
		System.out.println("vertices " + cotempg.vertexSet().size());
		System.out.println("edges " + cotempg.edgeSet().size());
		// SocialGraph.writeSimpleWeightedGraph2CSV(cotempg, new
		// File("Allemagne/stuhr-co-temporalgraph_2009-2013.csv"),
		// Long.valueOf(111));

		/******************************
		 * Multiplex system processing
		 ******************************/

		// Transform directed graphs into undirected graphs
		SimpleWeightedGraph<Long, DefaultWeightedEdge> simpleCoeditg = GraphAnalysis.directedGraph2simpleGraph(coeditg);
		SimpleWeightedGraph<Long, DefaultWeightedEdge> simpleWidthg = GraphAnalysis.directedGraph2simpleGraph(widthg);
		SimpleWeightedGraph<Long, DefaultWeightedEdge> simpleDepthg = GraphAnalysis.directedGraph2simpleGraph(depthg);
		SimpleWeightedGraph<Long, DefaultWeightedEdge> simpleUseg = GraphAnalysis.directedGraph2simpleGraph(useg);
		SimpleWeightedGraph<Long, DefaultWeightedEdge> simpleSuppressiong = GraphAnalysis
				.directedGraph2simpleGraph(suppressiong);

		// For all the graphs that were initially directed graph, only keep the
		// edges which weight > threshold
		HashMap<Long, Integer> contributorIndex = GraphAnalysis.contributorIndexForMultiplex(myContributors);
		double[][] adjacencyCoeditg = GraphAnalysis.createLayerAdjacencyMatrix(simpleCoeditg, 3, contributorIndex);
		double[][] adjacencyWidthg = GraphAnalysis.createLayerAdjacencyMatrix(simpleWidthg, 3, contributorIndex);
		double[][] adjacencyDepthg = GraphAnalysis.createLayerAdjacencyMatrix(simpleDepthg, 2, contributorIndex);
		double[][] adjacencyUseg = GraphAnalysis.createLayerAdjacencyMatrix(simpleUseg, 1, contributorIndex);
		double[][] adjacencySuppressiong = GraphAnalysis.createLayerAdjacencyMatrix(simpleSuppressiong, 1,
				contributorIndex);

		double[][] adjacencyCoLocationg = GraphAnalysis.createLayerAdjacencyMatrix(colocationg, 2, contributorIndex);
		double[][] adjacencyCoTemporalg = GraphAnalysis.createLayerAdjacencyMatrix(cotempg, 1.5, contributorIndex);

		// Create the multiplex adjacency list of matrix
		ArrayList<double[][]> adjMultiplex = new ArrayList<double[][]>();
		adjMultiplex.add(adjacencyCoeditg);
		adjMultiplex.add(adjacencyWidthg);
		adjMultiplex.add(adjacencyDepthg);
		adjMultiplex.add(adjacencyUseg);
		adjMultiplex.add(adjacencySuppressiong);
		adjMultiplex.add(adjacencyCoLocationg);
		adjMultiplex.add(adjacencyCoTemporalg);

		// Transform into a monoplex graph
		SimpleWeightedGraph<Long, DefaultWeightedEdge> monoplexg = GraphAnalysis.monoplex(adjMultiplex,
				contributorIndex);
		// SocialGraph.writeSimpleWeightedGraph2CSV(monoplexg, new
		// File("Allemagne/stuhr-monoplexg_2009-2013.csv"),
		// Long.valueOf(111)); // Export
		// // to
		// // csv
		// Write indicators
		HashMap<Long, Double> cc1 = GraphAnalysis.clusteringCoefficient1(adjMultiplex, contributorIndex);
		HashMap<Long, Double> cc2 = GraphAnalysis.clusteringCoefficient2(adjMultiplex, contributorIndex);
		HashMap<Long, Double> participationList = GraphAnalysis.participationCoefficientMultiplex(adjMultiplex,
				contributorIndex);
		GraphAnalysis.writeIndicators(contributorIndex, adjMultiplex, participationList, cc1, cc2,
				"Allemagne/stuhr-multiplex-indicateurs_2009-2013.csv");

	}

}
