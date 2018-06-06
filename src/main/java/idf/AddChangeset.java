package idf;

import fr.ign.cogit.geoxygene.osm.importexport.metrics.BuildingAssessment;
import fr.ign.cogit.geoxygene.osm.importexport.postgis.ChangesetRetriever;

/***
 * Add changeset information from OSM data in PostgreSQL database.
 * 
 * @author QTTruong
 *
 */
public class AddChangeset {
	public static void main(String[] args) throws Exception {
		ChangesetRetriever chgstRtv = new ChangesetRetriever("localhost", "5432", "idf", "postgres", "postgres");
		chgstRtv.updateDataFromChangeset();
		ChangesetRetriever.getOsmChange(Long.valueOf(48168515));
		chgstRtv.createChangesetTable();
		System.out.println("Changeset Node");
		chgstRtv.updateChangeSetTable("node");
		System.out.println("Changeset way");
		chgstRtv.updateChangeSetTable("way");
		System.out.println("Changeset relation");
		chgstRtv.updateChangeSetTable("relation");
		String val = chgstRtv.getChangesetValues(Long.valueOf(36376659));
		chgstRtv.insertOneRow(val);

		BuildingAssessment b = new BuildingAssessment("localhost", "5432", "bremen", "postgres", "postgres");
		b.updateContributorInfos(5495072);
		b.updateContributorInfos(465118);
		b.updateContributorInfos(2276105);

	}

}
