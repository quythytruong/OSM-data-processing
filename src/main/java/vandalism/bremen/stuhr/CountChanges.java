package vandalism.bremen.stuhr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.ign.cogit.geoxygene.osm.anonymization.db.SQLDBPreAnonymization;
import fr.ign.cogit.geoxygene.osm.importexport.OSMObject;
import fr.ign.cogit.geoxygene.osm.importexport.OSMResource;
import fr.ign.cogit.geoxygene.osm.importexport.metrics.OSMObjectAssessment;

public class CountChanges {
	public static void main(String[] args) {
		int[] batiExistant = { 26949825, 37235897, 53528159, 118916923, 118917078, 199044898, 199044899, 248540227,
				263739977 };
		String previousYears = "2014-11-23T00:00:00Z";
		// String urlAPI =
		// "https://api.openstreetmap.org/api/0.6/way/26949825/history";
		// System.out.println(urlAPI);
		// Document xml = SQLDBPreAnonymization.getDataFromAPI(urlAPI);
		// Node osm = xml.getFirstChild();
		// NodeList ways = osm.getChildNodes();

		for (int b : batiExistant) {
			System.out.println("--------------------------------------------------------------");
			System.out.println("id : " + b);
			String urlAPI = "https://api.openstreetmap.org/api/0.6/way/" + b + "/history";
			Document xml = SQLDBPreAnonymization.getDataFromAPI(urlAPI);
			Node osm = xml.getFirstChild();
			NodeList ways = osm.getChildNodes();
			List<OSMResource> resources = new ArrayList<OSMResource>();
			OSMObject o = new OSMObject(b);

			for (int i = 1; i < ways.getLength(); i++) {
				if (ways.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element way = (Element) ways.item(i);

					boolean isAfterPreviousYears = Date.from(Instant.parse(way.getAttribute("timestamp")))
							.after(Date.from(Instant.parse(previousYears)));
					if (isAfterPreviousYears) {
						OSMResource r = new OSMResource(way.getAttribute("user"), null, String.valueOf(b),
								way.getAttribute("changeset"), way.getAttribute("version"), way.getAttribute("uid"),
								way.getAttribute("timestamp"), way.getAttribute("visible"));
						NodeList tagsAndNodes = way.getChildNodes();
						for (int j = 1; j < tagsAndNodes.getLength(); j++) {
							List<Long> wayComposition = new ArrayList<Long>();
							if (tagsAndNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
								Element nd = (Element) tagsAndNodes.item(j);
								if (nd.getNodeName().equals("nd")) {
									// System.out.println(nd.getAttribute("ref"));
									wayComposition.add(Long.valueOf(nd.getAttribute("ref")));
								}
								if (nd.getNodeName().equals("tag")) {
									// System.out.println(nd.getAttribute("k") +
									// " " + nd.getAttribute("v"));
									r.addTag(nd.getAttribute("k"), nd.getAttribute("v"));
								}
							}
							o.wayComposition.add(wayComposition);
						}

						resources.add(r);
					}
				}
			}
			if (resources.size() > 1) {

				o.setContributions(resources);
				System.out.println(resources.size());
				System.out.println("n_geom_edition : " + o.getNbGeomEdit());
				System.out.println("n_tag_edition : " + OSMObjectAssessment.getNbTagEdition(o));
				System.out.println("n_tag_add : " + OSMObjectAssessment.getNbTagAddition(o));
				System.out.println("n_tag_del : " + OSMObjectAssessment.getNbTagDelete(o));
			}
		}
	}

}
