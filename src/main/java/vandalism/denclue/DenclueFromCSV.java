package vandalism.denclue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import smile.clustering.Clustering;
import smile.clustering.DENCLUE;

public class DenclueFromCSV {
	private double[][] data;
	private Map<Integer, double[]> indicatorsByID;
	private Map<Integer, Boolean> labelledData;
	private int nbIndicators;

	private DENCLUE clusters;

	DenclueFromCSV(String csvFilePath, char separator) throws FileNotFoundException, IOException {
		this.indicatorsByID = new HashMap<Integer, double[]>();
		this.labelledData = new HashMap<Integer, Boolean>();

		File file = new File(csvFilePath);
		FileReader fr = new FileReader(file);

		CSVReader csvReader = new CSVReader(fr, separator);
		// Skip header
		csvReader.readNext();

		String[] nextLine = csvReader.readNext();

		this.nbIndicators = nextLine.length - 2; // On ne compte pas la colonne
													// ID et vandalism

		while ((nextLine = csvReader.readNext()) != null) {

			// Données labellisée comme étant du vandalisme ou non
			Integer id = Integer.valueOf(nextLine[0]);
			Boolean vandalism = Boolean.valueOf(nextLine[1]);
			this.labelledData.put(id, vandalism);

			// Indicateurs de chaque objet identifié par son ID
			List<Double> indicators = new ArrayList<Double>();
			for (int i = 2; i < nextLine.length; i++) {
				indicators.add(Double.valueOf(nextLine[i]));
			}
			Double[] stockData = indicators.stream().toArray(Double[]::new);
			double[] d = ArrayUtils.toPrimitive(stockData);
			this.indicatorsByID.put(id, d);

		}
		csvReader.close();

		this.data = new double[this.indicatorsByID.size()][this.nbIndicators];

		int l = 0;

		for (Integer id : this.indicatorsByID.keySet()) {
			this.data[l] = this.indicatorsByID.get(id);
			l++;
		}
	}

	public void compute(double sigma, int m) {
		this.setClusters(new DENCLUE(this.data, sigma, m));
	}

	public Integer countOutliers() {
		int count = 0;
		for (Integer id : this.indicatorsByID.keySet())
			if (isOutlier(this.indicatorsByID.get(id)))
				count++;
		return count;
	}

	public Integer countClusterSize1() {
		int count = 0;
		for (Integer id : this.indicatorsByID.keySet())
			if (isClusterSize1(this.indicatorsByID.get(id)))
				count++;
		return count;
	}

	public Integer countTruePositiveOutlier() {
		int count = 0;
		for (Integer id : this.labelledData.keySet())
			if (this.labelledData.get(id))
				if (isOutlier(this.indicatorsByID.get(id)))
					count++;
		return count;
	}

	public Integer countTruePositiveClusterSize1() {
		int count = 0;
		for (Integer id : this.labelledData.keySet())
			if (this.labelledData.get(id))
				if (isClusterSize1(this.indicatorsByID.get(id)))
					count++;
		return count;
	}

	public Integer countClusterSize2() {
		int count = 0;
		for (Integer id : this.indicatorsByID.keySet())
			if (isClusterSize2(this.indicatorsByID.get(id)))
				count++;
		return count;
	}

	public Integer countTruePositiveClusterSize2() {
		int count = 0;
		for (Integer id : this.labelledData.keySet())
			if (this.labelledData.get(id))
				if (isClusterSize2(this.indicatorsByID.get(id)))
					count++;
		return count;
	}

	public boolean isOutlier(double[] d) {
		int classe = this.clusters.predict(d);
		return (classe == Clustering.OUTLIER);
	}

	public boolean isClusterSize1(double[] d) {
		int classe = this.clusters.predict(d);
		try {
			return (this.clusters.getClusterSize()[classe] == 1);
		} catch (ArrayIndexOutOfBoundsException e) { // Cas d'une anomalie
			// System.out.println("Label de la classe : " + classe);
			// System.out.println("Nombre de clusters " +
			// this.getUniqueLabels().size());

			return false;
		}
	}

	public boolean isClusterSize2(double[] d) {
		int classe = this.clusters.predict(d);
		try {
			return (this.clusters.getClusterSize()[classe] == 2);
		} catch (ArrayIndexOutOfBoundsException e) { // Cas d'une anomalie
			// System.out.println("Label de la classe : " + classe);
			// System.out.println("Nombre de clusters " +
			// this.getUniqueLabels().size());

			return false;
		}
	}

	public DENCLUE getClusters() {
		return clusters;
	}

	public void setClusters(DENCLUE clusters) {
		this.clusters = clusters;
	}

	public Map<Integer, double[]> getIndicatorsByID() {
		return this.indicatorsByID;
	}

	public Map<Integer, Boolean> getLabelledData() {
		return this.labelledData;
	}

