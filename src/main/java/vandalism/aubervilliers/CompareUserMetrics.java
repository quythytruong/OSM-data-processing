package vandalism.aubervilliers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.contributor.SocialGraph;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OSMWay;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMContributorAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;
import fr.ign.cogit.geoxygene.osm.schema.OSMDefaultFeature;
import fr.ign.cogit.geoxygene.util.CollectionsUtil;
import promethee.UserMetricsPromethee;

public class CompareUserMetrics {
	public static void main(String[] args) throws Exception {

		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		// Get the city boundaries
		Double[] bbox = loader.getCityBoundary("Aubervilliers", "2014-01-01");
		String[] timespan = { "2012-01-01", "2018-02-13T23:59:59Z" };

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

		// Suppression graph
		DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> suppressiong = SocialGraph
				.createSuppressionGraph2(myObjects, myContributors);

		System.out.println("Suppression graph : ");
		System.out.println("vertices " + suppressiong.vertexSet().size());
		System.out.println("edges " + suppressiong.edgeSet().size());

		/***********************************************
		 * Contributor Assessment : Metrics calculation
		 ***********************************************/
		Map<Integer, Integer> unsortedNbContributions = new HashMap<Integer, Integer>();
		Map<Integer, Double> unsortedAverageUserMetrics = new HashMap<Integer, Double>();
		Map<Integer, Double> unsortedWeightedAverageUserMetrics = new HashMap<Integer, Double>();
		int nbContrib1 = 0;

		for (OSMContributor user : myContributors.values()) {

			int totalContributions = user.getNbContributions();
			if (totalContributions == 1)
				nbContrib1++;
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
			/***
			 * pCoedited is computed such as : pCoedited = 1 <-> no contribution
			 * has been coedited ; pCoedited = 0 <-> all of his/her
			 * contributions have been edited
			 */
			Double pCoedited = 1 - Double.valueOf(coeditIndegree) / Double.valueOf(totalContributions);

			// Suppression indegree / total contributions
			int deleteIndegree = suppressiong.inDegreeOf(Long.valueOf(user.getId()));
			/**
			 * pDeleted is computed such as : pDeleted = 1 <-> no contribution
			 * has been deleted ; pDeleted = 0 <-> all of his/her contributions
			 * have been deleted
			 */
			Double pDeleted = Double.valueOf(deleteIndegree) / Double.valueOf(totalContributions);

			// Number of contributions per contributor
			unsortedNbContributions.put(user.getId(), totalContributions);

			// Average indicator per contributor
			Double avg = (pCreate + pModif + pDelete + pUsed + pCoedited + pDeleted) / 6;
			unsortedAverageUserMetrics.put(user.getId(), avg);

			// Weighted average indicator per contributor
			// Double weightedAvg = (pCreate + 2 * pModif + 2 * pDelete + 3 *
			// pUsed + pCoedited - pDeleted) / 10;
			// unsortedWeightedAverageUserMetrics.put(user.getId(),
			// weightedAvg);

			Double weightedAvg = (pCreate + 3 * pModif + 3 * pDelete + 3 * pUsed + pCoedited + pDelete) / 12;
			unsortedWeightedAverageUserMetrics.put(user.getId(), weightedAvg);
		}

		/****************************
		 * Comparaison des métriques
		 *****************************/
		// Classement des contributeurs
		System.out.println("Classement des contributeurs (total : " + myContributors.size() + " contributors) :");
		Set<Integer> classementParContributions = CollectionsUtil.sortByValueDescending(unsortedNbContributions)
				.keySet();
		Set<Integer> classementMoyenneMetriques = CollectionsUtil.sortByValueDescending(unsortedAverageUserMetrics)
				.keySet();
		Set<Integer> classementMoyennePondereeMetriques = CollectionsUtil
				.sortByValueDescending(unsortedWeightedAverageUserMetrics).keySet();

		List<Integer> usersRangedByContributions = new ArrayList<Integer>();
		usersRangedByContributions.addAll(classementParContributions);

		List<Integer> usersRangedByAvgMetrics = new ArrayList<Integer>();
		usersRangedByAvgMetrics.addAll(classementMoyenneMetriques);

		List<Integer> usersRangedByWeightedAvgMetrics = new ArrayList<Integer>();
		usersRangedByWeightedAvgMetrics.addAll(classementMoyennePondereeMetriques);

		System.out.println();

		/****************************
		 * Classement avec Promethee
		 ****************************/

		UserMetricsPromethee rankingPromethee = new UserMetricsPromethee();
		List<Integer> usersRangedByPromethee = rankingPromethee.computeRanking(myContributors, coeditg, useg,
				suppressiong);

		/**********************
		 * Hamming distance
		 *********************/

		int hammingDistContribution_Avg = CollectionsUtil.getHammingDistance2Lists(usersRangedByContributions,
				usersRangedByAvgMetrics);

		int hammingDistContribution_WeightedAvg = CollectionsUtil.getHammingDistance2Lists(usersRangedByContributions,
				usersRangedByWeightedAvgMetrics);

		int hammingdistAvg_WeightedAvg = CollectionsUtil.getHammingDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByWeightedAvgMetrics);

