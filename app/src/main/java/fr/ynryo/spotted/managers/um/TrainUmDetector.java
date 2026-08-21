package fr.ynryo.spotted.managers.um;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.spotted.genericMarkerDatas.MarkerStandardized;

/**
 * Reçoit une liste de trains fraichement fetched et return une liste de trains nettoyée contenant des UMs (Unités Multiples)
 */
public class TrainUmDetector {
    private static final double UM_THRESHOLD_DEG = 0.0005;

    /**
     * Détecte les trains circulant en Unité Multiple (UM).
     *
     * @param markers La liste des trains récupérés.
     * @return umPairs Une liste de paires de trains couplés.
     */
    public static List<MarkerStandardized> filterUm(List<MarkerStandardized> markers) {
        List<MarkerStandardized> result = new ArrayList<>(markers);

        for (int i = 0; i < result.size(); i++) {
            MarkerStandardized a = result.get(i);
            if (!a.isTrain() || a.getBearing() == 0) continue; //bearing == 0 == gare

            for (int j = i + 1; j < result.size(); j++) {
                MarkerStandardized b = result.get(j);
                if (!b.isTrain() || b.getBearing() == 0) continue;

                if (areColocated(a, b)) {
                    result.set(i, TrainUmAssembler.buildUmMarker(a, b)); // remplace a par l'UM
                    result.remove(j); // supprime b
                    break; // a ne peut avoir qu'un seul partenaire
                }
            }
        }

        return result;
    }

    /**
     * Determines whether two MarkerStandardized objects are geographically colocated
     * based on a predefined threshold for latitude and longitude differences.
     *
     * @param a The first MarkerStandardized object to compare.
     * @param b The second MarkerStandardized object to compare.
     * @return true if the two markers are within the threshold distance for both latitude
     * and longitude; false otherwise.
     */
    private static boolean areColocated(MarkerStandardized a, MarkerStandardized b) {
        return Math.abs(a.getLatitude() - b.getLatitude()) < UM_THRESHOLD_DEG && Math.abs(a.getLongitude() - b.getLongitude()) < UM_THRESHOLD_DEG;
    }
}
