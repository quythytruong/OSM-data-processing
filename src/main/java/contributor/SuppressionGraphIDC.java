package contributor;

import java.io.File;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class SuppressionGraphIDC {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 }; // ile de la cité
		String[] timespan = { "2009-01-01", "2013-01-01" };

		loader.getDataFrombbox(bbox, timespan);
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> suppressionGraph = SocialGraph
				.createSuppressionGraph(loader.myJavaObjects);
		SocialGraph.writeGraph2CSV(suppressionGraph, new File("idf-idc-suppressionGraph_2009-2012.csv"), (long) 111);
	}

}
