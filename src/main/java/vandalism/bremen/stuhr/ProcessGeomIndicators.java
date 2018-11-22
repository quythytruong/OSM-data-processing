package vandalism.bremen.stuhr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.spatial.geomroot.IGeometry;
import fr.ign.cogit.geoxygene.contrib.geometrie.IndicesForme;
import fr.ign.cogit.geoxygene.util.algo.CommonAlgorithms;
import fr.ign.cogit.geoxygene.util.algo.SmallestSurroundingRectangleComputation;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;
import fr.ign.cogit.geoxygene.util.conversion.ParseException;
import fr.ign.cogit.geoxygene.util.conversion.WktGeOxygene;

public class ProcessGeomIndicators {
	public static void main(String[] args) throws SQLException, ParseException {

		String url = "jdbc:postgresql://localhost:5432/bremen";
		Connection conn = DriverManager.getConnection(url, "postgres", "postgres");
		System.out.println("Connexion réussie !");

		Statement s = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet r = s.executeQuery("SELECT id, ST_AsText(geog) as wkt FROM vandalized_building_stuhr;");

		Map<Integer, IGeometry> vandalized = new HashMap<Integer, IGeometry>();
		while (r.next()) {
			IGeometry geom = WktGeOxygene.makeGeOxygene(r.getString("wkt"));
			vandalized.put(r.getInt("id"), geom);
		}
		s.close();
		conn.close();

		for (int id : vandalized.keySet()) {
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

			System.out.println("périmètre= " + perimeter);
			System.out.println("aire= " + area);
			System.out.println("shortestEdge= " + shortestEdge);
			System.out.println("medianEdge= " + medianEdge);
			System.out.println("elongation= " + elongation);
			System.out.println("convexity= " + convexity);
			System.out.println("compacite= " + compacite);
			System.out.println("areaSSR= " + areaSSR);

		}
	}
}
