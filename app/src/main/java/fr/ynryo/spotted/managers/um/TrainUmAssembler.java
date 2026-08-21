package fr.ynryo.spotted.managers.um;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.ynryo.spotted.genericMarkerDatas.MarkerStandardized;
import fr.ynryo.spotted.genericMarkerDatas.MarkerStop;
import fr.ynryo.spotted.genericMarkerDatas.MarkerType;
import fr.ynryo.spotted.utils.Time;

public class TrainUmAssembler {
    private final static String TAG = "TrainUmAssembler";

    /**
     * Builds a unified marker by combining data from two MarkerStandardized instances.
     * The unified marker includes a composite ID, average location, and combined metadata.
     *
     * @param trainA the first MarkerStandardized instance to be merged
     * @param trainB the second MarkerStandardized instance to be merged
     * @return a MarkerStandardized instance representing the unified marker created from the input instances
     */
    public static MarkerStandardized buildUmMarker(MarkerStandardized trainA, MarkerStandardized trainB) {
        MarkerStandardized um = new MarkerStandardized();
        String idA = trainA.getId();
        String idB = trainB.getId();
        um.setId(idA.compareTo(idB) <= 0 ? idA + "+" + idB : idB + "+" + idA);

        um.setMarkerType(MarkerType.TRAIN);
        um.setUmPair(trainA, trainB);

        um.setLatitude((trainA.getLatitude() + trainB.getLatitude()) / 2.0);
        um.setLongitude((trainA.getLongitude() + trainB.getLongitude()) / 2.0);
        um.setBearing((trainA.getBearing() + trainB.getBearing()) / 2.0f);
        um.setFillColor(trainA.getFillColor());
        um.setTextColor(trainA.getTextColor());
        um.setLineNumber(trainA.getLineNumber() + " et " + trainB.getLineNumber());
        um.setNetworkRef(trainA.getNetworkRef());

        return um;
    }

