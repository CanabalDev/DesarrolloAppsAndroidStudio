package com.example.desarrooapps;

final class FarmAreaMath {
    private FarmAreaMath() {
    }

    static Double parse(String text) {
        try {
            double value = Double.parseDouble(text.trim().replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static boolean hasMoreThanTwoDecimals(String text) {
        String normalized = text.trim().replace(',', '.');
        int decimalIndex = normalized.indexOf('.');
        return decimalIndex >= 0 && normalized.length() - decimalIndex - 1 > 2;
    }

    static double squareMetersFromHectares(double hectares) {
        return roundToTwo(hectares * 10000.0);
    }

    static double hectaresFromSquareMeters(double squareMeters) {
        return roundToTwo(squareMeters / 10000.0);
    }

    static boolean areCompatible(double hectares, double squareMeters) {
        return nearlyEqual(hectares, roundToTwo(squareMeters / 10000.0))
                || nearlyEqual(squareMeters, roundToTwo(hectares * 10000.0));
    }

    private static double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static boolean nearlyEqual(double first, double second) {
        return Math.abs(first - second) < 0.000001;
    }
}
