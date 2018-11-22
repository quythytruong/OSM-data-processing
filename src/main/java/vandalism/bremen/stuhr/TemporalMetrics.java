package vandalism.bremen.stuhr;

import java.sql.ResultSet;
import java.sql.SQLException;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class TemporalMetrics {
	public static void main(String[] args) throws SQLException {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bremen", "postgres", "postgres");
		// System.out.print(chgset.getChangesetValues(Long.valueOf(57519856)));
		// System.out.print(ChangesetRetriever.getNbModifications(Long.valueOf(57519856)));
		String query = "SELECT stuhr_building.id, changeset.changesetid FROM "
				+ "(SELECT a.id, a.v_contrib, way.changeset, way.datemodif FROM indicators.stuhr a, "
				+ "way WHERE a.is_way IS TRUE AND a.id = way.id AND a.v_contrib = way.vway) as stuhr_building,"
				+ "changeset WHERE stuhr_building.changeset = changeset.changesetid;";

		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			Integer nbEdits = ChangesetRetriever.getNbModifications(r.getLong("changesetid"));
			Integer id = r.getInt("id");
			String update = "UPDATE indicators.stuhr SET chgst_edits=" + nbEdits + " WHERE id = " + id;
			System.out.println(update);
			chgset.executeQuery(update);
		}

	}
}
