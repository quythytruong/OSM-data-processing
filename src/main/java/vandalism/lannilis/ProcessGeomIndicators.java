package vandalism.lannilis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.geometrie.IndicesForme;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.util.algo.CommonAlgorithms;
import fr.ign.cogit.geoxygene.util.algo.SmallestSurroundingRectangleComputation;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;

public class ProcessGeomIndicators {
	public static void main(String[] args) throws Exception {
		String url = "jdbc:postgresql://localhost:5432/bretagne";
		Connection conn = DriverManager.getConnection(url, "postgres", "postgres");
		System.out.println("Connexion réussie !");

		Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet r = s.executeQuery(
				"SELECT id, ST_AsText(ST_TRANSFORM(ST_GeometryN(geom, 1),2154)) as wkt FROM lannilis_vandalisme;");

		Map<Integer, IGeometry> vandalized = new HashMap<Integer, IGeometry>();
		StringBuffer query = new StringBuffer();
		query.append("INSERT INTO indicators.lannilis_v (id) VALUES ");

		// Stores the data in DB and in Java objects
		while (r.next()) {
			IGeometry geom = WktGeOxygene.makeGeOxygene(r.getString("wkt"));
			int id = r.getInt("id");
			vandalized.put(id, geom);
			query.append("(" + id + "),");
		}
		query.replace(query.length() - 1, query.length(), " ON CONFLICT DO NOTHING;");
		s.execute(query.toString());

		// Récupère les données OSM initiales
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "bretagne", "postgres", "postgres");
		b.loadBuildings("Lannilis", "2018-02-13T23:59:59Z");
		String epsg = "2154";
		// Bâtiments
		// Map<Long, IFeature> geometries = b.buildGeometry(epsg);
		// Elements naturels
		Collection<IFeature> lulc = b.getLULC(epsg).values();
		// Routes
		Collection<IFeature> roads = b.getRoads(epsg).values();

		for (int id : vandalized.keySet()) {
			query.setLength(0);
			System.out.println("id = " + id);
			IGeometry geom = vandalized.get(id);
			Double perimeter = geom.length();

			Double area = geom.area();

			Double shortestEdge = CommonAlgorithmsFromCartAGen.getShortestEdgeLength(geom);
			Double medianEdge = CommonAlgorithmsFromCartAGen.getEdgeLengthMedian(geom);

			Double elongation = CommonAlgorithms.elongation(geom);
			Double convexity = CommonAlgorithms.convexity(geom);
			Double compacite = IndicesForme.indiceCompacite(geom.mbRegion());
			Double areaSSR = SmallestSurroundingRectangleComputation.getSSR(geom).area();

			query.append("UPDATE indicators.lannilis_v SET perimeter=" + perimeter + ", area =" + area
					+ ", shortest_length =" + shortestEdge + ", median_length =" + medianEdge + ", elongation="
					+ elongation + ", convexity=" + convexity + ", compacity=" + compacite + ", area_mbr=" + areaSSR);

			int batiInterBati_0D = 0, batiInterBati_1D = 0, batiInterBati_2D = 0;

			// intersection bati/bati
			// for (IFeature bati : geometries.values()) {
			// if (bati.getId() == id)
			// continue;
			// IGeometry intersection = geom.intersection(bati.getGeom());
			// if (intersection == null)
			// continue;
			// if (!intersection.isEmpty()) {
			// if (intersection.dimension() == 0)
			// batiInterBati_0D++;
			// else if (intersection.dimension() == 1)
			// batiInterBati_1D++;
			// else if (intersection.dimension() == 2)
			// batiInterBati_2D++;
			// }
			// }
			query.append(", n_inter_bati_0d = " + batiInterBati_0D + ", n_inter_bati_1d=" + batiInterBati_1D
					+ ", n_inter_bati_2d=" + batiInterBati_2D);

			// intersection bati / lulc
			int nbIntersects = 0;
			int nbWithin = 0;
			for (IFeature land : lulc) {
				if (land == null)
					continue;
				if (geom.intersectsStrictement(land.getGeom())) {
					nbIntersects++;
				}
				if (geom.within(land.getGeom()))
					nbWithin++;
			}
			query.append(", n_inter_lulc=" + nbIntersects + ", n_is_within_lulc=" + nbWithin);

			// intersection bati / route
			nbIntersects = 0;
			for (IFeature road : roads) {
				if (road == null)
					continue;
				if (geom.intersects(road.getGeom())) {
					nbIntersects++;
				}

			}
			query.append(", n_inter_route=" + nbIntersects + " WHERE id = " + id);
			System.out.println(query.toString());
			s.execute(query.toString());

		}
	}
}
