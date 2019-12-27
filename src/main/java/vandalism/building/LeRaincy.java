package vandalism.building;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IPolygon;
import fr.ign.cogit.geoxygene.contrib.geometrie.IndicesForme;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;
import fr.ign.cogit.geoxygene.util.algo.CommonAlgorithms;
import fr.ign.cogit.geoxygene.util.algo.SmallestSurroundingRectangleComputation;
import fr.ign.cogit.geoxygene.util.algo.geometricAlgorithms.CommonAlgorithmsFromCartAGen;

public class LeRaincy {

	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");
		b.loadBuildings("Le Raincy", "2018-02-13T23:59:59Z");

		b.addID2Postgres("le_raincy", b.buildings.keySet());

		Map<Long, IFeature> geometries = b.buildGeometry("2154");

		// b.addID2Postgres("le_raincy", geometries.keySet());
		StringBuffer query = new StringBuffer();

		int it = 0;
		for (IFeature feat : geometries.values()) {
			if (feat == null)
				continue;
			// Metadonnees
			int version = b.buildings.get(Long.valueOf(feat.getId())).getVersion();
			int nbTags = b.buildings.get(Long.valueOf(feat.getId())).getTags().size();
			if (nbTags == 0)
				continue;
			String buildingValue = b.buildings.get(Long.valueOf(feat.getId())).getTags().get("building");
			boolean isYesBuilding = buildingValue.equalsIgnoreCase("yes");

			String source = b.buildings.get(Long.valueOf(feat.getId())).getSource();
			boolean isFromCadaster = false;
			if (source != null)
				isFromCadaster = (source.contains("Cadas") || source.contains("cadas"));

			int uid = b.buildings.get(Long.valueOf(feat.getId())).getUid();

			if (!feat.hasGeom())
				continue;
			// Indicateurs géométriques intrinsèques
			Double perimeter = feat.getGeom().length();
			Double area = feat.getGeom().area();

			Double shortestEdge = CommonAlgorithmsFromCartAGen.getShortestEdgeLength(feat.getGeom());
			Double medianEdge = CommonAlgorithmsFromCartAGen.getEdgeLengthMedian(feat.getGeom());

			Double elongation = CommonAlgorithms.elongation(feat.getGeom());
			Double convexity = CommonAlgorithms.convexity(feat.getGeom());
			Double compacite = IndicesForme.indiceCompacite(((IPolygon) feat.getGeom()));
			Double areaSSR = SmallestSurroundingRectangleComputation.getSSR(feat.getGeom()).area();

			// Bati - Bati
			int nbIntersects = 0;
			int nbTouches = 0;
			int nbContains = 0;
			int nbWithin = 0;
			Double ratioIntersect = 0.0;
			Double ratioTouchLength = 0.0;
			Double sharedArea = 0.0;

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
			query.append("UPDATE indicators.le_raincy SET v_contrib =" + version + ", uid = " + uid + ", n_tags ="
					+ nbTags + ", building_is_yes=" + isYesBuilding + ", source_cadaster=" + isFromCadaster
					+ ", perimeter=" + perimeter + ", area =" + area + ", shortest_length =" + shortestEdge
					+ ", median_length =" + medianEdge + ", elongation=" + elongation + ", convexity=" + convexity
					+ ", compacity=" + compacite + ", area_mbr=" + areaSSR + ", n_intersects = " + nbIntersects
					+ ", r_intersects=" + ratioIntersect + ", n_touches=" + nbTouches + ", r_touches ="
					+ ratioTouchLength + ", n_contains =" + nbContains + ", r_contains=" + sharedArea
					+ ", n_is_within =" + nbWithin + " WHERE id = " + feat.getId() + ";");
			it++;
			if (it == 1000) {
				System.out.println(query.toString());
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}
		}
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
		it = 0;

