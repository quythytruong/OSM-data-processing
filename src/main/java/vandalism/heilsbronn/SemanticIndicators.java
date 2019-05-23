package vandalism.heilsbronn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class SemanticIndicators {
	public static final String FINAL_CHAR_REGEX = "[-_!@#$%^&*()[\\\\]|;',./{}\\\\\\\\:\\\"<>?€~¤µ§°@]";
	public static final String FINAL_CHAR_ALPHA_NUMERIC = "[^a-z0-9 ]";

	public static int countSpecialCharacter(String s) {
		return s.split(FINAL_CHAR_REGEX, -1).length - 1;
	}

	public static void main(String[] args) throws Exception {
		String s = "hello-w%o*r!ld";
		System.out.println(countSpecialCharacter(s));
		System.out.println(s.length());
		Double value = Double.valueOf(countSpecialCharacter(s)) / Double.valueOf(s.length());
		value = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
		System.out.println(value);

		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "heilsbronn", "postgres", "postgres");

		chgset.executeAnyQuery(
				"ALTER TABLE indicators.heilsbronn_v ADD COLUMN max_special_char_ratio double precision;");
		// Cherche les données de bâti vandalisés
		String query = "SELECT * FROM heilsbronn_vandalisme;";

		ResultSet r = chgset.executeQuery(query);
		ResultSetMetaData rsmd = r.getMetaData();
		int columnsNumber = rsmd.getColumnCount() - 3;

		Map<Integer, Double> specialCharRatios = new HashMap<Integer, Double>();
		while (r.next()) {
			double maxSpecialCharRatio = 0;
			Integer id = r.getInt("id");
			System.out.println("id object : " + id);
			for (int i = 6; i <= columnsNumber; i++) {
				String tag = r.getString(i);
				// System.out.println(tag == null);
				if (tag == null)
					continue;
				Double ratio = Double.valueOf(countSpecialCharacter(tag)) / Double.valueOf(tag.length());
				ratio = BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP).doubleValue();
				if (ratio > maxSpecialCharRatio)
					maxSpecialCharRatio = ratio;

			}
			System.out.println("maxSpecialCharRatio : " + maxSpecialCharRatio);
			specialCharRatios.put(id, maxSpecialCharRatio);
		}

		query = "";
		for (int id : specialCharRatios.keySet())
			query += "UPDATE indicators.heilsbronn_v SET max_special_char_ratio = " + specialCharRatios.get(id)
					+ " WHERE id = " + id + "; ";
		chgset.executeAnyQuery(query);

		// Maximum ratio of special characters in the tags of Heilsbronn
		// buildings
		// BuildingAssessment b = new BuildingAssessment("localhost", "5432",
		// "heilsbronn", "postgres", "postgres");
		// b.loadBuildings("Heilsbronn", "2018-02-13T23:59:59Z");

		// LoadFromPostGIS ld = new LoadFromPostGIS("localhost", "5432",
		// "heilsbronn", "postgres", "postgres");
		// query = "SELECT way.*, hstore_to_json(way.tags) FROM
		// indicators.heilsbronn a , way "
		// + "WHERE a.max_special_char_ratio IS NULL AND a.id = way.id AND
		// a.v_contrib = way.vway AND a.is_way IS TRUE;";

		// query = "SELECT relation.*, hstore_to_json(relation.tags) FROM
		// indicators.stuhr a , relation "
		// + "WHERE a.max_special_char_ratio IS NULL AND a.id = relation.id "
		// + "AND a.v_contrib = relation.vrel AND a.is_way IS FALSE";

		// ResultSet r1 = chgset.executeQuery(query);

		// ld.writeOSMResource(r1, "way");

		// query = "";
		// for (OSMResource rb : ld.myJavaObjects) {
		// double maxSpecialCharRatio = 0;
		//
		// for (String tag : rb.getTags().values()) {
		// Double ratio = Double.valueOf(countSpecialCharacter(tag)) /
		// Double.valueOf(tag.length());
		// ratio = BigDecimal.valueOf(ratio).setScale(4,
		// RoundingMode.HALF_UP).doubleValue();
		// if (ratio > maxSpecialCharRatio)
		// maxSpecialCharRatio = ratio;
		// }
		// query += "UPDATE indicators.heilsbronn SET max_special_char_ratio = "
		// + maxSpecialCharRatio + " WHERE id ="
		// + rb.getId() + ";";
		// System.out.println(query);
		//
		// chgset.executeAnyQuery(query);
		// query = "";
		//
		// }
		// chgset.executeAnyQuery(query);

	}

}
