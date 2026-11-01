package com.kasimarket.kasimarket_api.location;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Checks the distance and time maths with places whose distances we know.
class GeoTest {

    // Randburg town centre and Johannesburg city centre (GPS coordinates).
    private static final double RANDBURG_LAT = -26.0936;
    private static final double RANDBURG_LON = 28.0064;
    private static final double JOBURG_LAT = -26.2041;
    private static final double JOBURG_LON = 28.0473;

    @Test
    void theSamePointIsZeroKmAway() {
        assertThat(Geo.distanceKm(RANDBURG_LAT, RANDBURG_LON, RANDBURG_LAT, RANDBURG_LON))
                .isEqualTo(0.0);
    }

    @Test
    void randburgToJohannesburgIsAboutThirteenKm() {
        double km = Geo.distanceKm(RANDBURG_LAT, RANDBURG_LON, JOBURG_LAT, JOBURG_LON);
        assertThat(km).isBetween(12.0, 14.0);
    }

    @Test
    void theDistanceIsTheSameInBothDirections() {
        double there = Geo.distanceKm(RANDBURG_LAT, RANDBURG_LON, JOBURG_LAT, JOBURG_LON);
        double back = Geo.distanceKm(JOBURG_LAT, JOBURG_LON, RANDBURG_LAT, RANDBURG_LON);
        assertThat(there).isEqualTo(back);
    }

    @Test
    void randburgToCapeTownIsAboutTwelveHundredAndFiftyKm() {
        double km = Geo.distanceKm(RANDBURG_LAT, RANDBURG_LON, -33.9249, 18.4241);
        assertThat(km).isBetween(1200.0, 1300.0);
    }

    @Test
    void approximateDistancesRoundToTheNearestHalfKmAndNeverBelowHalf() {
        assertThat(Geo.approximateKm(0.1)).isEqualTo(0.5);   // very close shows as 0.5
        assertThat(Geo.approximateKm(2.2)).isEqualTo(2.0);
        assertThat(Geo.approximateKm(2.3)).isEqualTo(2.5);
        assertThat(Geo.approximateKm(9.9)).isEqualTo(10.0);
    }

    @Test
    void etaAddsRoadDistanceAndRoundsUp() {
        // 5 km in a straight line becomes 6.5 km of road. At 25 km/h that is 15.6 minutes,
        // which we round UP to 16, so the client is never promised too little time.
        assertThat(Geo.etaMinutes(5.0, 25.0, 1.3)).isEqualTo(16);
    }

    @Test
    void etaIsNeverLessThanOneMinute() {
        // Even a worker 100 metres away needs at least "1 minute".
        assertThat(Geo.etaMinutes(0.1, 25.0, 1.3)).isEqualTo(1);
        assertThat(Geo.etaMinutes(0.0, 25.0, 1.3)).isEqualTo(1);
    }

    @Test
    void aFasterAverageSpeedGivesAShorterTime() {
        assertThat(Geo.etaMinutes(10.0, 50.0, 1.3)).isLessThan(Geo.etaMinutes(10.0, 25.0, 1.3));
    }
}