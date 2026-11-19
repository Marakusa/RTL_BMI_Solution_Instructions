package org.example.demo1;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

/**
 * Controller tests run against real JavaFX controls (not shown on screen).
 * They are skipped when the JavaFX toolkit cannot start, e.g. on a headless machine.
 */
class BMIControllerTest {

    private static boolean fxAvailable;
    private static Locale originalFormatLocale;

    private MockedStatic<LocalizationService> localization;
    private MockedStatic<BMIResultService> results;

    private BMIController controller;
    private Label lblWeight;
    private Label lblHeight;
    private Label lblResult;
    private Label lblLocalTime;
    private Button btnCalculate;
    private TextField tfWeight;
    private TextField tfHeight;
    private VBox rootVBox;

    @BeforeAll
    static void startJavaFx() {
        originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        try {
            Platform.startup(() -> { });
            fxAvailable = true;
        } catch (IllegalStateException alreadyStarted) {
            fxAvailable = true;
        } catch (RuntimeException | LinkageError noToolkit) {
            fxAvailable = false;
        }
    }

    @AfterAll
    static void restoreNumberFormat() {
        Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        assumeTrue(fxAvailable, "JavaFX toolkit not available, skipping controller tests");

        localization = mockStatic(LocalizationService.class);
        results = mockStatic(BMIResultService.class);
        localization.when(() -> LocalizationService.getLocalizedStrings(any(Locale.class))).thenReturn(Map.of(
                "weight", "Weight (kg)",
                "height", "Height (cm)",
                "calculate", "Calculate BMI",
                "result", "Your BMI is",
                "invalid", "Please enter valid numbers",
                "localTime", "Time:"));

        controller = new BMIController();
        lblWeight = inject("lblWeight", new Label());
        lblHeight = inject("lblHeight", new Label());
        lblResult = inject("lblResult", new Label("old result"));
        lblLocalTime = inject("lblLocalTime", new Label());
        btnCalculate = inject("btnCalculate", new Button());
        tfWeight = inject("tfWeight", new TextField());
        tfHeight = inject("tfHeight", new TextField());
        rootVBox = inject("rootVBox", new VBox());
    }

    @AfterEach
    void tearDown() {
        if (localization != null) {
            localization.close();
        }
        if (results != null) {
            results.close();
        }
    }

    @Test
    void initializeAppliesEnglishTexts() throws InterruptedException {
        controller.initialize();
        waitForFxThread();

        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("en", "US")));
        assertEquals("Weight (kg)", lblWeight.getText());
        assertEquals("Height (cm)", lblHeight.getText());
        assertEquals("Calculate BMI", btnCalculate.getText());
        assertEquals("", lblResult.getText());
        assertTrue(lblLocalTime.getText().matches("Time: \\d{2}:\\d{2}:\\d{2}"), lblLocalTime.getText());
        assertEquals(NodeOrientation.LEFT_TO_RIGHT, rootVBox.getNodeOrientation());
        assertEquals("-fx-text-alignment: left;", tfWeight.getStyle());
        assertEquals("-fx-text-alignment: left;", tfHeight.getStyle());
    }

    @Test
    void fallsBackToDefaultTextsWhenTranslationsMissing() {
        localization.when(() -> LocalizationService.getLocalizedStrings(any(Locale.class))).thenReturn(Map.of());

        controller.initialize();

        assertEquals("Weight", lblWeight.getText());
        assertEquals("Height", lblHeight.getText());
        assertEquals("Calculate", btnCalculate.getText());
        assertTrue(lblLocalTime.getText().startsWith("Local Time "), lblLocalTime.getText());
    }

    @Test
    void persianSwitchesToRightToLeft() throws InterruptedException {
        controller.onFAClick(null);
        waitForFxThread();

        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("fa", "IR")));
        assertEquals(NodeOrientation.RIGHT_TO_LEFT, rootVBox.getNodeOrientation());
        assertEquals("-fx-text-alignment: right;", tfWeight.getStyle());
        assertEquals("-fx-text-alignment: right;", tfHeight.getStyle());
    }

    @Test
    void urduSwitchesToRightToLeft() throws InterruptedException {
        controller.onURClick(null);
        waitForFxThread();

        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("ur", "PA")));
        assertEquals(NodeOrientation.RIGHT_TO_LEFT, rootVBox.getNodeOrientation());
    }

    @Test
    void leftToRightLanguagesRestoreOrientation() throws InterruptedException {
        controller.onFAClick(null);
        waitForFxThread();

        controller.onFRClick(null);
        waitForFxThread();
        assertEquals(NodeOrientation.LEFT_TO_RIGHT, rootVBox.getNodeOrientation());

        controller.onVIClick(null);
        controller.onENClick(null);
        waitForFxThread();
        assertEquals(NodeOrientation.LEFT_TO_RIGHT, rootVBox.getNodeOrientation());

        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("fr", "FR")));
        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("vi", "VI")));
        localization.verify(() -> LocalizationService.getLocalizedStrings(Locale.of("en", "US")));
    }

    @Test
    void calculateShowsAndSavesBmi() {
        controller.initialize();
        tfWeight.setText("70");
        tfHeight.setText("175");

        controller.onCalculateClick(null);

        assertEquals("Your BMI is 22.86", lblResult.getText());
        results.verify(() -> BMIResultService.saveResult(eq(70.0), eq(1.75), anyDouble(), eq("en")));
    }

    @Test
    void calculateSavesCurrentLanguage() {
        controller.onFRClick(null);
        tfWeight.setText("80");
        tfHeight.setText("180");

        controller.onCalculateClick(null);

        assertEquals("Your BMI is 24.69", lblResult.getText());
        results.verify(() -> BMIResultService.saveResult(eq(80.0), eq(1.8), anyDouble(), eq("fr")));
    }

    @Test
    void invalidInputShowsErrorAndSavesNothing() {
        controller.initialize();
        tfWeight.setText("abc");
        tfHeight.setText("175");

        controller.onCalculateClick(null);

        assertEquals("Please enter valid numbers", lblResult.getText());
        results.verify(() -> BMIResultService.saveResult(anyDouble(), anyDouble(), anyDouble(), anyString()), never());
    }

    @Test
    void invalidInputUsesDefaultMessageWhenTranslationMissing() {
        localization.when(() -> LocalizationService.getLocalizedStrings(any(Locale.class))).thenReturn(Map.of());
        controller.initialize();
        tfWeight.setText("");
        tfHeight.setText("");

        controller.onCalculateClick(null);

        assertEquals("Invalid input", lblResult.getText());
    }

    private <T> T inject(String fieldName, T value) throws ReflectiveOperationException {
        Field field = BMIController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
        return value;
    }

    /** Waits until everything queued with Platform.runLater has run. */
    private static void waitForFxThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX thread did not respond");
    }
}
