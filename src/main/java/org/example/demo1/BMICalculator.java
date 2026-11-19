package org.example.demo1;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Set;

/**
 * UI-independent BMI and layout-direction logic used by {@link BMIController}.
 */
public final class BMICalculator {

    private static final Set<String> RTL_LANGUAGES = Set.of("fa", "ur", "ar", "he");

    private BMICalculator() {
    }

    /** Converts a height in centimetres to metres. */
    public static double centimetresToMetres(double heightCm) {
        return heightCm / 100.0;
    }

    /** BMI = weight (kg) / height (m)^2. */
    public static double calculate(double weightKg, double heightMetres) {
        return weightKg / (heightMetres * heightMetres);
    }

    /** Formats a BMI value with two decimals using the default locale. */
    public static String format(double bmi) {
        return new DecimalFormat("#0.00").format(bmi);
    }

    /** True for languages written right-to-left (Persian, Urdu, Arabic, Hebrew). */
    public static boolean isRightToLeft(Locale locale) {
        return RTL_LANGUAGES.contains(locale.getLanguage());
    }
}
