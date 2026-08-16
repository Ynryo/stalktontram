package fr.ynryo.stalktonbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import fr.ynryo.stalktonbus.apiResponsesPOJO.markers.BusTrackerMarkerData;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.stalktonbus.genericMarkerDatas.MarkerType;

public class BusTrackerMarkerDataStandardizedTest {

    @Test
    public void testCreateNewMarkerFromWithNullOrEmptyFieldsDoesNotThrow() {
        BusTrackerMarkerData busTrackerMarkerData = new BusTrackerMarkerData();
        MarkerType type = MarkerType.guestFromMarkerId(busTrackerMarkerData.getId());

        MarkerDataStandardized standardized = MarkerDataStandardized.createNewMarkerFrom(busTrackerMarkerData, type);
        assertNotNull(standardized);
        assertNull(standardized.getId());
        assertNull(standardized.getLineNumber());
        assertEquals("", standardized.getNetworkRef());
    }

    @Test
    public void testDefaultConstructorDoesNotThrowOnGettersAndSetters() {
        MarkerDataStandardized marker = new MarkerDataStandardized();
        assertNotNull(marker.getId());
        assertEquals(0.0, marker.getLatitude(), 0.0001);
        assertEquals(0.0, marker.getLongitude(), 0.0001);
        assertEquals(0.0f, marker.getBearing(), 0.0001f);
        assertEquals("#424242", marker.getFillColor());
        assertEquals("#FFFFFF", marker.getTextColor());

        marker.setId("SNCF::12345");
        assertEquals("SNCF::12345", marker.getId());
        marker.setLatitude(48.8566);
        assertEquals(48.8566, marker.getLatitude(), 0.0001);
    }
}
