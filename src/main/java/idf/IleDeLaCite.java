package idf;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class IleDeLaCite {
	/**
	 * Calcul d'indicateurs sur les contributions sur l'île de la cité entre
	 * 2013 et 2015
	 */
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		OSMObject.dbName = "idf";

		Double[] bbox = { 2.3322, 48.8489, 2.3634, 48.8627 }; // ile de la cité
		String[] timespan = { "2011-01-01", "2011-12-31" };

		loader.getDataFrombbox(bbox, timespan);
		System.out.println("Size Myjavaobjects :" + loader.myJavaObjects.size());
		OSMResourceQualityAssessment assess = new OSMResourceQualityAssessment(loader.myJavaObjects);
		assess.writeOSMObjectsDetails2CSV("idc_details_contributions_2011.csv");

		// HashMap<Long, OSMContributor> myContributors =
		// ContributorAssessment.contributorSummary(loader.myJavaObjects);
		// ContributorAssessment.writeContributorSummary(myContributors, new
		// File("idc_contributors_2013-2015.csv"));

		// Order by object
		// HashMap<Long, OSMObject> myObjects =
		// OSMResourceQualityAssessment.groupByOSMObject(loader.myJavaObjects);
		// System.out.println("Size myObjects " + myObjects.size());

	}

}
