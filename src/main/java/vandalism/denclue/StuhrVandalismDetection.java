package vandalism.denclue;

import java.io.FileNotFoundException;
import java.io.IOException;

public class StuhrVandalismDetection {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String path = "D:/Users/qttruong/data/stuhr/csv/indicators_stuhr.csv";
		char sep = ',';
		DenclueFromCSV stuhr = new DenclueFromCSV(path, sep);
		stuhr.compute(100, 100);

		System.out.println("Taille des " + stuhr.getClusters().getNumClusters() + " clusters : ");
		for (int size : stuhr.getClusters().getClusterSize())
			System.out.println(size);

		System.out.println();

		for (int label : stuhr.getUniqueLabels())
			System.out.println(label);

		System.out.println("Label des " + stuhr.getClusters().getClusterLabel().length + " éléments : ");

		// for (int label :
		// Arrays.stream(stuhr.getClusters().getClusterLabel()).distinct().toArray(Integer[]::new))
		// for (int label : stuhr.getClusters().getClusterLabel())
		// System.out.println(label);

		for (int id : stuhr.getIndicatorsByID().keySet()) {

			int classe = stuhr.getClusters().predict(stuhr.getIndicatorsByID().get(id));
			if (stuhr.getClusters().getClusterSize()[classe] < 6311) {
				System.out.println("------- id : " + id + " -------");
				System.out.println("Label de la classe " + classe);
				System.out
						.println("Nombre d'éléments dans la classe : " + stuhr.getClusters().getClusterSize()[classe]);
			}
		}

		System.out.println("nOutliers : " + stuhr.countOutliers());
		System.out.println("nClusters1 : " + stuhr.countClusterSize1());

		stuhr.toCSV("D:/Users/qttruong/data/stuhr/csv/DENCLUE_stuhr_precision_recall.csv");

	}

}
