package contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fr.ign.cogit.cartagen.algorithms.block.deletion.BuildingDeletionPromethee;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PreferenceFunction;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeCandidate;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeCriterion;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.PrometheeDecision;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.Type2PreferenceFunction;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.Type4PreferenceFunction;
import fr.ign.cogit.cartagen.util.multicriteriadecision.promethee.Type5PreferenceFunction;
import fr.ign.cogit.geoxygene.osm.contributor.OSMContributor;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMContributorAssessment;

/**
 * Algorithm that sorts OSM contributors according to several user metrics
 * 
 * @author QTTruong
 *
 */
public class UserMetricsPromethee {
	private static Logger LOGGER = Logger.getLogger(BuildingDeletionPromethee.class);
	private Collection<PrometheeCriterion> criteria;
	private Map<PrometheeCriterion, Double> weights;

	private static String PARAM_NB_CONTRIBUTIONS = "number of contributions";
	private static String PARAM_P_CREATE = "portion of created objects";
	private static String PARAM_P_MODIF = "portion of modified objects";
	private static String PARAM_P_DELETE = "portion of deleted objects";
	private static String PARAM_P_USED = "portion of used objects";
	private static String PARAM_P_COEDITED = "portion of later-on edited objects";
	private static String PARAM_P_DELETED = "portion of later-on deleted objects";

	public UserMetricsPromethee() {
		getDefaultCriteria();
	}

	public List<Integer> compute(Map<Long, OSMContributor> myContributors,
			DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> coeditg,
			DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> useg,
			DefaultDirectedWeightedGraph<Long, DefaultWeightedEdge> suppressiong) {

		List<Integer> userRanking = new ArrayList<Integer>();

		Collection<PrometheeCandidate> candidates = new HashSet<PrometheeCandidate>();

		for (OSMContributor user : myContributors.values()) {

			int totalContributions = user.getNbContributions();
			if (totalContributions == 0)
				continue;

			// compute the parameters
			Map<String, Object> parameters = new HashMap<>();

			// Number of contributions per contributor
			parameters.put(PARAM_NB_CONTRIBUTIONS, totalContributions);

			// Nb of creations / total contributions
			int nbCreate = OSMContributorAssessment.getNbCreations(user.getResource());
			Double pCreate = Double.valueOf(nbCreate) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_CREATE, pCreate);

			// Nb of modifications / total contributions
			int nbModif = OSMContributorAssessment.getNbModification(user.getResource());
			Double pModif = Double.valueOf(nbModif) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_MODIF, pModif);

			// Nb of suppression / total contributions
			int nbDelete = OSMContributorAssessment.getNbDeletes(user.getResource());
			Double pDelete = Double.valueOf(nbDelete) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_DELETE, pDelete);

			// Use indegree / total contributions
			int useIndegree = useg.inDegreeOf(Long.valueOf(user.getId()));
			Double pUsed = Double.valueOf(useIndegree) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_USED, pUsed);

			// Coedition indegree / total contributions
			int coeditIndegree = coeditg.inDegreeOf(Long.valueOf(user.getId()));
			Double pCoedited = Double.valueOf(coeditIndegree) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_COEDITED, pCoedited);

			// Suppression indegree / total contributions
			int deleteIndegree = suppressiong.inDegreeOf(Long.valueOf(user.getId()));
			Double pDeleted = Double.valueOf(deleteIndegree) / Double.valueOf(totalContributions);
			parameters.put(PARAM_P_DELETED, pDeleted);

			Map<PrometheeCriterion, Double> criteriaValues = new HashMap<PrometheeCriterion, Double>();
			for (PrometheeCriterion crit : this.criteria)
				criteriaValues.put(crit, crit.value(parameters));

			// create the action object
			PrometheeCandidate candidate = new PrometheeCandidate(user, criteriaValues);
			candidates.add(candidate);
		}
		PrometheeDecision method = new PrometheeDecision(weights);
		List<PrometheeCandidate> list = method.makeRankingDecision(candidates);
		System.out.println("Promethee number of candidates : " + list.size());

		for (int i = 0; i < list.size(); i++) {
			userRanking.add(((OSMContributor) list.get(i).getCandidateObject()).getId());
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Candidate ranking:");
			for (PrometheeCandidate cand : list)
				LOGGER.trace(((OSMContributor) cand.getCandidateObject()).getId());
		}
		return userRanking;
	}

	private void getDefaultCriteria() {
		this.criteria = new HashSet<>();
		this.weights = new HashMap<>();
		// nb of contributions criterion
		PrometheeCriterion nbContrib = new BuildNbContributionsCriterion("nb of contributions",
				new Type5PreferenceFunction(1., 32.));
		this.criteria.add(nbContrib);
		this.weights.put(nbContrib, 0.3);

		// pModif criterion
		PrometheeCriterion pModif = new BuildPModifyCriterion("pModif", new Type4PreferenceFunction(0.2686, 0.4130));
		this.criteria.add(pModif);
		this.weights.put(pModif, 0.1);

		// pDelete criterion
		PrometheeCriterion pDelete = new BuildPDeleteCriterion("pDelete", new Type4PreferenceFunction(0.0462, 0.1685));
		this.criteria.add(pDelete);
		this.weights.put(pDelete, 0.1);

		// pUsed criterion
		PrometheeCriterion pUsed = new BuildPUsedCriterion("pUsed", new Type2PreferenceFunction(0.04886));
		this.criteria.add(pUsed);
		this.weights.put(pUsed, 0.2);

		// pCoedited criterion
		PrometheeCriterion pCoedited = new BuildPCoeditedCriterion("pCoedited", new Type2PreferenceFunction(0.2154));
		this.criteria.add(pCoedited);
		this.weights.put(pCoedited, -0.1);

		// pDeleted criterion
		PrometheeCriterion pDeleted = new BuildPDeletedCriterion("pDeleted", new Type2PreferenceFunction(0.04615));
		this.criteria.add(pDeleted);
		this.weights.put(pDeleted, -0.2);

	}

	class BuildNbContributionsCriterion extends PrometheeCriterion {

		public BuildNbContributionsCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double nbContributions = Double.valueOf((Integer) param.get(PARAM_NB_CONTRIBUTIONS));
			return nbContributions;
		}
	}

	class BuildPCreateCriterion extends PrometheeCriterion {

		public BuildPCreateCriterion(String name, PreferenceFunction preferenceFunction) {
			super(name, preferenceFunction);
		}

		@Override
		public double value(Map<String, Object> param) {
			double v = (Double) param.get(PARAM_P_CREATE);
			return v;
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
			double v = (Double) param.get(PARAM_P_DELETED);
			return v;
		}
	}

}
