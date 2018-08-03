package contributor.multiplex;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IEnvelope;
import fr.ign.cogit.geoxygene.datatools.CRSConversion;
import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OSMWay;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Envelope;

public class Aubervilliers {
	public static void main(String[] args) throws Exception {

		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		// Get the city boundaries
		Double[] bbox = loader.getCityBoundary("Aubervilliers", "2014-01-01");
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
		HashMap<Long, OSMObject> myObjects = OSMResourceQualityAssessment.groupByOSMObject(loader.myJavaObjects);
		System.out.println("Size myObjects " + myObjects.size());

		// Nodes objects only
		HashMap<Long, OSMObject> nodeObjects = OSMResourceQualityAssessment.groupByOSMObject(nodes);

		// Way objects only
		HashMap<Long, OSMObject> wayObjects = OSMResourceQualityAssessment.groupByOSMObject(ways);
		for (OSMObject object : wayObjects.values())
			for (OSMResource r : object.getContributions())
				object.wayComposition.add(((OSMWay) r.getGeom()).getVertices());

		// Configure Social Graph class static attribute
		SocialGraph.dbName = "idf";

		// Make a summary of the contributors in Aubervilliers
		HashMap<Long, OSMContributor> myContributors = ContributorAssessment.contributorSummary(loader.myJavaObjects);

		/**************************
		 * Graph creations
		 **************************/

		// Coedition graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> coeditg = SocialGraph.createCoEditionGraph(myObjects,
				myContributors);

		System.out.println("Co-edition graph : ");
		System.out.println("vertices " + coeditg.vertexSet().size());
		System.out.println("edges " + coeditg.edgeSet().size());
		// SocialGraph.writeGraph2CSV(coeditg, new
		// File("Aubervilliers/coedit_graph_2000-20180212.csv"), (long) 0);

