package vandalism.bremen.stuhr;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;

public class AddHistoryMetrics {
	public static void main(String[] args) throws Exception {
		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "bremen", "postgres", "postgres");
		b.loadBuildings("Stuhr", "2018-02-13T23:59:59Z");

		StringBuffer query = new StringBuffer();

		int it = 0;
		// History indicators
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
			query.append("UPDATE indicators.stuhr SET n_versions_last_4_years=" + nbVersions + ", n_users_last_4_years="
					+ nbUsers + ", n_del=" + nbDeletes + ", n_geom_edit=" + nbGeomEdit + ", n_tag_edit=" + nbTagEdit
					+ ", n_tag_add=" + nbTagAdd + ", n_tag_del=" + nbTagDel + ", is_way =" + isWay + " WHERE id = "
					+ r.getId() + ";");
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
	}

}
