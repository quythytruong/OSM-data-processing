package contributor.multiplex;

import java.util.Set;

import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class AubervilliersUserContributions {

	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		// Get the city boundaries
		Double[] bbox = loader.getCityBoundary("Aubervilliers", "2018-02-13T23:59:59Z");
		String[] timespan = { "2000-01-01", "2018-02-13T23:59:59Z" };

		// Load contributions
		// Get nodes
		Set<OSMResource> nodes = loader.getEvolutionNode(bbox, timespan);

		// Get ways
		Set<OSMResource> ways = loader.getEvolutionWay(bbox, timespan);

		loader.myJavaObjects.addAll(nodes);
		loader.myJavaObjects.addAll(ways);
		System.out.println("Size Myjavaobjects :" + loader.myJavaObjects.size());

		// Order contributions by object
		// Nodes and ways
		// HashMap<Long, OSMObject> myObjects =
		// OSMResourceQualityAssessment.groupByOSMObject(loader.myJavaObjects);
		// System.out.println("Size myObjects " + myObjects.size());

		// Configure Social Graph class static attribute
		SocialGraph.dbName = "idf";

		// Make a summary of the contributors in Aubervilliers
		// HashMap<Long, OSMContributor> myContributors =
		// ContributorAssessment.contributorSummary(loader.myJavaObjects);

		// System.out.println("Nombre de contributions de 83557 : "
		// + myContributors.get(Long.valueOf(83557)).getContributions().size());

		OSMResourceQualityAssessment osmResourceQuality = new OSMResourceQualityAssessment(loader.myJavaObjects);
		osmResourceQuality.writeOSMObjectCSV("Aubervilliers/nodes_ways_2000-20180213.csv");
	}

}
