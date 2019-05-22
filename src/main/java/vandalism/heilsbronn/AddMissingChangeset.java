package vandalism.heilsbronn;

import java.sql.ResultSet;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class AddMissingChangeset {
	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "heilsbronn", "postgres", "postgres");
		// Add changeset table
		chgset.createChangesetTable();

		// Cherche les changesets des nodes, ways ou relations (modifier la
		// table en fonction de ce qu'on cherche) qui ne sont pas encores
		// stockés dans la base de données

		// String query = "SELECT DISTINCT changeset FROM node "
		// + "WHERE changeset NOT IN (SELECT changesetid FROM changeset)";

		// String query = "SELECT DISTINCT changeset FROM way "
		// + "WHERE changeset NOT IN (SELECT changesetid FROM changeset)";

		String query = "SELECT DISTINCT changeset FROM way WHERE id IN (SELECT id FROM indicators.heilsbronn) "
				+ "AND changeset NOT IN (SELECT changesetid FROM changeset)";

		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			String changesetValues = chgset.getChangesetValues(r.getLong("changeset"));
			chgset.insertOneRow(changesetValues); // insère les infos du
													// changeset dans la table
													// changeset
		}

	}

}
