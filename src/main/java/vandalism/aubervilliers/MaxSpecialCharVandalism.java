package vandalism.aubervilliers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

public class MaxSpecialCharVandalism {

	public static final String FINAL_CHAR_REGEX = "[-_!@#$%^&*()[\\\\]|;',./{}\\\\\\\\:\\\"<>?€~¤µ§°@]";
	public static final String FINAL_CHAR_ALPHA_NUMERIC = "[^a-z0-9 ]";

	public static int countSpecialCharacter(String s) {
		return s.split(FINAL_CHAR_REGEX, -1).length - 1;
	}

	public static void main(String[] args) throws Exception {

		ChangesetRetriever chgset = new ChangesetRetriever("localhost", "5432", "idf", "postgres", "postgres");

		// Cherche les données de bâti vandalisés
		String query = "SELECT * FROM vandalized_building_aubervilliers";

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
			query += "UPDATE indicators.aubervilliers_vandalism SET max_special_char_ratio = "
					+ specialCharRatios.get(id) + " WHERE id = " + id + "; ";
		chgset.executeAnyQuery(query);

	}
}
