package vandalism.building;

import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class Fougeres {
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS loader = new LoadFromPostGIS("localhost", "5432", "bretagne", "postgres", "postgres");
		// Get the city boundaries
		// Double[] bbox = loader.getCityBoundary("Fougères",
		// "2018-02-13T23:59:59Z");
		// String[] timespan = { "2000-01-01", "2018-02-13T23:59:59Z" };
		// Set<OSMResource> ways = loader.getEvolutionWay(bbox, timespan);
		// loader.myJavaObjects.addAll(ways);
		// System.out.println("Size Myjavaobjects :" +
		// loader.myJavaObjects.size());

		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "bretagne", "postgres", "postgres");
		b.loadBuildings("Fougères", "2018-02-13T23:59:59Z");
		String epsg = "2154";
		Map<Long, IFeature> geometries = b.buildGeometry(epsg);
		System.out.println(geometries.size());

	}
}
