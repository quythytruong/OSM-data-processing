package vandalism;

import smile.clustering.Clustering;
import smile.clustering.DENCLUE;

public class TestDENCLUE {

	public static void main(String[] args) {
		double sigma = 1;
		int m = 10;

		double[][] data = new double[10][1];

		data[0][0] = 1.00;
		data[1][0] = 1.20;
		data[2][0] = 1.10;
		data[3][0] = 1.15;
		data[4][0] = 102.0;
		data[5][0] = 1.02;
		data[6][0] = 1.03;
		data[7][0] = 1.04;
		data[8][0] = 1.05;
		data[9][0] = 1.06;

		DENCLUE clusters = new DENCLUE(data, sigma, m);

		System.out.println("---------------------------------------------------");

		for (int i = 0; i < data.length; i++) {
			int classe = clusters.predict(data[i]);
			boolean isOutlier = (classe == Clustering.OUTLIER);

			System.out.println("Classe du point " + i + " : " + classe + " / outlier : " + isOutlier);

		}

		System.out.println("---------------------------------------------------");
		System.out.println("Nombre de cluster(s) = " + clusters.getNumClusters());
		System.out.println("---------------------------------------------------");
		System.out.println("Moyenne cluster 1 = " + clusters.getDensityAttractors()[0][0]);
		System.out.println("Moyenne cluster 2 = " + clusters.getDensityAttractors()[1][0]);

		System.out.println("---------------------------------------------------");

		System.out.println("Nombre de clusters : " + clusters.getNumClusters());
		for (int num = 0; num < clusters.getNumClusters(); num++) {
			System.out.println("Classe " + num);
			System.out.println("Nombre d'éléments : " + clusters.getClusterSize()[num]);
		}
		for (int label : clusters.getClusterLabel())
			System.out.println(label);
	}

}
