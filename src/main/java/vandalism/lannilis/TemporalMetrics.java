package vandalism.lannilis;

import java.sql.ResultSet;
import java.sql.SQLException;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class TemporalMetrics {
	public static void main(String[] args) throws SQLException {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bretagne", "postgres", "postgres");

		String query = "SELECT DISTINCT changeset.changesetid FROM"
				+ "(SELECT a.id, a.v_contrib, way.changeset, way.datemodif FROM" + " indicators.lannilis a, "
				+ "way WHERE a.is_way IS TRUE AND a.chgst_edits IS NULL AND a.id =" + " way.id "
				+ "AND a.v_contrib = way.vway) as lannilis_building," + "changeset WHERE lannilis_building.changeset ="
				+ "		 changeset.changesetid;";

		ResultSet r = chgset.executeQuery(query);
		while (r.next()) {
			Long chgstid = r.getLong("changesetid");
			Integer nbEdits = ChangesetRetriever.getNbModifications(chgstid);
			// Integer id = r.getInt("id");
			String update = "UPDATE indicators.lannilis SET chgst_edits=" + nbEdits + " WHERE id "
					+ "IN (SELECT b.id FROM way a, indicators.lannilis b WHERE" + "	 a.changeset = " + chgstid
					+ " AND a.id = b.id AND b.v_contrib =a.vway);";
			System.out.println(update);
			chgset.executeQuery(update);
		}

		Integer nbEdits = ChangesetRetriever.getNbModifications(Long.valueOf(56640077));
		String update = "UPDATE indicators.lannilis SET chgst_edits=" + nbEdits + " WHERE id = 7922291;";
		System.out.println(update);
		chgset.executeQuery(update);

	}
}
