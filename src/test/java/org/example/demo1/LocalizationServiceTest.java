package org.example.demo1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalizationServiceTest {

    private MockedStatic<DriverManager> driverManager;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() throws SQLException {
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        driverManager = mockStatic(DriverManager.class);
        driverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    @AfterEach
    void tearDown() {
        driverManager.close();
    }

    @Test
    void returnsAllRowsForRequestedLanguage() throws SQLException {
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("key")).thenReturn("weight", "height");
        when(resultSet.getString("value")).thenReturn("Poids", "Taille");

        Map<String, String> strings = LocalizationService.getLocalizedStrings(Locale.FRANCE);

        assertEquals(Map.of("weight", "Poids", "height", "Taille"), strings);
        verify(statement).setString(1, "fr");
    }

    @Test
    void returnsEmptyMapWhenLanguageHasNoRows() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        Map<String, String> strings = LocalizationService.getLocalizedStrings(Locale.US);

        assertTrue(strings.isEmpty());
        verify(statement).setString(1, "en");
    }

    @Test
    void returnsEmptyMapWhenDatabaseIsUnavailable() {
        // Create the exception first: SQLException's constructor calls DriverManager.getLogWriter()
        SQLException refused = new SQLException("connection refused");
        driverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenThrow(refused);

        Map<String, String> strings = LocalizationService.getLocalizedStrings(Locale.US);

        assertTrue(strings.isEmpty());
    }

    @Test
    void connectsToConfiguredHostAndDatabase() {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        String envHost = System.getenv("DB_HOST");
        String expectedHost = (envHost == null || envHost.isEmpty()) ? "localhost" : envHost;

        LocalizationService.getLocalizedStrings(Locale.US);

        driverManager.verify(() -> DriverManager.getConnection(url.capture(), anyString(), anyString()));
        assertTrue(url.getValue().startsWith("jdbc:mysql://" + expectedHost + ":3306/bmi_localization"),
                "Unexpected JDBC URL: " + url.getValue());
    }
}
