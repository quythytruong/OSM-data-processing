package contributor;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class CoTemporalGraphNepal {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS nepalLoader = new LoadFromPostGIS("localhost", "5432", "nepal", "postgres", "postgres");
		// Get the city boundaries
		Double[] katmanduBoundaries = nepalLoader.getCityBoundary("काठमाडौं", "2015-06-01");
		String[] timespan = { "2014-01-01", "2017-01-01" };

		// Load contributions
		// Get nodes
		Set<OSMResource> nodesKatmandu = nepalLoader.getEvolutionNode(katmanduBoundaries, timespan);

		// Get ways
		// Set<OSMResource> waysKatmandu =
		// nepalLoader.getEvolutionWay(katmanduBoundaries, timespan);

		nepalLoader.myJavaObjects.addAll(nodesKatmandu);
		// nepalLoader.myJavaObjects.addAll(waysKatmandu);
		System.out.println("Size Myjavaobjects :" + nepalLoader.myJavaObjects.size());

		// Make a summary of the contributors in Katmandou
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment
				.contributorSummary(nepalLoader.myJavaObjects);
		System.out.println("Size myContributors :" + myContributors.size());

		// Co-location graph : threshold = 10000 i.e. les zones d'activités
		// sont des polygones de côté < 10 km
		// SimpleWeightedGraph<Long, DefaultWeightedEdge> colocationg =
		// SocialGraph.createCoLocationGraph(myContributors,
		// katmanduBoundaries, timespan, 10000, "6207");
		SimpleWeightedGraph<Long, DefaultWeightedEdge> colocationg = SocialGraph.createCoLocationGraph(myContributors,
				nodesKatmandu, 10000, "6207");

		System.out.println("Co-location graph : ");
		System.out.println("vertices " + colocationg.vertexSet().size());
		System.out.println("edges " + colocationg.edgeSet().size());
		SocialGraph.writeSimpleWeightedGraph2CSV(colocationg, new File("Nepal/katmandou-colocationgraph_2014-2017.csv"),
				Long.valueOf(111)); // Export
									// to
									// csv

	}

}
