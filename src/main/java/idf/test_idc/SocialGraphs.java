package idf.test_idc;

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

public class SocialGraphs {
	/**
	 * Construction de graphes d'interactions sur la zone de l'Île de la Cité
	 * entre 2014 et 2015. Construction des graphes de : co-édition, largeur de
	 * collaboration, profondeur de collaboration, utilisation
	 * 
	 * @param args
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 }; // ile de la cité
		String[] timespan = { "2014-01-01", "2015-12-31" };

		loader.getDataFrombbox(bbox, timespan);
		System.out.println("Size Myjavaobjects :" + loader.myJavaObjects.size());
		OSMResourceQualityAssessment assess = new OSMResourceQualityAssessment(loader.myJavaObjects);
		assess.writeOSMObjectsDetails2CSV("idc_details_contributions_2014-2015.csv");

		HashMap<Long, OSMContributor> myContributors = ContributorAssessment.contributorSummary(loader.myJavaObjects);
		ContributorAssessment.writeContributorSummary(myContributors, new File("idf_contributors_idc_2014-2015.csv"));

		// Order by object
		HashMap<Long, OSMObject> myObjects = OSMResourceQualityAssessment.groupByOSMObject(loader.myJavaObjects);
		System.out.println("Size myObjects " + myObjects.size());

		// Graphe de co-édition
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> coeditg = SocialGraph.createCoEditionGraph(myObjects,
				myContributors);
		System.out.println("vertices " + coeditg.vertexSet().size());
		System.out.println("edges " + coeditg.edgeSet().size());
		SocialGraph.writeGraph2CSV(coeditg, new File("idf-idc-coeditGraph_2014-2015.csv"), Long.valueOf(111));

		// Graphe de largeur de collaboration
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> widthCollabg = SocialGraph
				.createCollaborationGraph(myObjects, myContributors, "width");
		System.out.println("vertices " + widthCollabg.vertexSet().size());
		System.out.println("edges " + widthCollabg.edgeSet().size());
		SocialGraph.writeGraph2CSV(widthCollabg, new File("idf-idc-widthGraph_2014-2015.csv"), Long.valueOf(111));

		// Graphe de profondeur de collaboration
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> depthCollabg = SocialGraph
				.createCollaborationGraph(myObjects, myContributors, "depth");
		System.out.println("vertices " + depthCollabg.vertexSet().size());
		System.out.println("edges " + depthCollabg.edgeSet().size());
		SocialGraph.writeGraph2CSV(depthCollabg, new File("idf-idc-depthGraph_2014-2015.csv"), Long.valueOf(111));

		// Graphe d'utilisation
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> useg = SocialGraph.createUseGraph(loader.myJavaObjects,
				timespan[0], timespan[1]);
		SocialGraph.writeGraph2CSV(useg, new File("idf-idc-useGraph_2014-2015.csv"), Long.valueOf(111));
		System.out.println("vertices " + useg.vertexSet().size());
		System.out.println("edges " + useg.edgeSet().size());

	}

}
