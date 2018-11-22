package vandalism.bremen;

import java.sql.ResultSet;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class AddMissingChangeset {
	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bremen", "postgres", "postgres");

		// Cherche les changesets des nodes, ways ou relations (modifier la
		// table en fonction de ce qu'on cherche) qui ne sont pas encores
		// stockés dans la base de données
		String query = "SELECT DISTINCT changeset FROM way "
				+ "WHERE changeset NOT IN (SELECT changesetid FROM changeset)";

		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			String changesetValues = chgset.getChangesetValues(r.getLong("changeset"));
			chgset.insertOneRow(changesetValues); // insère les infos du
													// changeset dans la table
													// changeset
		}

	}

}