		// Use graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> useg = SocialGraph.createUseGraph2(myContributors,
				wayObjects, nodeObjects, timespan[0]);

		System.out.println("Use graph : ");
		System.out.println("vertices " + useg.vertexSet().size());
		System.out.println("edges " + useg.edgeSet().size());
		int max = 0;
		for (OSMContributor user : myContributors.values()) {
			if (useg.inDegreeOf(Long.valueOf(user.getId())) > max)
				max = useg.inDegreeOf(Long.valueOf(user.getId()));
		}
		// SocialGraph.writeGraph2CSV(useg, new
		// File("Aubervilliers/use_graph_2000-20180212.csv"), (long) 0);

		// Suppression graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> suppressiong = SocialGraph
				.createSuppressionGraph2(myObjects, myContributors);

		System.out.println("Suppression graph : ");
		System.out.println("vertices " + suppressiong.vertexSet().size());
		System.out.println("edges " + suppressiong.edgeSet().size());
		// SocialGraph.writeGraph2CSV(suppressiong, new
		// File("Aubervilliers/suppression_graph_2000-20180212.csv"),
		// (long) 0);

		/*************************
		 * Contributor Assessment
		 *************************/
		Map<Long, Object[]> indicatorUser = new HashMap<Long, Object[]>();
		ChangesetRetriever chgstRtv = new ChangesetRetriever("localhost", "5432", "idf", "postgres", "postgres");

		for (OSMContributor user : myContributors.values()) {
			int totalContributions = user.getNbContributions();
			if (totalContributions == 0)
				continue;
			// Nb of creations / total contributions
			int nbCreate = OSMContributorAssessment.getNbCreations(user.getResource());
			Double pCreate = Double.valueOf(nbCreate) / Double.valueOf(totalContributions);

			// Nb of modifications / total contributions
			int nbModif = OSMContributorAssessment.getNbModification(user.getResource());
			Double pModif = Double.valueOf(nbModif) / Double.valueOf(totalContributions);

			// Nb of suppression / total contributions
			int nbDelete = OSMContributorAssessment.getNbDeletes(user.getResource());
			Double pDelete = Double.valueOf(nbDelete) / Double.valueOf(totalContributions);

			// Use indegree / total contributions
			int useIndegree = useg.inDegreeOf(Long.valueOf(user.getId()));
			Double pUsed = Double.valueOf(useIndegree) / max;

			// Coedition indegree / total contributions
			int coeditIndegree = coeditg.inDegreeOf(Long.valueOf(user.getId()));
			Double pCoedited = 1 - Double.valueOf(coeditIndegree) / Double.valueOf(totalContributions);

			// Suppression indegree / total contributions
			int deleteIndegree = suppressiong.inDegreeOf(Long.valueOf(user.getId()));
			Double pDeleted = 1 - Double.valueOf(deleteIndegree) / Double.valueOf(totalContributions);

			int nbWeeks = user.getNbWeeksActivity();

			Set<Integer> changesetIDs = ChangesetRetriever.getAllChangesets(user.getResource());

			Double[] meanChangesetExtent = chgstRtv.getChangesetsMeanExtent(changesetIDs);
			Double lonMin = meanChangesetExtent[0];
			Double latMin = meanChangesetExtent[1];
			Double lonMax = meanChangesetExtent[2];
			Double latMax = meanChangesetExtent[3];

			IDirectPosition lowerCorner = CRSConversion.wgs84ToLambert93(lonMin, latMin);
			IDirectPosition upperCorner = CRSConversion.wgs84ToLambert93(lonMax, latMax);
			IEnvelope chgsetEnvelope = new GM_Envelope(upperCorner, lowerCorner);

			IEnvelope aubervilliersEnvelope = new GM_Envelope(CRSConversion.wgs84ToLambert93(bbox[2], bbox[3]),
					CRSConversion.wgs84ToLambert93(bbox[0], bbox[1]));

			Double focalisation = 0.0;
			if (chgsetEnvelope.getGeom().area() == 0.0)
				if (lowerCorner.getCoordinate(0) == 0.0 && lowerCorner.getCoordinate(1) == 0.0
						&& upperCorner.getCoordinate(0) == 0.0 && upperCorner.getCoordinate(1) == 0.0)
					focalisation = 0.0;
				else
					focalisation = 1.0;
			else {
				if (aubervilliersEnvelope.contains(chgsetEnvelope))
					focalisation = 1.0;
				else if (chgsetEnvelope.contains(aubervilliersEnvelope))
					focalisation = aubervilliersEnvelope.getGeom().area() / chgsetEnvelope.getGeom().area();
				else
					focalisation = aubervilliersEnvelope.getGeom().intersection(chgsetEnvelope.getGeom()).area()
							/ chgsetEnvelope.getGeom().area();
			}

			// Double avg = (pCreate + pModif + pDelete + pUsed + pCoedited +
			// pDelete) / 6;

			// Double avgW = (pCreate + 3 * pModif + 3 * pDelete + 3 * pUsed +
			// pCoedited + pDelete) / 12;

			Object[] indicator = { totalContributions, pCreate, pModif, pDelete, pUsed, pCoedited, pDeleted, nbWeeks,
					focalisation, lonMin, latMin, lonMax, latMax, chgsetEnvelope.getGeom().area(),
					aubervilliersEnvelope.getGeom().area() };
			indicatorUser.put(Long.valueOf(user.getId()), indicator);

		}
		ContributorAssessment.FILE_HEADER = "uid," + "total_contributions," + "p_creation," + "p_modification,"
				+ "p_delete," + "p_is_used," + "p_is_edited," + "p_is_deleted," + "nbWeeks," + "focalisation,"
				+ "lonMin," + "latMin," + "lonMax," + "latMax," + "area_mean_chgst," + "area_aubervilliers_bbox";
		ContributorAssessment.toCSV(indicatorUser, new File("Aubervilliers/user-features_2000-20180213.csv"));

	}

}