		// LULC
		Collection<IFeature> lulc = b.getLULC("2154").values();
		for (Long bati : geometries.keySet()) {
			IFeature feat = geometries.get(bati);
			int nbIntersects = 0;
			int nbTouches = 0;
			int nbContains = 0;
			int nbWithin = 0;
			Double ratioIntersect = 0.0;
			Double ratioTouchLength = 0.0;
			Double sharedArea = 0.0;
			// System.out.println("Feature ID : " + Long.valueOf(feat.getId()));
			for (IFeature land : lulc) {
				if (land == null)
					continue;
				if (feat.getGeom().intersectsStrictement(land.getGeom())) {
					nbIntersects++;
					ratioIntersect += feat.getGeom().intersection(land.getGeom()).area() / feat.getGeom().area();
				}
				if (feat.getGeom().within(land.getGeom()))
					nbWithin++;
				if (feat.getGeom().contains(land.getGeom())) {
					nbContains++;
					sharedArea += land.getGeom().area() / feat.getGeom().area();
				}
				if (feat.getGeom().touches(land.getGeom())) {
					nbTouches++;
					ratioTouchLength += feat.getGeom().intersection(land.getGeom()).length() / feat.getGeom().length();
				}
			}
			query.append("UPDATE indicators.le_raincy SET n_intersects_lulc=" + nbIntersects + ", r_intersects_lulc="
					+ ratioIntersect + ", n_touches_lulc=" + nbTouches + ", r_touches_lulc=" + ratioTouchLength
					+ ", n_contains_lulc=" + nbContains + ", r_contains_lulc=" + sharedArea + ", n_is_within_lulc="
					+ nbWithin + " WHERE id = " + feat.getId() + ";");
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

		// Appariement
		for (Long id : geometries.keySet()) {
			if (!geometries.get(id).hasGeom())
				continue;
			Double min = b.minSurfaceDistanceFromMatching(geometries.get(id));
			String matchQuery = "UPDATE indicators.le_raincy " + "SET min_dist_surf_bati_bdtopo=" + min + " WHERE id = "
					+ id;
			b.updatePostgresIndicators(matchQuery);

		}

		for (OSMResource r : b.buildings.values()) {
			if (r == null)
				continue;
			OSMObject obj = b.getHistorySinceDate(r, "2014-02-13T23:59:59Z");
			boolean isWay = OSMObjectAssessment.getGeomPrimitiveName(obj).equals("OSMWay");
			int nbVersions = OSMObjectAssessment.getNbVersions(obj);
			int nbUsers = OSMObjectAssessment.getNbContributors(obj);
			int nbDeletes = OSMObjectAssessment.getNbDelete(obj);
			int nbGeomEdit = OSMObjectAssessment.getNbGeomEdition(obj);
			int nbTagEdit = OSMObjectAssessment.getNbTagEdition(obj);
			int nbTagAdd = OSMObjectAssessment.getNbTagAddition(obj);
			int nbTagDel = OSMObjectAssessment.getNbTagDelete(obj);
			query.append("UPDATE indicators.le_raincy SET n_versions_last_4_years=" + nbVersions + ", n_users="
					+ nbUsers + ", n_del=" + nbDeletes + ", n_geom_edit=" + nbGeomEdit + ", n_tag_edit=" + nbTagEdit
					+ ", n_tag_add=" + nbTagAdd + ", n_tag_del=" + nbTagDel + ", is_way =" + isWay + " WHERE id = "
					+ r.getId() + ";");
			it++;
			if (it == 1000) {
				// System.out.println(query.toString());
				b.updatePostgresIndicators(query.toString());
				query.setLength(0);
				it = 0;
			}
		}
		b.updatePostgresIndicators(query.toString());
		query.setLength(0);
		it = 0;

		// Contributeurs
		Map<Integer, Integer[]> contribIndicators = new HashMap<Integer, Integer[]>();
		Map<Integer, Set<Integer>> changesetByUID = new HashMap<Integer, Set<Integer>>();
		for (OSMResource r : b.buildings.values()) {
			if (r == null)
				continue;
			if (contribIndicators.get(r.getUid()) == null) {
				b.updateContributorInfos(r.getUid());
				Integer[] valuesInit = { 0, 0, 0, 0 };
				contribIndicators.put(r.getUid(), valuesInit);
				changesetByUID.put(r.getUid(), new HashSet<Integer>());
			}
			if (r.getVersion() == 1) {
				contribIndicators.get(r.getUid())[0]++;
				continue;
			}
			if (r.isVisible())
				contribIndicators.get(r.getUid())[1]++;
			else
				contribIndicators.get(r.getUid())[2]++;
			changesetByUID.get(r.getUid()).add(r.getChangeSet());
			contribIndicators.get(r.getUid())[3] = changesetByUID.get(r.getUid()).size();

		}
		for (Integer uid : contribIndicators.keySet()) {
			String update = "UPDATE indicators.le_raincy SET";
			update += " n_last_user_created=" + contribIndicators.get(uid)[0];
			update += ", n_last_user_modif=" + contribIndicators.get(uid)[1];
			update += ", n_last_user_del=" + contribIndicators.get(uid)[2];
			update += ", n_last_user_changesets=" + contribIndicators.get(uid)[3];
			update += " WHERE uid = " + uid;
			b.updatePostgresIndicators(update);
		}

	}
}
