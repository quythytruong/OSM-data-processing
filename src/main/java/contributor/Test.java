package contributor;

import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IDirectPosition;
import fr.ign.cogit.geoxygene.api.spatial.coordgeom.IEnvelope;
import fr.ign.cogit.geoxygene.datatools.CRSConversion;
import fr.ign.cogit.geoxygene.spatial.coordgeom.GM_Envelope;

public class Test {
	public static void main(String[] args) throws Exception {

		IDirectPosition lowerCorner = CRSConversion.wgs84ToLambert93(-180, -54.488268);
		IDirectPosition upperCorner = CRSConversion.wgs84ToLambert93(180, 81.19125);
		System.out.println(lowerCorner.getCoordinate(0));
		System.out.println(lowerCorner.getCoordinate(1));
		System.out.println(upperCorner.getCoordinate(0));
		System.out.println(upperCorner.getCoordinate(1));
		IEnvelope chgsetEnvelope = new GM_Envelope(upperCorner, lowerCorner);
		chgsetEnvelope.getGeom().setCRS(3758);
		System.out.println("Area = " + chgsetEnvelope.getGeom().getCRS());
	}

}
