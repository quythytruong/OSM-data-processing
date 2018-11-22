package vandalism.building;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.contrib.geometrie.IndicesForme;
import fr.ign.cogit.geoxygene.osm.NePasCommit.LoadOSMBuildings;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.util.algo.CommonAlgorithms;
import fr.ign.cogit.geoxygene.util.algo.SmallestSurroundingRectangleComputation;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;

public class GeometricIndicators {
	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		b.loadBuildings("Noisy-le-Grand", "2018-02-13T23:59:59Z");

		Map<Long, IFeature> geometries = b.buildGeometry("2154");

		Map<Long, Object[]> indicatorGeom = new HashMap<Long, Object[]>();
		for (IFeature feat : geometries.values()) {
			if (feat == null)
				continue;
			// Metadonnees
			int version = b.buildings.get(Long.valueOf(feat.getId())).getVersion();
			int nbTags = b.buildings.get(Long.valueOf(feat.getId())).getTags().size();

			String buildingValue = b.buildings.get(Long.valueOf(feat.getId())).getTags().get("building");
			boolean isYesBuilding = buildingValue.equalsIgnoreCase("yes");

			String source = b.buildings.get(Long.valueOf(feat.getId())).getSource();
			boolean isFromCadaster = false;
			if (source != null)
				isFromCadaster = (source.contains("cadas") || source.contains("Cadas"));

			int uid = b.buildings.get(Long.valueOf(feat.getId())).getUid() + 111; // Anonymisé

			// Indicateurs géométriques intrinsèques
			Double perimeter = feat.getGeom().length();
			Double area = feat.getGeom().area();

			Double shortestEdge = CommonAlgorithmsFromCartAGen.getShortestEdgeLength(feat.getGeom());
			Double medianEdge = CommonAlgorithmsFromCartAGen.getEdgeLengthMedian(feat.getGeom());

			Double elongation = CommonAlgorithms.elongation(feat.getGeom());
			Double convexity = CommonAlgorithms.convexity(feat.getGeom());
			Double compacite = IndicesForme.indiceCompacite(((IPolygon) feat.getGeom()));
			Double areaSSR = SmallestSurroundingRectangleComputation.getSSR(feat.getGeom()).area();

			// TODO: Indicateurs topologiques
			// Bati - Bati
			int nbIntersects = 0;
			int nbTouches = 0;
			int nbContains = 0;
			int nbWithin = 0;
			Double ratioIntersect = 0.0;
			Double ratioTouchLength = 0.0;
			Double sharedArea = 0.0;

			// System.out.println("Feature ID : " + Long.valueOf(feat.getId()));
			for (IFeature bati : geometries.values()) {
				if (bati.getId() == feat.getId())
					continue;
				if (feat.getGeom().intersectsStrictement(bati.getGeom())) {
					nbIntersects++;
					ratioIntersect += feat.getGeom().difference(bati.getGeom()).area() / feat.getGeom().area();
				}
				if (feat.getGeom().within(bati.getGeom()))
					nbWithin++;
				if (feat.getGeom().contains(bati.getGeom())) {
					nbContains++;
					sharedArea += bati.getGeom().area() / feat.getGeom().area();
				}
				if (feat.getGeom().touches(bati.getGeom())) {
					nbTouches++;
					ratioTouchLength += feat.getGeom().intersection(bati.getGeom()).length() / feat.getGeom().length();
				}
			}

			Object[] indicator = { version, uid, nbTags, isYesBuilding, isFromCadaster, perimeter, area, shortestEdge,
					medianEdge, elongation, convexity, compacite, areaSSR, nbIntersects, ratioIntersect, nbTouches,
					ratioTouchLength, nbContains, sharedArea, nbWithin };
			indicatorGeom.put(Long.valueOf(feat.getId()), indicator);
		}
		LoadOSMBuildings.toCSV(indicatorGeom, new File("indicateurs_geom_Noisy-le-Grand_PostGIS.csv"));

	}
}
