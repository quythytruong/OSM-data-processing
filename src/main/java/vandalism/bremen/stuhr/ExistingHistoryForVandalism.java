package vandalism.bremen.stuhr;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class ExistingHistoryForVandalism {
	public static void main(String[] args) throws SQLException {
		LoadFromPostGIS ld = new LoadFromPostGIS("localhost", "5432", "bremen", "postgres", "postgres");
		String select = "SELECT id, v_contrib FROM indicators.stuhr_v WHERE uid = 7000000";
		ResultSet r = ld.executeQuery(select);

		// Récupération des batis de type relation
		Map<Long, Integer> idExistant = new HashMap<Long, Integer>();
		while (r.next())
			idExistant.put(r.getLong("id"), r.getInt("v_contrib"));

		// Reconstruction de l'historique de chaque objet existant
		for (Long id : idExistant.keySet()) {
			OSMObject histo = new OSMObject(id);
			for (int v = 1; v < idExistant.get(id); v++) {
				OSMResource resource = ld.getWayFromAPI(id, String.valueOf(v));
				histo.addcontribution(resource);
			}
			System.out.println("OSMobject nb version : " + histo.getContributions().size());
			// Calcul des métriques historiques
			// int nbVersions = OSMObjectAssessment.getNbVersions(histo);
			// int nbUsers = OSMObjectAssessment.getNbContributors(histo);
			// int nbDeletes = OSMObjectAssessment.getNbDelete(histo);
			// int nbGeomEdit = OSMObjectAssessment.getNbGeomEdition(histo);
			// int nbTagEdit = OSMObjectAssessment.getNbTagEdition(histo);
			// int nbTagAdd = OSMObjectAssessment.getNbTagAddition(histo);
			// int nbTagDel = OSMObjectAssessment.getNbTagDelete(histo);
			//
			// String update = "UPDATE indicators.stuhr_v SET n_versions= " +
			// nbVersions + ", n_users = " + nbUsers
			// + ", n_del = " + nbDeletes + ", n_geom_edit = " + nbGeomEdit + ",
			// n_tag_edit = " + nbTagEdit
			// + ", n_tag_add = " + nbTagAdd + ", n_tag_del = " + nbTagDel + "
			// WHERE id = " + id + ";";

			// Calcule la durée écoulée entre la dernière version et le
			// vandalisme. On fixe la date du vandalisme au 7 décembre 2017
			OSMResource latest = OSMObjectAssessment.getVmax(histo);
			System.out.println("Contribution " + id + " last modif : " + latest.getDate());

			String update = "UPDATE indicators.stuhr_v SET timespan_to_previous = extract(epoch FROM ('2017-12-07 12:00:00+01')::timestamp - ('"
					+ latest.getDate() + "')::timestamp) WHERE id =" + id;
			System.out.println(update);
			ld.executeAnyQuery(update);
		}
	}

}
