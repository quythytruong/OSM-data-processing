package idf.test_idc;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class TestFormerVersion {

	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		// Charge un objet
		String query = "SELECT node.idnode, node.id,node.uid,node.vnode, node.changeset, node.username, node.datemodif, hstore_to_json(node.tags), node.visible, node.lon, node.lat "
				+ " FROM node WHERE idnode = 21042619963;";
		loader.selectFromDB(query, "node");
		System.out.println(loader.myJavaObjects.size());
		OSMObject.dbName = "idf";
		OSMResourceQualityAssessment assess = new OSMResourceQualityAssessment(loader.myJavaObjects);
		assess.writeOSMObjectsDetails2CSV("test.csv");

	}

}
