package vandalism.aubervilliers;

import java.util.Collection;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;

public class UpdateLULC {
	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		b.loadBuildings("Aubervilliers", "2018-02-13T23:59:59Z");

		Map<Long, IFeature> geometries = b.buildGeometry("2154");
		StringBuffer query = new StringBuffer();
		int it = 0;

		// LULC
		Collection<IFeature> lulc = b.getLULC("2154").values();
		for (Long bati : geometries.keySet()) {
			IFeature feat = geometries.get(bati);
			int nbIntersects = 0;
			int nbWithin = 0;
			for (IFeature land : lulc) {
				try {
					// if (land == null)
					// continue;
					if (feat.getGeom().intersectsStrictement(land.getGeom())) {
						nbIntersects++;
					}
					if (feat.getGeom().within(land.getGeom()))
						nbWithin++;
				} catch (NullPointerException e) {
					continue;
				}
			}
			query.append("UPDATE indicators.aubervilliers SET n_inter_lulc=" + nbIntersects + ", n_is_within_lulc="
					+ nbWithin + " WHERE id = " + feat.getId() + ";");
			it++;
			if (it == 1000) {
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}

		}
		System.out.println(query.toString());
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
	}
}
