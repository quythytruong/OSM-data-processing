package vandalism.denclue;

import java.io.FileNotFoundException;
import java.io.IOException;

public class AubervilliersVandalismDetection {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		String path = "D:/Users/qttruong/data/aubervilliers/csv/indicators_aubervilliers_avec_appariement.csv";
		char sep = ',';
		DenclueFromCSV stuhr = new DenclueFromCSV(path, sep);
		stuhr.compute(100, 100);

		System.out.println("Nmobre d'anomalies " + stuhr.countOutliers());
		System.out.println("Nmobre de données vandalisées " + stuhr.countVandalism());

		System.out.println("Taille des " + stuhr.getClusters().getNumClusters() + " clusters : ");
		for (int size : stuhr.getClusters().getClusterSize())
			System.out.println(size);

		System.out.println();

		for (int label : stuhr.getUniqueLabels())
			System.out.println(label);

		System.out.println("Label des " + stuhr.getClusters().getClusterLabel().length + " éléments : ");

		for (int id : stuhr.getIndicatorsByID().keySet()) {

			int classe = stuhr.getClusters().predict(stuhr.getIndicatorsByID().get(id));
			if (stuhr.getClusters().getClusterSize()[classe] < 6311) {
				System.out.println("------- id : " + id + " -------");
				System.out.println("Label de la classe " + classe);
				System.out
						.println("Nombre d'éléments dans la classe : " + stuhr.getClusters().getClusterSize()[classe]);
			}
		}

		Double pOutliers = ((double) stuhr.countOutliers()) / ((double) stuhr.getLabelledData().values().size());
		System.out.println("pOutliers : " + pOutliers);
		Double pClusters1 = ((double) stuhr.countClusterSize1()) / ((double) stuhr.getLabelledData().size());
		System.out.println("pClusters1 : " + pClusters1);

		Double tpOutliers = ((double) stuhr.countTruePositiveOutlier()) / ((double) stuhr.countVandalism());
		System.out.println("tpOutliers : " + tpOutliers);
		Double tpClusters1 = ((double) stuhr.countTruePositiveClusterSize1()) / ((double) stuhr.countVandalism());
		System.out.println("tpClusters1 : " + tpClusters1);

		// stuhr.toCSV_cluster2(
		// "D:/Users/qttruong/data/aubervilliers/csv/DENCLUE_aubervilliers_avec_appariement_precision_recall_cluster2.csv");

	}

}
