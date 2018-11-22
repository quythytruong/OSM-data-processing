package vandalism.building;

import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;

public class UpdateBuildingMatching {
	public static void main(String[] args) throws Exception {
		BuildingAssessment batiAuberv = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		batiAuberv.loadBuildings("Aubervilliers", "2018-02-13T23:59:59Z");
		Map<Long, IFeature> geometries = batiAuberv.buildGeometry("2154");

		for (Long id : geometries.keySet()) {
			if (!geometries.get(id).hasGeom())
				continue;
			Double min = batiAuberv.minSurfaceDistanceFromMatching(geometries.get(id));
			// String matchQuery = "UPDATE
			// indicators.aubervilliers_all_with_vandalized_data "
			// + "SET min_dist_surf_bati_bdtopo=" + min + " WHERE id = " + id;
			String matchQuery = "UPDATE indicators.aubervilliers " + "SET min_dist_surf_bati_bdtopo=" + min
					+ " WHERE id = " + id;
			batiAuberv.updatePostgresIndicators(matchQuery);
		}
	}
}
