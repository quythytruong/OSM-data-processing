package vandalism.building;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class AddHistoryMetrics {
	/***
	 * Modifier le nom de la ville au besoin
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LoadFromPostGIS ld = new LoadFromPostGIS("localhost", "5432", "bretagne", "postgres", "postgres");

		String select = "SELECT a.*, hstore_to_json(a.tags) FROM way a, indicators.fougeres b "
				+ "WHERE b.id = a.id AND b.v_contrib = a.vway AND b.is_way IS TRUE";
		ld.selectFromDB(select, "way");

		OSMObject.dbName = "bretagne";

		StringBuffer query = new StringBuffer();
		int it = 0;
		// History indicators
		for (OSMResource r : ld.myJavaObjects) {
			if (r == null)
				continue;
			OSMObject obj = OSMObject.makeFromHistory(r.getId(), r.getVersion(), "way");

			int nbVersions = OSMObjectAssessment.getNbVersions(obj);
			int nbUsers = OSMObjectAssessment.getNbContributors(obj);
			int nbDeletes = OSMObjectAssessment.getNbDelete(obj);
			int nbGeomEdit = OSMObjectAssessment.getNbGeomEdition(obj);
			int nbTagEdit = OSMObjectAssessment.getNbTagEdition(obj);
			int nbTagAdd = OSMObjectAssessment.getNbTagAddition(obj);
			int nbTagDel = OSMObjectAssessment.getNbTagDelete(obj);
			query.append("UPDATE indicators.fougeres SET n_versions=" + nbVersions + ", n_users=" + nbUsers + ", n_del="
					+ nbDeletes + ", n_geom_edit=" + nbGeomEdit + ", n_tag_edit=" + nbTagEdit + ", n_tag_add="
					+ nbTagAdd + ", n_tag_del=" + nbTagDel + " WHERE id = " + r.getId() + ";");
			it++;

			if (obj.getContributions().size() < 2)
				continue;
			OSMResource latest = OSMObjectAssessment.getVmax(obj);
			OSMResource previous = null;

			for (OSMResource r1 : obj.getContributions())
				if (r1.getVersion() == latest.getVersion() - 1) {
					previous = r1;
					break;
				}
			boolean isGeomEdit = OSMResourceQualityAssessment.isGeomEdition(latest, previous);
			boolean isTagCreation = OSMResourceQualityAssessment.isTagCreation(latest.getTags(), previous.getTags());
			boolean isTagModif = OSMResourceQualityAssessment.isTagModification(latest, previous);
			boolean isTagDel = OSMResourceQualityAssessment.isTagDelete(latest.getTags(), previous.getTags());
			query.append("UPDATE indicators.fougeres SET is_geom_edit_prev_version = " + isGeomEdit
					+ ", is_tag_edit_prev_version = " + isTagModif + ",is_tag_add_prev_version =" + isTagCreation
					+ ",is_tag_del_prev_version = " + isTagDel + " WHERE id = " + r.getId() + ";");

			it++;
			// if (it > 1000) {
			// System.out.println(query.toString());
			// ld.executeAnyQuery(query.toString());
			// query.setLength(0);
			// it = 0;
			//
			// }
		}
		ld.executeAnyQuery(query.toString());
		query.setLength(0);
		it = 0;

	}

}
