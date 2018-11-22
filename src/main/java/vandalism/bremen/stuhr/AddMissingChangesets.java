package vandalism.bremen.stuhr;

import java.sql.ResultSet;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class AddMissingChangesets {

	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bremen", "postgres", "postgres");

		// Cherche les changesets des données de Stuhr qui ne sont pas encores
		// stockés dans la base de données
		String query = "SELECT way.changeset FROM indicators.stuhr a, way "
				+ "WHERE a.chgst_duration_seconds IS NULL AND a.id = way.id AND a.v_contrib = way.vway";

		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			String changesetValues = chgset.getChangesetValues(r.getLong("changeset"));
			chgset.insertOneRow(changesetValues); // insère les infos du
													// changeset dans la table
													// changeset
		}

		query = "SELECT a.id, relation.changeset FROM indicators.stuhr a , relation "
				+ "WHERE a.chgst_edits IS NULL AND a.id = relation.id AND a.v_contrib = relation.vrel";

		ResultSet r1 = chgset.executeQuery(query);
		while (r1.next()) {
			Integer nbEdits = ChangesetRetriever.getNbModifications(r1.getLong("changeset"));
			Integer id = r1.getInt("id");
			String update = "UPDATE indicators.stuhr SET chgst_edits=" + nbEdits + " WHERE id = " + id;
			System.out.println(update);
			chgset.executeQuery(update);
		}

	}

}
