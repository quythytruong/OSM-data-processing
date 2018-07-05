package contributor;

import java.io.File;
import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class CoTemporalGraphIDC {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 }; // ile de la cité
		String[] timespan = { "2009-01-01", "2013-01-01" };

		loader.getDataFrombbox(bbox, timespan);
		System.out.println("Size Myjavaobjects :" + loader.myJavaObjects.size());
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment.contributorSummary(loader.myJavaObjects);
		// ContributorAssessment.writeContributorSummary(myContributors, new
		// File("idf_contributors_idc_2009-2012.csv"));

		SimpleWeightedGraph<Long, DefaultWeightedEdge> cotemporalGraph = SocialGraph
				.createCoTemporalGraph(myContributors, timespan);
		System.out.println("vertices " + cotemporalGraph.vertexSet().size());
		System.out.println("edges " + cotemporalGraph.edgeSet().size());

		SocialGraph.writeSimpleWeightedGraph2CSV(cotemporalGraph,
				new File("idf-idc-coTemporalGraph-Fermeture-2009-2012.csv"), (long) 111);

	}

}