		int hammingDistContribution_Promethee = CollectionsUtil.getHammingDistance2Lists(usersRangedByContributions,
				usersRangedByPromethee);
		int hammingDistAvg_Promethee = CollectionsUtil.getHammingDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByPromethee);
		int hammingDistWeightedAvg_Promethee = CollectionsUtil.getHammingDistance2Lists(usersRangedByWeightedAvgMetrics,
				usersRangedByPromethee);

		System.out.println("Hamming distance nb contributions/ avg metrics: " + hammingDistContribution_Avg);
		System.out.println(
				"Hamming distance nb contributions/ weighted avg metrics: " + hammingDistContribution_WeightedAvg);
		System.out.println("Hamming distance avg metrics / weighted avg metrics: " + hammingdistAvg_WeightedAvg);
		System.out.println();

		System.out.println("Hamming distance nb contributions / Promethee: " + hammingDistContribution_Promethee);
		System.out.println("Hamming distance avg metrics / Promethee: " + hammingDistAvg_Promethee);
		System.out.println("Hamming weighted avg metrics / Promethee: " + hammingDistWeightedAvg_Promethee);
		System.out.println();

		/*****************
		 * Edit distance
		 ****************/

		int editDistContribution_Avg = CollectionsUtil.getEditDistance2Lists(usersRangedByContributions,
				usersRangedByAvgMetrics);

		int editDistContribution_WeightedAvg = CollectionsUtil.getEditDistance2Lists(usersRangedByContributions,
				usersRangedByWeightedAvgMetrics);

		int editdistAvg_WeightedAvg = CollectionsUtil.getEditDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByWeightedAvgMetrics);

		int editDistContribution_Promethee = CollectionsUtil.getEditDistance2Lists(usersRangedByContributions,
				usersRangedByPromethee);

		int editDistAvg_Promethee = CollectionsUtil.getEditDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByPromethee);

		int editdistWeightedAvg_Promethee = CollectionsUtil.getEditDistance2Lists(usersRangedByWeightedAvgMetrics,
				usersRangedByPromethee);

		System.out.println("Edit distance nb contributions/ avg metrics: " + editDistContribution_Avg);
		System.out.println("Edit distance nb contributions/ weighted avg metrics: " + editDistContribution_WeightedAvg);
		System.out.println("Edit distance avg metrics / weighted avg metrics: " + editdistAvg_WeightedAvg);
		System.out.println();
		System.out.println("Edit distance nb contributions / Promethee: " + editDistContribution_Promethee);
		System.out.println("Edit distance avg metrics / Promethee: " + editDistAvg_Promethee);
		System.out.println("Edit weighted avg metrics / Promethee: " + editdistWeightedAvg_Promethee);
		System.out.println();

		/********************************
		 * Jaro Winkler Distance
		 ********************************/

		Double JWDistContribution_Avg = CollectionsUtil.getJaroWinklerDistance2Lists(usersRangedByContributions,
				usersRangedByAvgMetrics, (double) 0.01);

		Double JWDistContribution_WeightedAvg = CollectionsUtil.getJaroWinklerDistance2Lists(usersRangedByContributions,
				usersRangedByWeightedAvgMetrics, (double) 0.01);

		Double JWdistAvg_WeightedAvg = CollectionsUtil.getJaroWinklerDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByWeightedAvgMetrics, (double) 0.01);

		Double JWdistContribution_Promethee = CollectionsUtil.getJaroWinklerDistance2Lists(usersRangedByContributions,
				usersRangedByPromethee, (double) 0.01);

		Double JWdistAvg_Promethee = CollectionsUtil.getJaroWinklerDistance2Lists(usersRangedByAvgMetrics,
				usersRangedByPromethee, (double) 0.01);

		Double JWdistWeightedAvg_Promethee = CollectionsUtil
				.getJaroWinklerDistance2Lists(usersRangedByWeightedAvgMetrics, usersRangedByPromethee, (double) 0.01);

		System.out.println("Jaro Winkler distance nb contributions/ avg metrics: " + JWDistContribution_Avg);
		System.out.println(
				"Jaro Winkler distance nb contributions/ weighted avg metrics: " + JWDistContribution_WeightedAvg);
		System.out.println("Jaro Winkler distance avg metrics / weighted avg metrics: " + JWdistAvg_WeightedAvg);
		System.out.println();
		System.out.println("Jaro Winkler distance nb contributions / Promethee: " + JWdistContribution_Promethee);
		System.out.println("Jaro Winkler distance avg metrics / Promethee: " + JWdistAvg_Promethee);
		System.out.println("Jaro Winkler weighted avg metrics / Promethee: " + JWdistWeightedAvg_Promethee);
		System.out.println();

		/*******************************************************************************
		 * Calcul de la cardinalité de l'intersection entre chaque morceau de
		 * classement
		 *******************************************************************************/
		// On divise les classements en 3 parts égales
		// Classement par nombre de contributions
		List<Integer> usersRangedByContributions1OutOf3 = usersRangedByContributions.subList(0, 63);
		List<Integer> usersRangedByContributions2OutOf3 = usersRangedByContributions.subList(63, 126);
		List<Integer> usersRangedByContributions3OutOf3 = usersRangedByContributions.subList(126, 189);
		System.out.println("Taille de usersRangedByContributions3OutOf3 : " + usersRangedByContributions3OutOf3.size());

		// Classement par moyenne des 6 métriques
		List<Integer> usersRangedByAvgMetrics1OutOf3 = usersRangedByAvgMetrics.subList(0, 63);
		List<Integer> usersRangedByAvgMetrics2OutOf3 = usersRangedByAvgMetrics.subList(63, 126);
		List<Integer> usersRangedByAvgMetrics3OutOf3 = usersRangedByAvgMetrics.subList(126, 189);

		// Classement par moyenne pondérée des 6 métriques
		List<Integer> usersRangedByWeightedAvgMetrics1OutOf3 = usersRangedByWeightedAvgMetrics.subList(0, 63);
		List<Integer> usersRangedByWeightedAvgMetrics2OutOf3 = usersRangedByWeightedAvgMetrics.subList(63, 126);
		List<Integer> usersRangedByWeightedAvgMetrics3OutOf3 = usersRangedByWeightedAvgMetrics.subList(126, 189);

		// Nombre de contributions / Moyenne des 6 métriques
		System.out.println("Nombre de contributions / Moyenne des 6 métriques");

		System.out.println("Users ranged by contributions : " + usersRangedByContributions.size());
		List<Integer> nbContribIntersectionAvg_1OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionAvg_2OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionAvg_3OutOf3 = new ArrayList<Integer>();

		nbContribIntersectionAvg_1OutOf3.addAll(usersRangedByContributions1OutOf3);
		nbContribIntersectionAvg_2OutOf3.addAll(usersRangedByContributions2OutOf3);
		nbContribIntersectionAvg_3OutOf3.addAll(usersRangedByContributions3OutOf3);

		nbContribIntersectionAvg_1OutOf3.retainAll(usersRangedByAvgMetrics1OutOf3);
		System.out.println("1/3 Recouvrement : " + nbContribIntersectionAvg_1OutOf3.size() + "/"
				+ usersRangedByContributions1OutOf3.size());

		nbContribIntersectionAvg_2OutOf3.retainAll(usersRangedByAvgMetrics2OutOf3);
		System.out.println("2/3 Recouvrement : " + nbContribIntersectionAvg_2OutOf3.size() + "/"
				+ usersRangedByContributions2OutOf3.size());

		nbContribIntersectionAvg_3OutOf3.retainAll(usersRangedByAvgMetrics3OutOf3);
		System.out.println("3/3 Recouvrement : " + nbContribIntersectionAvg_3OutOf3.size() + "/"
				+ usersRangedByContributions3OutOf3.size());

		// Nombre de contributions / Moyenne pondérée des 6 métriques
		System.out.println("Nombre de contributions / Moyenne pondérée des 6 métriques");

		List<Integer> nbContribIntersectionWeightedAvg_1OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionWeightedAvg_2OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionWeightedAvg_3OutOf3 = new ArrayList<Integer>();

		nbContribIntersectionWeightedAvg_1OutOf3.addAll(usersRangedByContributions1OutOf3);
		nbContribIntersectionWeightedAvg_2OutOf3.addAll(usersRangedByContributions2OutOf3);
		nbContribIntersectionWeightedAvg_3OutOf3.addAll(usersRangedByContributions3OutOf3);

		nbContribIntersectionWeightedAvg_1OutOf3.retainAll(usersRangedByWeightedAvgMetrics1OutOf3);
		System.out.println("1/3 Recouvrement : " + nbContribIntersectionWeightedAvg_1OutOf3.size() + "/"
				+ usersRangedByAvgMetrics1OutOf3.size());
		nbContribIntersectionWeightedAvg_2OutOf3.retainAll(usersRangedByWeightedAvgMetrics2OutOf3);
		System.out.println("2/3 Recouvrement : " + nbContribIntersectionWeightedAvg_2OutOf3.size() + "/"
				+ usersRangedByAvgMetrics2OutOf3.size());

		nbContribIntersectionWeightedAvg_3OutOf3.retainAll(usersRangedByWeightedAvgMetrics3OutOf3);
		System.out.println("3/3 Recouvrement : " + nbContribIntersectionWeightedAvg_3OutOf3.size() + "/"
				+ usersRangedByAvgMetrics3OutOf3.size());

		// Moyenne pondérée / Moyenne pondérée des 6 métriques
		System.out.println("Moyenne pondérée / Moyenne pondérée des 6 métriques");

		List<Integer> avgIntersectionWeightedAvg_1OutOf3 = new ArrayList<Integer>();
		List<Integer> avgIntersectionWeightedAvg_2OutOf3 = new ArrayList<Integer>();
		List<Integer> avgIntersectionWeightedAvg_3OutOf3 = new ArrayList<Integer>();

		avgIntersectionWeightedAvg_1OutOf3.addAll(usersRangedByAvgMetrics1OutOf3);
		avgIntersectionWeightedAvg_2OutOf3.addAll(usersRangedByAvgMetrics2OutOf3);
		avgIntersectionWeightedAvg_3OutOf3.addAll(usersRangedByAvgMetrics3OutOf3);

		avgIntersectionWeightedAvg_1OutOf3.retainAll(usersRangedByWeightedAvgMetrics1OutOf3);
		System.out.println("1/3 Recouvrement : " + avgIntersectionWeightedAvg_1OutOf3.size() + "/"
				+ usersRangedByAvgMetrics1OutOf3.size());

		avgIntersectionWeightedAvg_2OutOf3.retainAll(usersRangedByWeightedAvgMetrics2OutOf3);
		System.out.println("2/3 Recouvrement : " + avgIntersectionWeightedAvg_2OutOf3.size() + "/"
				+ usersRangedByAvgMetrics2OutOf3.size());

		avgIntersectionWeightedAvg_3OutOf3.retainAll(usersRangedByWeightedAvgMetrics3OutOf3);
		System.out.println("3/3 Recouvrement " + avgIntersectionWeightedAvg_3OutOf3.size() + "/"
				+ usersRangedByAvgMetrics3OutOf3.size());

		// Classement PROMETHEE
		List<Integer> usersRangedByPromethee1OutOf3 = usersRangedByPromethee.subList(0, 20);
		List<Integer> usersRangedByPromethee2OutOf3 = usersRangedByPromethee.subList(20, 170);
		List<Integer> usersRangedByPromethee3OutOf3 = usersRangedByPromethee.subList(170, 189);

		// PROMETHEE & nb contributions
		List<Integer> nbContribIntersectionPromethee_1OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionPromethee_2OutOf3 = new ArrayList<Integer>();
		List<Integer> nbContribIntersectionPromethee_3OutOf3 = new ArrayList<Integer>();

		nbContribIntersectionPromethee_1OutOf3.addAll(usersRangedByContributions1OutOf3);
		nbContribIntersectionPromethee_2OutOf3.addAll(usersRangedByContributions2OutOf3);
		nbContribIntersectionPromethee_3OutOf3.addAll(usersRangedByContributions3OutOf3);

		System.out.println("Nombre de contributions / PROMETHEE");
		nbContribIntersectionPromethee_1OutOf3.retainAll(usersRangedByPromethee1OutOf3);
		System.out.println("1/3 Recouvrement : " + nbContribIntersectionAvg_1OutOf3.size() + "/"
				+ usersRangedByContributions1OutOf3.size());

		nbContribIntersectionPromethee_2OutOf3.retainAll(usersRangedByPromethee2OutOf3);
		System.out.println("2/3 Recouvrement : " + nbContribIntersectionAvg_2OutOf3.size() + "/"
				+ usersRangedByContributions2OutOf3.size());

		nbContribIntersectionPromethee_3OutOf3.retainAll(usersRangedByPromethee3OutOf3);
		System.out.println("3/3 Recouvrement : " + nbContribIntersectionAvg_3OutOf3.size() + "/"
				+ usersRangedByContributions3OutOf3.size());

		// PROMETHEE & avg user metrics
		List<Integer> avgIntersectionPromethee_1OutOf3 = new ArrayList<Integer>();
		List<Integer> avgIntersectionPromethee_2OutOf3 = new ArrayList<Integer>();
		List<Integer> avgIntersectionPromethee_3OutOf3 = new ArrayList<Integer>();
		//
		avgIntersectionPromethee_1OutOf3.addAll(usersRangedByAvgMetrics1OutOf3);
		avgIntersectionPromethee_2OutOf3.addAll(usersRangedByAvgMetrics2OutOf3);
		avgIntersectionPromethee_3OutOf3.addAll(usersRangedByAvgMetrics3OutOf3);

		System.out.println("PROMETHEE / Moyenne des métriques");

		avgIntersectionPromethee_1OutOf3.retainAll(usersRangedByPromethee1OutOf3);
		System.out.println("1/3 Recouvrement : " + avgIntersectionPromethee_1OutOf3.size() + "/"
				+ usersRangedByContributions1OutOf3.size());

		avgIntersectionPromethee_2OutOf3.retainAll(usersRangedByPromethee2OutOf3);
		System.out.println("2/3 Recouvrement : " + avgIntersectionPromethee_2OutOf3.size() + "/"
				+ usersRangedByContributions2OutOf3.size());

		avgIntersectionPromethee_3OutOf3.retainAll(usersRangedByPromethee3OutOf3);
		System.out.println("3/3 Recouvrement : " + avgIntersectionPromethee_3OutOf3.size() + "/"
				+ usersRangedByContributions3OutOf3.size());

		// PROMETHEE & weighted avg user metrics
		List<Integer> weightedAvgIntersectionPromethee_1OutOf3 = new ArrayList<Integer>();
		List<Integer> weightedAvgIntersectionPromethee_2OutOf3 = new ArrayList<Integer>();
		List<Integer> weightedAvgIntersectionPromethee_3OutOf3 = new ArrayList<Integer>();

		weightedAvgIntersectionPromethee_1OutOf3.addAll(usersRangedByWeightedAvgMetrics1OutOf3);
		weightedAvgIntersectionPromethee_2OutOf3.addAll(usersRangedByWeightedAvgMetrics2OutOf3);
		weightedAvgIntersectionPromethee_3OutOf3.addAll(usersRangedByWeightedAvgMetrics3OutOf3);

		System.out.println("PROMETHEE / Moyenne pondérée des métriques");

		weightedAvgIntersectionPromethee_1OutOf3.retainAll(usersRangedByPromethee1OutOf3);
		System.out.println("1/3 Recouvrement : " + weightedAvgIntersectionPromethee_1OutOf3.size() + "/"
				+ usersRangedByContributions1OutOf3.size());
		//
		weightedAvgIntersectionPromethee_2OutOf3.retainAll(usersRangedByPromethee2OutOf3);
		System.out.println("2/3 Recouvrement : " + weightedAvgIntersectionPromethee_2OutOf3.size() + "/"
				+ usersRangedByContributions2OutOf3.size());
		//
		weightedAvgIntersectionPromethee_3OutOf3.retainAll(usersRangedByPromethee3OutOf3);
		System.out.println("3/3 Recouvrement : " + weightedAvgIntersectionPromethee_3OutOf3.size() + "/"
				+ usersRangedByContributions3OutOf3.size());

		/***************************************
		 * Suivi de contributeurs particuliers
		 ***************************************/
		// Grands contributeurs
		final int GC1 = 210173;
		final int GC2 = 3620638;
		final int GC3 = 83557;
		final int GC4 = 160042;
		final int GC5 = 705842;

		// Contributeurs moyens
		final int MC1 = 24207;
		final int MC2 = 6236830;

		// Petits contributeurs
		final int PC1 = 940533;
		final int PC2 = 3083733;

		System.out.println("Classement contributeur #210173 (GC1) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(GC1));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(GC1));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(GC1));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(GC1));

		System.out.println("Classement contributeur #3620638 (GC2) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(GC2));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(GC2));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(GC2));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(GC2));

		System.out.println("Classement contributeur #3620638 (GC3) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(GC3));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(GC3));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(GC3));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(GC3));

		System.out.println("Classement contributeur #3620638 (GC4) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(GC4));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(GC4));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(GC4));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(GC4));

		System.out.println("Classement contributeur #3620638 (GC5) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(GC5));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(GC5));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(GC5));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(GC5));

		System.out.println("Classement contributeur #208829 (MC1) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(MC1));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(MC1));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(MC1));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(MC1));

		System.out.println("Classement contributeur #6236830 (MC2) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(MC2));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(MC2));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(MC2));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(MC2));

		System.out.println("Classement contributeur #210173 (PC1) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(PC1));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(PC1));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(PC1));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(PC1));

		System.out.println("Classement contributeur #3083733 (PC2) ");
		System.out.println("Rang M1 = " + usersRangedByContributions.indexOf(PC2));
		System.out.println("Rang M2 = " + usersRangedByAvgMetrics.indexOf(PC2));
		System.out.println("Rang M3 = " + usersRangedByWeightedAvgMetrics.indexOf(PC2));
		System.out.println("Rang Promethee = " + usersRangedByPromethee.indexOf(PC2));

		Map<Integer, Double> netflows = rankingPromethee.getCandidatesNetFlows(myContributors, coeditg, useg,
				suppressiong);
		System.out.println("Net Flows : ");
		System.out.println("GC1 :" + netflows.get(GC1));
		System.out.println("GC2 :" + netflows.get(GC2));
		System.out.println("GC3 :" + netflows.get(GC3));
		System.out.println("GC4 :" + netflows.get(GC4));
		System.out.println("GC5 :" + netflows.get(GC5));
		System.out.println("MC1 :" + netflows.get(MC1));
		System.out.println("MC2 :" + netflows.get(MC2));
		System.out.println("PC1 :" + netflows.get(PC1));
		System.out.println("PC2 :" + netflows.get(PC2));

		System.out.println("Nombre de contributeurs à 1 contribution : " + nbContrib1);

		for (OSMDefaultFeature r : myContributors.get(Long.valueOf(GC5)).getContributions()) {
			System.out.println("contributions GC5 " + r.getId() + " - Géométrie : " + r.getGeom().toString());
		}
		for (OSMDefaultFeature r : myContributors.get(Long.valueOf(MC2)).getContributions()) {
			System.out.println("contributions MC2 " + r.getId() + " - Géométrie : " + r.getGeom().toString());
		}
		for (OSMDefaultFeature r : myContributors.get(Long.valueOf(PC1)).getContributions()) {
			System.out.println("contributions PC1 " + r.getId() + " - Géométrie : " + r.getGeom().toString());
		}
		for (OSMDefaultFeature r : myContributors.get(Long.valueOf(PC2)).getContributions()) {
			System.out.println("contributions PC2 " + r.getId() + " - Géométrie : " + r.getGeom().toString());
		}

	}

}
