package contributor;

import java.io.File;
import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class CoLocationGraphIDC {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 }; // ile de la cité
		String[] timespan = { "2009-01-01", "2013-01-01" };

		loader.getDataFrombbox(bbox, timespan);
		System.out.println("Size Myjavaobjects :" + loader.myJavaObjects.size());
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment.contributorSummary(loader.myJavaObjects);
		// ContributorAssessment.writeContributorSummary(myContributors, new
		// File("idf_contributors_idc_2009-2012.csv"));

		SimpleWeightedGraph<Long, DefaultWeightedEdge> colocationGraph = SocialGraph
				.createCoLocationGraph(myContributors, bbox, timespan, (double) 100.0, "2154");
		System.out.println("vertices " + colocationGraph.vertexSet().size());
		System.out.println("edges " + colocationGraph.edgeSet().size());

		SocialGraph.writeSimpleWeightedGraph2CSV(colocationGraph, new File("idf-idc-colocationGraph_2009-2012.csv"),
				Long.valueOf(111));

	}

}
