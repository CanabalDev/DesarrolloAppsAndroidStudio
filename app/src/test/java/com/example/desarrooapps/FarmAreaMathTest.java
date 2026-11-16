package com.example.desarrooapps;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FarmAreaMathTest {
    @Test
    public void calculatesSquareMetersFromHectaresToTwoDecimals() {
        assertEquals(12300.0, FarmAreaMath.squareMetersFromHectares(1.23), 0.000001);
    }

    @Test
    public void calculatesRoundedHectaresFromSquareMeters() {
        assertEquals(1.23, FarmAreaMath.hectaresFromSquareMeters(12345.0), 0.000001);
    }

    @Test
    public void acceptsEitherDocumentedRoundedConversionDirection() {
        assertTrue(FarmAreaMath.areCompatible(1.23, 12345.0));
        assertTrue(FarmAreaMath.areCompatible(1.23, 12300.0));
        assertFalse(FarmAreaMath.areCompatible(1.5, 12345.0));
    }

    @Test
    public void parsesLocaleDecimalSeparatorAndLimitsPrecision() {
        assertEquals(1.25, FarmAreaMath.parse("1,25"), 0.000001);
        assertTrue(FarmAreaMath.hasMoreThanTwoDecimals("1.234"));
        assertFalse(FarmAreaMath.hasMoreThanTwoDecimals("1,23"));
        assertNull(FarmAreaMath.parse("not-a-number"));
    }
}
