package vandalism.aubervilliers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;

import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.LoadFromPostGIS;

public class MaxSpecialChar {
	public static final String FINAL_CHAR_REGEX = "[-_!@#$%^&*()[\\\\]|;',./{}\\\\\\\\:\\\"<>?€~¤µ§°@]";
	public static final String FINAL_CHAR_ALPHA_NUMERIC = "[^a-z0-9 ]";

	public static int countSpecialCharacter(String s) {
		return s.split(FINAL_CHAR_REGEX, -1).length - 1;
	}

	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "bretagne", "postgres", "postgres");

		LoadFromPostGIS ld = new LoadFromPostGIS("localhost", "5432", "bretagne", "postgres", "postgres");
		String query = "SELECT way.*, hstore_to_json(way.tags) FROM indicators.fougeres a , way "
				+ "WHERE a.max_special_char_ratio IS NULL AND a.id = way.id "
				+ "AND a.v_contrib = way.vway AND a.is_way IS TRUE;";

		ResultSet r1 = chgset.executeQuery(query);

		ld.writeOSMResource(r1, "way");

		query = "";
		for (OSMResource rb : ld.myJavaObjects) {
			double maxSpecialCharRatio = 0;

			for (String tag : rb.getTags().values()) {
				Double ratio = Double.valueOf(countSpecialCharacter(tag)) / Double.valueOf(tag.length());
				ratio = BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP).doubleValue();
				if (ratio > maxSpecialCharRatio)
					maxSpecialCharRatio = ratio;
			}
			query += "UPDATE indicators.fougeres SET max_special_char_ratio = " + maxSpecialCharRatio + " WHERE id ="
					+ rb.getId() + ";";
			System.out.println(query);

			chgset.executeAnyQuery(query);
			query = "";

		}
		chgset.executeAnyQuery(query);

		// Cas des relations
		query = "SELECT relation.*, hstore_to_json(relation.tags) FROM indicators.fougeres a , relation "
				+ "WHERE a.max_special_char_ratio IS NULL AND a.id = relation.id "
				+ "AND a.v_contrib = relation.vrel AND a.is_way IS FALSE;";

		System.out.println(query);

		r1 = chgset.executeQuery(query);
		ld.writeOSMResource(r1, "relation");
		query = "";
		for (OSMResource rb : ld.myJavaObjects) {
			double maxSpecialCharRatio = 0;

			for (String tag : rb.getTags().values()) {
				Double ratio = Double.valueOf(countSpecialCharacter(tag)) / Double.valueOf(tag.length());
				ratio = BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP).doubleValue();
				if (ratio > maxSpecialCharRatio)
					maxSpecialCharRatio = ratio;
			}
			query += "UPDATE indicators.fougeres SET max_special_char_ratio = " + maxSpecialCharRatio + " WHERE id ="
					+ rb.getId() + ";";
			System.out.println(query);

			chgset.executeAnyQuery(query);
			query = "";

		}
		chgset.executeAnyQuery(query);

	}

}