    /**
     * Assembles a list of timeline rows representing stops for a unified marker (UM),
     * combining and aligning stops from two associated MarkerStandardized instances.
     *
     * @param um the unified marker containing two associated MarkerStandardized instances, which
     *           represent two trains combined as a UM. The method uses these instances to retrieve and
     *           align their respective stop data.
     * @return a list of TrainUmTimelineRow objects, where each row represents either a common stop,
     *         a split stop (distinct stops from the two trains), or an aligned stop based on calculated
     *         chronological order.
     */
    public static List<TrainUmTimelineRow> assembleUmStops(MarkerStandardized um) {
        List<TrainUmTimelineRow> displayRows = new ArrayList<>();

        if (!um.isUm()) {
            return displayRows;
        }

        MarkerStandardized trainA = um.getUmA();
        MarkerStandardized trainB = um.getUmB();

        List<MarkerStop> stopsA = trainA.getStops() != null ? trainA.getStops() : new ArrayList<>();
        List<MarkerStop> stopsB = trainB.getStops() != null ? trainB.getStops() : new ArrayList<>();

        if (stopsA.isEmpty() && stopsB.isEmpty()) return displayRows;

        // 1. Trouver les références des arrêts communs
        List<String> commonRefs = new ArrayList<>();
        Set<String> bRefs = new HashSet<>();
        for (MarkerStop b : stopsB) bRefs.add(b.getStopRef());
        for (MarkerStop a : stopsA) {
            if (bRefs.contains(a.getStopRef())) commonRefs.add(a.getStopRef());
        }

        if (commonRefs.isEmpty()) {
            // Aucun arrêt commun : on affiche tout en parallèle
            zipStopsSideBySide(stopsA, stopsB, 0, stopsA.size(), 0, stopsB.size(), displayRows);
            return displayRows;
        }

        String firstCommon = commonRefs.get(0);
        String lastCommon = commonRefs.get(commonRefs.size() - 1);

        int indexA = 0;
        int indexB = 0;

        // ==========================================
        // PHASE 1 : AVANT LE MERGE (Côte à côte)
        // ==========================================
        int endPhase1A = findStopIndex(stopsA, firstCommon);
        int endPhase1B = findStopIndex(stopsB, firstCommon);

        if (endPhase1A > 0 || endPhase1B > 0) {
            zipStopsSideBySide(stopsA, stopsB, indexA, endPhase1A, indexB, endPhase1B, displayRows);
            // Insertion du graphique de jonction
            displayRows.add(TrainUmTimelineRow.createMergeGraphic());
        }

        indexA = endPhase1A;
        indexB = endPhase1B;

        // ==========================================
        // PHASE 2 : TRAJET COMMUN (Fusionné)
        // ==========================================
        int endPhase2A = findStopIndex(stopsA, lastCommon) + 1; // +1 pour inclure le dernier arrêt commun
        int endPhase2B = findStopIndex(stopsB, lastCommon) + 1;

        while (indexA < endPhase2A && indexB < endPhase2B) {
            // On vérifie que c'est bien le même arrêt physiquement
            MarkerStop stopA = stopsA.get(indexA);
            MarkerStop stopB = stopsB.get(indexB);

            if (stopA.getStopRef().equals(stopB.getStopRef())) {
                MarkerStop commonStop = new MarkerStop(stopA);
                commonStop.setVehicle(um); // Appartient à l'UM global

                // Vérifier si les horaires divergent même s'ils sont dans la phase commune (séparation au départ du quai)
                Time depA = stopA.getDepartureTime() != null ? stopA.getDepartureTime() : stopA.getArrivalTime();
                Time depB = stopB.getDepartureTime() != null ? stopB.getDepartureTime() : stopB.getArrivalTime();

                if (depA != null && depB != null && !depA.equals(depB)) {
                    // Les trains se séparent ici !
                    displayRows.add(TrainUmTimelineRow.createSideBySideStop(stopA, stopB));
                } else {
                    displayRows.add(TrainUmTimelineRow.createCommonStop(commonStop));
                }
            } else {
                // Securité si désynchronisation inattendue
                displayRows.add(TrainUmTimelineRow.createSideBySideStop(stopA, stopB));
            }
            indexA++;
            indexB++;
        }

        // ==========================================
        // PHASE 3 : APRÈS LE SPLIT (Côte à côte)
        // ==========================================
        if (indexA < stopsA.size() || indexB < stopsB.size()) {
            // Insertion du graphique de séparation
            displayRows.add(TrainUmTimelineRow.createSplitGraphic());
            zipStopsSideBySide(stopsA, stopsB, indexA, stopsA.size(), indexB, stopsB.size(), displayRows);
        }

        // Mettre à jour les flags IsFirst et IsLast pour l'affichage de la timeline (vLineTop/Bottom)
        if (!displayRows.isEmpty()) {
            // Cherche le premier vrai arrêt (ignore les graphismes)
            for (TrainUmTimelineRow row : displayRows) {
                if (row.getType() == TimelineRowType.COMMON || row.getType() == TimelineRowType.SIDE_BY_SIDE) {
                    row.setFirstPosition(true);
                    break;
                }
            }
            // Cherche le dernier vrai arrêt
            for (int r = displayRows.size() - 1; r >= 0; r--) {
                TrainUmTimelineRow row = displayRows.get(r);
                if (row.getType() == TimelineRowType.COMMON || row.getType() == TimelineRowType.SIDE_BY_SIDE) {
                    row.setLastPosition(true);
                    break;
                }
            }
        }

        return displayRows;
    }

    /**
     * Fonction utilitaire pour trouver l'index d'un arrêt dans une liste.
     */
    private static int findStopIndex(List<MarkerStop> stops, String stopRef) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getStopRef().equals(stopRef)) return i;
        }
        return stops.size();
    }

    /**
     * Aligne côte à côte les arrêts exclusifs de deux listes.
     */
    private static void zipStopsSideBySide(List<MarkerStop> stopsA, List<MarkerStop> stopsB,
                                           int startA, int endA, int startB, int endB,
                                           List<TrainUmTimelineRow> displayRows) {
        int i = startA;
        int j = startB;
        while (i < endA || j < endB) {
            MarkerStop stopA = (i < endA) ? stopsA.get(i) : null;
            MarkerStop stopB = (j < endB) ? stopsB.get(j) : null;
            displayRows.add(TrainUmTimelineRow.createSideBySideStop(stopA, stopB));
            if (i < endA) i++;
            if (j < endB) j++;
        }
    }

    public static String getDestination(MarkerStandardized um) {
        if (!um.isUm()) return um.getDestination();

        String destA = um.getUmA().getDestination();
        String destB = um.getUmB().getDestination();

        if (destA != null && destA.equals(destB)) {
            return destA;
        } else {
            return destA + " / " + destB;
        }
    }
}
