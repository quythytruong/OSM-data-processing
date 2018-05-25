package contributor;

import java.io.File;
import java.util.HashMap;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

/***
 * Demo : Building coedition graph on OSM data mapped in Witry-lès-Reims.
 * 
 * @author QTTruong
 *
 */
public class WitryLesReimsGraphs {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "champagne-ardenne-osh", "postgres",
				"postgres");

		Double[] bbox = { 4.10, 49.28, 4.13, 49.2977 }; // Witry-lès-Reims
		String[] timespan = { "2010-01-01", "2017-12-31" };

		System.out.println("Get data");
		loader.getDataFrombbox(bbox, timespan);

		// Make a summary of the contributors in Witry-lès-Reims
		System.out.println("MyJavaObject's size : " + loader.myJavaObjects.size());
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment.contributorSummary(loader.myJavaObjects);

		// Graphs creation
		// Order by object
		HashMap<Long, OSMObject> myObjects = OSMResourceQualityAssessment.groupByOSMObject(loader.myJavaObjects);
		System.out.println("Size myObjects " + myObjects.size());

		// Coedition graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> coeditg = SocialGraph.createCoEditionGraph(myObjects,
				myContributors);
		System.out.println("vertices " + coeditg.vertexSet().size());
		System.out.println("edges " + coeditg.edgeSet().size());
		// Export to csv
		SocialGraph.writeGraph2CSV(coeditg, new File("witry-les-reims-coeditGraph_2010-2017.csv"));

	}
}
