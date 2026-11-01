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
}