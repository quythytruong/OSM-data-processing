package vandalism.aubervilliers;

import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;

public class UpdateTopoMetrics {

	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		b.loadBuildings("Aubervilliers", "2018-02-13T23:59:59Z");

		Map<Long, IFeature> geometries = b.buildGeometry("2154");
		StringBuffer query = new StringBuffer();
		int it = 0;
		for (IFeature feat : geometries.values()) {
			if (feat == null)
				continue;
			// Indicateurs topologiques
			// Mesure la dimension des éventuelles intersections avec d'autres
			// bâtis
			int batiInterBati_0D = 0, batiInterBati_1D = 0, batiInterBati_2D = 0;
			for (IFeature bati : geometries.values()) {
				if (bati.getId() == feat.getId())
					continue;

				try {
					IGeometry intersection = feat.getGeom().intersection(bati.getGeom());
					if (!intersection.isEmpty()) { // lève une exception si
						// intersection vide
						if (intersection.dimension() == 0)
							batiInterBati_0D++;
						else if (intersection.dimension() == 1)
							batiInterBati_1D++;
						else if (intersection.dimension() == 2)
							batiInterBati_2D++;
					}
				} catch (NullPointerException e) {
					continue;
				}

			}
			query.append("UPDATE indicators.aubervilliers SET n_inter_bati_0d = " + batiInterBati_0D
					+ ", n_inter_bati_1d=" + batiInterBati_1D + ", n_inter_bati_2d=" + batiInterBati_2D + " WHERE id = "
					+ feat.getId() + ";");
			it++;
			if (it == 1000) {
				System.out.println(query.toString());
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}
		}

	}
}
