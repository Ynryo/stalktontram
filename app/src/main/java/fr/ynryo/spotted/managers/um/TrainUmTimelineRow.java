package fr.ynryo.spotted.managers.um;

import fr.ynryo.spotted.genericMarkerDatas.MarkerStop;

public class TrainUmTimelineRow {
    private TimelineRowType type;
    private MarkerStop stopA; // Utilisé comme arrêt commun si type == COMMON
    private MarkerStop stopB;
    private boolean isFirstPosition = false;
    private boolean isLastPosition = false;

    private TrainUmTimelineRow(TimelineRowType type, MarkerStop stopA, MarkerStop stopB, boolean isFirstPosition, boolean isLastPosition) {
        this.type = type;
        this.stopA = stopA;
        this.stopB = stopB;
        this.isFirstPosition = isFirstPosition;
        this.isLastPosition = isLastPosition;
    }

    public static TrainUmTimelineRow createCommonStop(MarkerStop stop) {
        return new TrainUmTimelineRow(TimelineRowType.COMMON, stop, null, false, false);
    }

    public static TrainUmTimelineRow createSideBySideStop(MarkerStop stopA, MarkerStop stopB) {
        return new TrainUmTimelineRow(TimelineRowType.SIDE_BY_SIDE, stopA, stopB, false, false);
    }

    public static TrainUmTimelineRow createMergeGraphic() {
        return new TrainUmTimelineRow(TimelineRowType.MERGE_GRAPHIC, null, null, false, false);
    }

    public static TrainUmTimelineRow createSplitGraphic() {
        return new TrainUmTimelineRow(TimelineRowType.SPLIT_GRAPHIC, null, null, false, false);
    }

    public MarkerStop getStopA() {
        return stopA;
    }

    public MarkerStop getStopB() {
        return stopB;
    }

    public TimelineRowType getType() {
        return type;
    }

    public boolean isFirstPosition() {
        return isFirstPosition;
    }

    public void setFirstPosition(boolean firstPosition) {
        isFirstPosition = firstPosition;
    }

    public boolean isLastPosition() {
        return isLastPosition;
    }

    public void setLastPosition(boolean lastPosition) {
        isLastPosition = lastPosition;
    }
}