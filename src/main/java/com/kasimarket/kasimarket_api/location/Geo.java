package com.kasimarket.kasimarket_api.location;

// Maths for distances between two GPS positions. No map service needed.
// "final" and the private constructor mean nobody can create a Geo object:
// it is only a toolbox of ready-made calculations.
public final class Geo {

    // The Earth's average radius in kilometres.
    private static final double EARTH_RADIUS_KM = 6371.0;

    private Geo() {
    }

    // The straight-line distance in km between two points on the Earth.
    // This is the "Haversine formula". The Earth is round, so we cannot just
    // use Pythagoras on the numbers: we have to follow the curve.
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    // Makes a distance "approximate" for privacy: rounded to the nearest half km,
    // and never less than 0.5 km. A worker sees "2.5 km", never the exact position.
    public static double approximateKm(double km) {
        return Math.max(0.5, Math.round(km * 2) / 2.0);
    }

    // NEW: estimates the travel time in whole minutes.
    // Roads are longer than a straight line, so we multiply the distance by a road factor
    // (for example 1.3), then divide by the average speed. We always round UP, so a client
    // is never told "3 minutes" when it will really take 3 minutes and 40 seconds.
    // The answer is never less than 1 minute.
    public static int etaMinutes(double straightKm, double speedKmh, double roadFactor) {
        double roadKm = straightKm * roadFactor;
        double minutes = roadKm / speedKmh * 60.0;
        return (int) Math.max(1, Math.ceil(minutes));
    }
}