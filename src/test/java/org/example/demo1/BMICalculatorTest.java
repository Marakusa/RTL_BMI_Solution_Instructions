package org.example.demo1;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BMICalculatorTest {

    private static Locale originalFormatLocale;

    @BeforeAll
    static void useUsNumberFormat() {
        originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
    }

    @AfterAll
    static void restoreNumberFormat() {
        Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
    }

    @Test
    void convertsCentimetresToMetres() {
        assertEquals(1.75, BMICalculator.centimetresToMetres(175), 1e-9);
        assertEquals(0.0, BMICalculator.centimetresToMetres(0), 1e-9);
    }

    @Test
    void calculatesBmi() {
        assertEquals(22.857, BMICalculator.calculate(70, 1.75), 1e-3);
        assertEquals(25.0, BMICalculator.calculate(100, 2.0), 1e-9);
    }

    @Test
    void zeroHeightGivesInfinity() {
        assertTrue(Double.isInfinite(BMICalculator.calculate(70, 0)));
    }

    @Test
    void formatsWithTwoDecimals() {
        assertEquals("22.86", BMICalculator.format(22.857142));
        assertEquals("25.00", BMICalculator.format(25));
        assertEquals("0.50", BMICalculator.format(0.5));
    }

    @Test
    void detectsRightToLeftLanguages() {
        assertTrue(BMICalculator.isRightToLeft(Locale.of("fa", "IR")));
        assertTrue(BMICalculator.isRightToLeft(Locale.of("ur", "PA")));
        assertTrue(BMICalculator.isRightToLeft(Locale.of("ar")));
        assertTrue(BMICalculator.isRightToLeft(Locale.of("he")));
    }

    @Test
    void detectsLeftToRightLanguages() {
        assertFalse(BMICalculator.isRightToLeft(Locale.US));
        assertFalse(BMICalculator.isRightToLeft(Locale.FRANCE));
        assertFalse(BMICalculator.isRightToLeft(Locale.of("vi", "VI")));
    }
}
