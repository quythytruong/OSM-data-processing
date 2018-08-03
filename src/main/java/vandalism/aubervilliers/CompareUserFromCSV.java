package vandalism.aubervilliers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ign.cogit.geoxygene.osm.importexport.metrics.ContributorAssessment;
import promethee.PrometheeFromCSV;

public class CompareUserFromCSV {
	public static void main(String[] args) throws Exception {
		PrometheeFromCSV rankingPromethee = new PrometheeFromCSV();

		List<Integer> usersRangedByPromethee = rankingPromethee
				.computeRanking("D:/Users/qttruong/data/aubervilliers/user_indicators.csv", ',');

		// Write PROMETHEE ranking into csv file
		Map<Long, Object[]> prometheeRanking = new HashMap<Long, Object[]>();
		for (int i = 0; i < usersRangedByPromethee.size(); i++) {
			Object[] ranking = { i };
			prometheeRanking.put(Long.valueOf(usersRangedByPromethee.get(i)), ranking);
		}
		ContributorAssessment.FILE_HEADER = "uid,ranking_promethee";
		ContributorAssessment.toCSV(prometheeRanking,
				new File("D:/Users/qttruong/data/aubervilliers/user_ranking_promethee.csv"));
	}
}
