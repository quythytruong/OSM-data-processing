package vandalism.thimphu;

import java.util.Collection;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.geometrie.IndicesForme;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.util.algo.CommonAlgorithms;
import fr.ign.cogit.geoxygene.util.algo.SmallestSurroundingRectangleComputation;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;

public class Thimphu {
	public static void main(String[] args) throws Exception {
		// run
		// first
		// pgScript
		// "relation_boundary.sql"
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "bhutan", "postgres", "postgres");
		b.loadBuildings("Thimphu", "2019-03-19T23:59:59Z");
		String epsg = "5266";
		Map<Long, IFeature> geometries = b.buildGeometry(epsg);

		// b.addID2Postgres("Thimphu", geometries.keySet());
		StringBuffer query = new StringBuffer();

		int it = 0;
		for (IFeature feat : geometries.values()) {
			if (feat == null)
				continue;
			// Metadonnees
			int version = b.buildings.get(Long.valueOf(feat.getId())).getVersion();
			int nbTags = b.buildings.get(Long.valueOf(feat.getId())).getTags().size();
			int uid = b.buildings.get(Long.valueOf(feat.getId())).getUid();

			// Indicateurs géométriques intrinsèques
			Double perimeter = feat.getGeom().length();
			Double area = feat.getGeom().area();

			Double shortestEdge = CommonAlgorithmsFromCartAGen.getShortestEdgeLength(feat.getGeom());
			Double medianEdge = CommonAlgorithmsFromCartAGen.getEdgeLengthMedian(feat.getGeom());

			Double elongation = CommonAlgorithms.elongation(feat.getGeom());
			Double convexity = CommonAlgorithms.convexity(feat.getGeom());
			Double compacite = IndicesForme.indiceCompacite(((IPolygon) feat.getGeom()));

			Double areaSSR = 0.0;
			if (SmallestSurroundingRectangleComputation.getSSR(feat.getGeom()) != null)
				areaSSR = SmallestSurroundingRectangleComputation.getSSR(feat.getGeom()).area();

			// Indicateurs topologiques
			// Mesure la dimension des éventuelles intersections avec d'autres
			// bâtis
			int batiInterBati_0D = 0, batiInterBati_1D = 0, batiInterBati_2D = 0;
			for (IFeature bati : geometries.values()) {
				if (bati.getId() == feat.getId())
					continue;
				IGeometry intersection = feat.getGeom().intersection(bati.getGeom());
				if (intersection != null) {
					if (intersection.dimension() == 0)
						batiInterBati_0D++;
					else if (intersection.dimension() == 1)
						batiInterBati_1D++;
					else if (intersection.dimension() == 2)
						batiInterBati_2D++;
				}
			}

			query.append("UPDATE indicators.thimphu SET v_contrib =" + version + ", uid = " + uid);
			// Indicateurs topologiques bati/bati
			query.append(", n_tags =" + nbTags + ",  perimeter=" + perimeter + ", area =" + area + ", shortest_length ="
					+ shortestEdge + ", median_length =" + medianEdge + ", elongation=" + elongation + ", convexity="
					+ convexity + ", compacity=" + compacite + ", area_mbr=" + areaSSR + ", n_inter_bati_0d = "
					+ batiInterBati_0D + ", n_inter_bati_1d=" + batiInterBati_1D + ", n_inter_bati_2d="
					+ batiInterBati_2D + " WHERE id = " + feat.getId() + ";");

			it++;
			if (it == 1000) {
				System.out.println(query.toString());
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}
		}
		// Pour charger les dernières lignes qui n'ont pas été ajoutées à la
		// base de données
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
		it = 0;

		// LULC
		Collection<IFeature> lulc = b.getLULC(epsg).values();
		for (Long bati : geometries.keySet()) {
			IFeature feat = geometries.get(bati);
			int nbIntersects = 0;
			int nbWithin = 0;
			for (IFeature land : lulc) {
				if (land == null)
					continue;
				if (feat.getGeom().intersectsStrictement(land.getGeom())) {
					nbIntersects++;
				}
				if (feat.getGeom().within(land.getGeom()))
					nbWithin++;
			}
			query.append("UPDATE indicators.thimphu SET n_inter_lulc=" + nbIntersects + ", n_is_within_lulc=" + nbWithin
					+ " WHERE id = " + feat.getId() + ";");
			it++;
			if (it == 1000) {
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}
		}
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
		it = 0;

		// Road intersection
		Collection<IFeature> roads = b.getRoads(epsg).values();
		for (Long bati : geometries.keySet()) {
			IFeature feat = geometries.get(bati);
			int nbIntersects = 0;
			for (IFeature road : roads) {
				if (road == null)
					continue;

				if (feat.getGeom().intersects(road.getGeom())) {
					nbIntersects++;
				}
				query.append("UPDATE indicators.thimphu SET n_inter_route=" + nbIntersects + " WHERE id = "
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
