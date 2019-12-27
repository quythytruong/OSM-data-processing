package testdoc;

import java.sql.SQLException;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class TestCertificateValidity {

	public static void main(String[] args) throws SQLException {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		OSMObject.dbName = "idf";
		String timestamp = "2016-12-03 03:34:21+01";
		// OSM API : request node history
		OSMResource nodeTest = loader.getNodeFromAPI((long) 1672178060, timestamp);
		System.out.println(nodeTest.toString());

		// OSM API : request way history
		OSMResource wayTest = loader.getWayFromAPI((long) 118916923, "1");
		System.out.println(wayTest.toString());

		// OSM API : request changeset details
		// String host, String port, String dbName, String dbUser, String dbPwd
		ChangesetRetriever chgstrtv = new ChangesetRetriever("localhost", "5432", "idf", "postgres", "postgres");
		System.out.println(chgstrtv.getChangesetValues((long) 64506118));
	}

}
