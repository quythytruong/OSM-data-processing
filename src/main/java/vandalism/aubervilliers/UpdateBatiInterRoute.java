package vandalism.aubervilliers;

import java.util.Collection;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;

public class UpdateBatiInterRoute {
	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		b.loadBuildings("Aubervilliers", "2018-02-13T23:59:59Z");

		Map<Long, IFeature> geometries = b.buildGeometry("2154");
		StringBuffer query = new StringBuffer();
		int it = 0;

		// Road intersection
		Collection<IFeature> roads = b.getRoads("2154").values();
		for (Long bati : geometries.keySet()) {
			IFeature feat = geometries.get(bati);
			int nbIntersects = 0;
			for (IFeature road : roads) {
				if (road == null)
					continue;

				if (feat.getGeom().intersects(road.getGeom())) {
					nbIntersects++;
				}
				query.append("UPDATE indicators.aubervilliers SET n_inter_route=" + nbIntersects + " WHERE id = "
						+ feat.getId() + ";");
				it++;
				if (it == 1000) {
					b.updatePostgresIndicators(query.toString());
					query.setLength(0);
					it = 0;
				}
			}
		}
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
		it = 0;

	}
}
