package vandalism.building;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.OsmRelationMember;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMResourceQualityAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class HistoryMetricsOSMRelation {

	public static void main(String[] args) throws Exception {
		// Reprend le calcul des métriques historiques pour le cas particulier
		// des relations
		LoadFromPostGIS ld = new LoadFromPostGIS("localhost", "5432", "bretagne", "postgres", "postgres");
		String select = "SELECT id, v_contrib FROM indicators.fougeres WHERE is_way IS FALSE;";
		ResultSet r = ld.executeQuery(select);

		// Récupération des batis de type relation
		Map<Long, Integer> idRelations = new HashMap<Long, Integer>();
		while (r.next())
			idRelations.put(r.getLong("id"), r.getInt("v_contrib"));

		// Reconstruction de l'historique de chaque objet relation (avec idrel)
		for (Long id : idRelations.keySet()) {
			OSMObject histo = new OSMObject(id);
			for (int v = 1; v <= idRelations.get(id); v++) {
				System.out.println("Id = " + id + " v." + v);
				// Récupère les membres des relations
				r = ld.executeQuery("SELECT * FROM relationmember WHERE idrel=" + id + "" + v);
				// NB: il faut préciser dbName
				OsmRelationMember.dbName = "bretagne";
				List<OsmRelationMember> members = OsmRelationMember.makeMember(id, v);

				// Récupère les infos de la table relation
				r = ld.executeQuery("SELECT *, hstore_to_json (tags) " + "FROM relation WHERE idrel = " + id + "" + v);
				while (r.next()) {
					OSMResource relation = ld.writeRelation(r, members);
					histo.getContributions().add(relation);
				}
			}
			System.out.println("OSMobject nb version : " + histo.getContributions().size());
			// Calcul des métriques historiques
			int nbVersions = OSMObjectAssessment.getNbVersions(histo);
			int nbUsers = OSMObjectAssessment.getNbContributors(histo);
			int nbDeletes = OSMObjectAssessment.getNbDelete(histo);
			int nbGeomEdit = OSMObjectAssessment.getNbGeomEdition(histo);
			int nbTagEdit = OSMObjectAssessment.getNbTagEdition(histo);
			int nbTagAdd = OSMObjectAssessment.getNbTagAddition(histo);
			int nbTagDel = OSMObjectAssessment.getNbTagDelete(histo);

			String update = "UPDATE indicators.fougeres SET n_versions=" + nbVersions + ", n_users=" + nbUsers
					+ ", n_del=" + nbDeletes + ", n_geom_edit=" + nbGeomEdit + ", n_tag_edit=" + nbTagEdit
					+ ", n_tag_add=" + nbTagAdd + ", n_tag_del=" + nbTagDel + " WHERE id = " + id + ";";
			System.out.println(update);
			ld.executeAnyQuery(update);

			if (histo.getContributions().size() < 2)
				continue;
			OSMResource latest = OSMObjectAssessment.getVmax(histo);
			OSMResource previous = null;

			for (OSMResource r1 : histo.getContributions())
				if (r1.getVersion() == latest.getVersion() - 1) {
					previous = r1;
					break;
				}
			boolean isGeomEdit = OSMResourceQualityAssessment.isGeomEdition(latest, previous);
			boolean isTagCreation = OSMResourceQualityAssessment.isTagCreation(latest.getTags(), previous.getTags());
			boolean isTagModif = OSMResourceQualityAssessment.isTagModification(latest, previous);
			boolean isTagDel = OSMResourceQualityAssessment.isTagDelete(latest.getTags(), previous.getTags());
			update = "UPDATE indicators.fougeres SET is_del_prev_version = " + isGeomEdit
					+ ", is_tag_edit_prev_version = " + isTagModif + ",is_tag_add_prev_version =" + isTagCreation
					+ ",is_tag_del_prev_version = " + isTagDel + " WHERE id = " + id + ";";
			ld.executeAnyQuery(update);

		}

	}

}
