package testdoc;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class RunLoadFromPostGIS {

	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "idf", "postgres", "postgres");
		OSMObject.dbName = "idf";
		String timestamp = "2016-12-03 03:34:21+01";
		// Double[] boundary = loader.getCityBoundary("Montfermeil", timestamp);
		// System.out.println(boundary.length);
		// System.out.println(boundary.toString());

		OSMResource relationTest = loader.getRelation((long) 5615175, timestamp);
		OSMResource nodeTest = loader.getNodeFromAPI((long) 1672178060, timestamp);
		loader.getEnvelope(nodeTest, timestamp);

		// System.out.println(loader.isInSpatioTemporalArea(relationTest,
		// boundary, timestamp));
		Double[] empriseRelation = loader.getEnvelope(relationTest, timestamp);
		for (Double d : empriseRelation)
			System.out.println(d);

		// Double[] boundaryAubervilliers =
		// loader.getCityBoundary("Aubervilliers", timestamp);
		// System.out.println(loader.isInSpatioTemporalArea(relationTest,
		// boundaryAubervilliers, timestamp));
	}

}