	public double overallPrecision(int sigma, int m) {
		this.compute(sigma, m);
		return (this.countTruePositiveOutlier() + this.countTruePositiveClusterSize1())
				/ (this.countClusterSize1() + this.countOutliers());

	}

	public double outlierPrecision(int sigma, int m) {
		this.compute(sigma, m);
		return this.countTruePositiveOutlier() / this.countOutliers();

	}

	public int countVandalism() {
		int count = 0;
		for (boolean b : this.labelledData.values())
			if (b)
				count++;
		return count;
	}

	public Set<Integer> getUniqueLabels() {
		Set<Integer> hashSet = new HashSet<Integer>();

		for (int d : this.getClusters().getClusterLabel())
			hashSet.add(d);

		return hashSet;
	}

	public void toCSV(String csvFilePath) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath), ',');
		// String[] line = { "sigma", "m", "p_outliers", "p_cluster_size1",
		// "TP_outliers", "TP_overall" };
		String[] line = { "sigma", "m", "n_outliers", "n_cluster_size1", "TruePositive", "FalsePositive",
				"overall_precision", "overall_recall" };
		writer.writeNext(line);

		for (double sigma = 0.001; sigma < 0.3; sigma = sigma * 2) {
			for (int m = 1000; m < 4000; m += 1000) {
				this.compute(sigma, m);
				System.out.println("------------------" + " sigma = " + sigma + " ; m = " + m + " ------------------");

				System.out.println("outliers : " + this.countOutliers());
				System.out.println("clusters size 1 : " + this.countClusterSize1());

				double truePositive = (double) this.countTruePositiveOutlier()
						+ (double) this.countTruePositiveClusterSize1();

				double falsePositive = (double) this.countClusterSize1() - (double) this.countTruePositiveClusterSize1()
						+ (double) this.countTruePositiveOutlier() - (double) this.countTruePositiveOutlier();

				// Precision = true positive / elements which are classified as
				// outlier
				Double overallPrecision = truePositive
						/ ((double) this.countOutliers() + (double) this.countClusterSize1());

				// Recall = true positive / element which are real vandalism
				Double overallRecall = truePositive / (double) this.countVandalism();

				System.out.println("overallPrecision : " + overallPrecision);
				System.out.println("overallRecall : " + overallRecall);

				String[] row = { String.valueOf(sigma), String.valueOf(m), String.valueOf(this.countOutliers()),
						String.valueOf(this.countClusterSize1()), String.valueOf(truePositive),
						String.valueOf(falsePositive), String.valueOf(overallPrecision),
						String.valueOf(overallRecall) };
				writer.writeNext(row);
			}
		}

		writer.close();

	}

	public void toCSV_cluster2(String csvFilePath) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath), ',');
		// String[] line = { "sigma", "m", "p_outliers", "p_cluster_size1",
		// "TP_outliers", "TP_overall" };
		String[] line = { "sigma", "m", "n_outliers", "n_cluster_size1", "TruePositive", "FalsePositive",
				"overall_precision", "overall_recall" };
		writer.writeNext(line);

		for (double sigma = 0.001; sigma < 0.3; sigma = sigma * 2) {
			for (int m = 1000; m < 4000; m += 1000) {
				this.compute(sigma, m);
				System.out.println("------------------" + " sigma = " + sigma + " ; m = " + m + " ------------------");

				System.out.println("outliers : " + this.countOutliers());
				System.out.println("clusters size 1 : " + this.countClusterSize1());

				double truePositive = (double) this.countTruePositiveOutlier()
						+ (double) this.countTruePositiveClusterSize1() + (double) this.countTruePositiveClusterSize2();

				double falsePositive = (double) this.countClusterSize1() - (double) this.countTruePositiveClusterSize1()
						+ (double) this.countTruePositiveOutlier() - (double) this.countTruePositiveOutlier()
						+ (double) this.countClusterSize2() - (double) this.countTruePositiveClusterSize2();

				// Precision = true positive / elements which are classified as
				// outlier
				Double overallPrecision = truePositive / ((double) this.countOutliers()
						+ (double) this.countClusterSize1() + (double) this.countClusterSize2());

				// Recall = true positive / element which are real vandalism
				Double overallRecall = truePositive / (double) this.countVandalism();

				System.out.println("overallPrecision : " + overallPrecision);
				System.out.println("overallRecall : " + overallRecall);

				String[] row = { String.valueOf(sigma), String.valueOf(m), String.valueOf(this.countOutliers()),
						String.valueOf(this.countClusterSize1()), String.valueOf(truePositive),
						String.valueOf(falsePositive), String.valueOf(overallPrecision),
						String.valueOf(overallRecall) };
				writer.writeNext(row);
			}
		}

		writer.close();

	}
}
