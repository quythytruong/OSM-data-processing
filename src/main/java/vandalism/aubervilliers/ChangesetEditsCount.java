package vandalism.aubervilliers;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

/**
 * Compte le nombre d'éditions par changeset et met à jour la table des
 * indicateurs
 * 
 * @author QTTruong
 *
 */
public class ChangesetEditsCount {

	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bretagne", "postgres", "postgres");

		// Cherche les changesets des données de Aubervilliers qui ne sont pas
		// encore stockés dans la base de données
		String query = "SELECT aubervilliers_building.id, changeset.changesetid FROM "
				+ "(SELECT a.id, a.v_contrib, way.changeset, way.datemodif FROM indicators.fougeres a, "
				+ "way WHERE a.is_way IS TRUE AND a.chgst_edits IS NULL AND a.id = way.id "
				+ "AND a.v_contrib = way.vway) as aubervilliers_building,"
				+ "	changeset WHERE aubervilliers_building.changeset = changeset.changesetid;";
		query = "SELECT a.id, a.v_contrib, way.changeset as changesetid FROM indicators.fougeres a, "
				+ "way WHERE a.is_way IS TRUE AND a.chgst_edits IS NULL AND a.id =	 way.id "
				+ "AND a.v_contrib = way.vway;";
		query = "SELECT a.id, a.v_contrib, relation.changeset as changesetid FROM indicators.fougeres a, "
				+ "relation WHERE a.is_way IS FALSE AND a.chgst_edits IS NULL AND a.id = relation.id "
				+ "AND a.v_contrib = relation.vrel;";

		Map<Integer, Long> userChangeset = new HashMap<Integer, Long>();
		Map<Long, Integer> chgstEdits = new HashMap<Long, Integer>();
		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			Long changesetid = r.getLong("changesetid");
			Integer id = r.getInt("id");
			userChangeset.put(id, changesetid);
			if (!chgstEdits.containsKey(changesetid)) {
				Integer nbEdits = ChangesetRetriever.getNbModifications(changesetid);
				String changesetValues = chgset.getChangesetValues(changesetid);
				chgset.insertOneRow(changesetValues); // insère les infos du
														// changeset dans la
														// table
														// changeset
				chgstEdits.put(changesetid, nbEdits);
			}

		}

		for (Integer userID : userChangeset.keySet()) {
			Integer nEdits = chgstEdits.get(userChangeset.get(userID));
			String update = "UPDATE indicators.fougeres SET chgst_edits=" + nEdits + " WHERE id = " + userID;
			System.out.println(update);
			chgset.executeQuery(update);
		}

	}

}
