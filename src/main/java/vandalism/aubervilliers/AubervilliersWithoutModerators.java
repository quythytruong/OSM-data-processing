package vandalism.aubervilliers;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import smile.clustering.Clustering;
import smile.clustering.DENCLUE;

public class AubervilliersWithoutModerators {
	public static void main(String[] args) throws Exception {
		Set<Long> fantasyVandalism = new HashSet<Long>();
		Set<Long> artisticVandalism = new HashSet<Long>();

		try (Scanner scanner = new Scanner(new File("D:/Users/qttruong/data/aubervilliers/artistic_vandalism.txt"));) {
			// int nword = 0;
			while (scanner.hasNext()) {
				String sent = scanner.next();
				// nword++;
				// System.out.printf("%3d) %s%n", nword, sent);
				artisticVandalism.add(Long.valueOf(sent));
			}
		}
		try (Scanner scanner = new Scanner(new File("D:/Users/qttruong/data/aubervilliers/fantasy_vandalism.txt"));) {
			// int nword = 0;
			while (scanner.hasNext()) {
				String sent = scanner.next();
				// nword++;
				// System.out.printf("%3d) %s%n", nword, sent);
				fantasyVandalism.add(Long.valueOf(sent));
			}
		}

		BuildingAssessment aubervilliers = new BuildingAssessment("localhost", "5432", "idf", "postgres", "postgres");

		// Avec les indicateurs géométriques
		String column = "";
		column += "id,";
		column += "perimeter_out_of_max,";
		column += "area_out_of_max,";
		column += "shortest_length_out_of_perimeter,";
		column += "median_length_out_of_perimeter,";
		column += "elongation,";
		column += "convexity,";
		column += "compacity";
		// column += "min_dist_surf_bati_bdtopo";

		Set<double[]> geomIndicators = aubervilliers.getNormalizedIndicators(column, "aubervilliers");
		// Set<double[]> geomIndicators = bremen.getNormalizedIndicators(column,
		// "stuhr");

		// Convert set into array
		double[][] dataGeom = new double[geomIndicators.size()][7];
		Map<Long, double[]> indexedBuildings = new HashMap<Long, double[]>();
		int i = 0;
		for (double[] row : geomIndicators) {
			// System.out.println("perimètre =" + row[0] + "; aire =" + row[1]);
			for (int j = 1; j < row.length; j++)
				dataGeom[i][j - 1] = row[j];
			indexedBuildings.put(new Double(row[0]).longValue(), dataGeom[i]);
			i++;

		}
		int normalbuildings = 0;
		for (long id : indexedBuildings.keySet()) {
			if (fantasyVandalism.contains(id) || artisticVandalism.contains(id))
				continue;
			normalbuildings++;
		}

		System.out.println(geomIndicators.size());
		System.out.println(normalbuildings);
		System.out.println(dataGeom[0].length);

		DENCLUE d1 = new DENCLUE(dataGeom, 0.005, indexedBuildings.size());

		int artisticVandalismClusterSize1 = 0;
		int artisticVandalismOutlier = 0;

		int fantasyVandalismClusterSize1 = 0;
		int fantasyVandalismOutlier = 0;

		int normalClusterSize1 = 0;
		int normalOutlier = 0;

		// for (int v = 1; v <= 27; v++) {
		// int labelClass = d1.predict(indexedBuildings.get(Long.valueOf(v)));
		// if (labelClass != Clustering.OUTLIER)
		// System.out.println(v + ") Cluster " + labelClass + " size : " +
		// d1.getClusterSize()[labelClass]);
		// else
		// System.out.println(v + ") classé outlier ");
		// }

		for (long id : indexedBuildings.keySet()) {
			int labelClass = d1.predict(indexedBuildings.get(id));
			// if (fantasyVandalism.contains(id) &&
			// d1.getClusterSize()[labelClass] != 1)
			// System.out.println("Fantasy vandalism in cluster size > 1 : " +
			// id);

			if (labelClass == Clustering.OUTLIER) {
				if (fantasyVandalism.contains(id)) {
					fantasyVandalismOutlier++;
					System.out.println("ID fantasy outlier :" + id);
				} else if (artisticVandalism.contains(id)) {
					artisticVandalismOutlier++;
					System.out.println("ID artistic outlier :" + id);
				} else
					normalOutlier++;
			} else if (d1.getClusterSize()[labelClass] == 1)
				if (fantasyVandalism.contains(id))
					fantasyVandalismClusterSize1++;
				else if (artisticVandalism.contains(id))
					artisticVandalismClusterSize1++;

				else
					normalClusterSize1++;
		}
		System.out.println("Fantasy vandalism : " + " outliers : " + fantasyVandalismOutlier + " - In cluster 1 : "
				+ fantasyVandalismClusterSize1);
		System.out.println();
		System.out.println();

		System.out.println("Artistic vandalism : " + "outliers : " + artisticVandalismOutlier + " - In cluster 1 : "
				+ artisticVandalismClusterSize1);
		System.out.println();
		System.out.println();

		System.out.println("Normal ");
		System.out.println("outliers : " + normalOutlier);
		System.out.println("In cluster 1 : " + normalClusterSize1);

	}

}
