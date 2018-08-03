package promethee;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PreferenceFunction;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeCandidate;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeCriterion;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeDecision;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.Type6PreferenceFunction;

public class PrometheeFromCSV {
	private Collection<PrometheeCriterion> criteria;
	private Map<PrometheeCriterion, Double> weights;

	private static String PARAM_NB_CONTRIBUTIONS = "number of contributions";
	private static String PARAM_P_MODIF = "portion of modified objects";
	private static String PARAM_P_DELETE = "portion of deleted objects";
	private static String PARAM_P_USED = "portion of used objects";
	private static String PARAM_P_COEDITED = "portion of later-on edited objects";
	private static String PARAM_P_DELETED = "portion of later-on deleted objects";
	private static String PARAM_WEEKS = "total number of weeks contributed";
	private static String PARAM_FOCALISATION = "focalisation of the average changeset extent";

	public PrometheeFromCSV() {
		getDefaultCriteria();
	}

	/**
	 * Compute PROMETHEE candidates from a csv file that contains the metrics.
	 * 
	 * @param csvFilePath
	 * @return A collection of PROMETHEE candidates
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Collection<PrometheeCandidate> compute(String csvFilePath, char separator)
			throws FileNotFoundException, IOException {
		Collection<PrometheeCandidate> candidates = new HashSet<PrometheeCandidate>();
		File file = new File(csvFilePath);
		FileReader fr = new FileReader(file);

		CSVReader csvReader = new CSVReader(fr, separator);
		String[] nextLine = csvReader.readNext();
		while ((nextLine = csvReader.readNext()) != null) {
			// compute the parameters
			Map<String, Object> parameters = new HashMap<>();

			// User ID
			Long uid = Long.valueOf(nextLine[0]);
			// Number of contributions per contributor
			parameters.put(PARAM_NB_CONTRIBUTIONS, Double.valueOf(nextLine[1]));

			// Nb of modifications / total contributions
			parameters.put(PARAM_P_MODIF, Double.valueOf(nextLine[3]));

			// Nb of suppression / total contributions
			parameters.put(PARAM_P_DELETE, Double.valueOf(nextLine[4]));

			// Use indegree / total contributions
			parameters.put(PARAM_P_USED, Double.valueOf(nextLine[5]));

			// Coedition indegree / total contributions
			parameters.put(PARAM_P_COEDITED, Double.valueOf(nextLine[6]));

			// Suppression indegree / total contributions
			parameters.put(PARAM_P_DELETED, Double.valueOf(nextLine[7]));

			// Number of weeks
			parameters.put(PARAM_WEEKS, Double.valueOf(nextLine[8]));

			// Focalisation index
			parameters.put(PARAM_FOCALISATION, Double.valueOf(nextLine[9]));

			Map<PrometheeCriterion, Double> criteriaValues = new HashMap<PrometheeCriterion, Double>();
			for (PrometheeCriterion crit : this.criteria)
				criteriaValues.put(crit, crit.value(parameters));
			PrometheeCandidate candidate = new PrometheeCandidate(uid, criteriaValues);
			candidates.add(candidate);
		}

		return candidates;
	}

	public List<Integer> computeRanking(String csvFilePath, char separator) throws FileNotFoundException, IOException {

		List<Integer> userRanking = new ArrayList<Integer>();
		Collection<PrometheeCandidate> candidates = compute(csvFilePath, separator);
		PrometheeDecision method = new PrometheeDecision(weights);
		List<PrometheeCandidate> list = method.makeRankingDecision(candidates);
		System.out.println("Promethee number of candidates : " + list.size());

		for (int i = 0; i < list.size(); i++) {
			userRanking.add(Integer.valueOf(list.get(i).getCandidateObject().toString()));
		}
		return userRanking;
	}

	private void getDefaultCriteria() {
		this.criteria = new HashSet<>();
		this.weights = new HashMap<>();
		// nb of contributions criterion
		PrometheeCriterion nbContrib = new BuildNbContributionsCriterion("nb of contributions",
				new Type6PreferenceFunction(1));
		this.criteria.add(nbContrib);
		this.weights.put(nbContrib, 0.01);

		// pModif criterion
		PrometheeCriterion pModif = new BuildPModifyCriterion("pModif", new Type6PreferenceFunction(0.2));
		this.criteria.add(pModif);
		this.weights.put(pModif, 0.13);

		// pDelete criterion
		PrometheeCriterion pDelete = new BuildPDeleteCriterion("pDelete", new Type6PreferenceFunction(0.1));
		this.criteria.add(pDelete);
		this.weights.put(pDelete, 0.13);

		// pUsed criterion
		PrometheeCriterion pUsed = new BuildPUsedCriterion("pUsed", new Type6PreferenceFunction(0.1));
		this.criteria.add(pUsed);
		this.weights.put(pUsed, 0.13);

		// pCoedited criterion
		PrometheeCriterion pCoedited = new BuildPCoeditedCriterion("pCoedited", new Type6PreferenceFunction(0.01));
		this.criteria.add(pCoedited);
		this.weights.put(pCoedited, 0.1);

		// pDeleted criterion
		PrometheeCriterion pDeleted = new BuildPDeletedCriterion("pDeleted", new Type6PreferenceFunction(0.01));
		this.criteria.add(pDeleted);
		this.weights.put(pDeleted, 0.1);

		// Week criterion
		PrometheeCriterion weeks = new BuildWeeksCriterion("weeks", new Type6PreferenceFunction(2));
		this.criteria.add(weeks);
		this.weights.put(weeks, 0.2);

		// Focalisation criterion
		PrometheeCriterion focalisation = new BuildFocalisationCriterion("focalisation",
				new Type6PreferenceFunction(0.3));
		this.criteria.add(focalisation);
		this.weights.put(focalisation, 0.2);
	}

	class BuildNbContributionsCriterion extends PrometheeCriterion {

		public BuildNbContributionsCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double nbContributions = (double) param.get(PARAM_NB_CONTRIBUTIONS);
			return nbContributions;
		}
	}

	class BuildPModifyCriterion extends PrometheeCriterion {

		public BuildPModifyCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (Double) param.get(PARAM_P_MODIF);
			return v;
		}
	}

	class BuildPDeleteCriterion extends PrometheeCriterion {

		public BuildPDeleteCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (Double) param.get(PARAM_P_DELETE);
			return v;
		}
	}

	class BuildPUsedCriterion extends PrometheeCriterion {

		public BuildPUsedCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (Double) param.get(PARAM_P_USED);
			return v;
		}
	}

	class BuildPCoeditedCriterion extends PrometheeCriterion {

		public BuildPCoeditedCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (Double) param.get(PARAM_P_COEDITED);
			return v;
		}
	}

	class BuildPDeletedCriterion extends PrometheeCriterion {

		public BuildPDeletedCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (double) param.get(PARAM_P_DELETED);
			return v;
		}
	}

	class BuildWeeksCriterion extends PrometheeCriterion {

		public BuildWeeksCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (double) param.get(PARAM_WEEKS);
			return v;
		}
	}

	class BuildFocalisationCriterion extends PrometheeCriterion {

		public BuildFocalisationCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (double) param.get(PARAM_FOCALISATION);
			return v;
		}
	}

}
