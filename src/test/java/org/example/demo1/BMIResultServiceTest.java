package org.example.demo1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BMIResultServiceTest {

    private MockedStatic<DriverManager> driverManager;
    private Connection connection;
    private PreparedStatement statement;

    @BeforeEach
    void setUp() throws SQLException {
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);

        driverManager = mockStatic(DriverManager.class);
        driverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
    }

    @AfterEach
    void tearDown() {
        driverManager.close();
    }

    @Test
    void savesResultWithAllParameters() throws SQLException {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

        BMIResultService.saveResult(70.0, 1.75, 22.86, "en");

        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("INSERT INTO bmi_results"));
        verify(statement).setDouble(1, 70.0);
        verify(statement).setDouble(2, 1.75);
        verify(statement).setDouble(3, 22.86);
        verify(statement).setString(4, "en");
        verify(statement).executeUpdate();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void connectsToMysqlUrl() {
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);

        BMIResultService.saveResult(80.0, 1.80, 24.69, "fr");

        driverManager.verify(() -> DriverManager.getConnection(url.capture(), anyString(), anyString()));
        assertTrue(url.getValue().startsWith("jdbc:mysql://"), "Unexpected JDBC URL: " + url.getValue());
    }

    @Test
    void doesNotThrowWhenDatabaseIsUnavailable() throws SQLException {
        // Create the exception first: SQLException's constructor calls DriverManager.getLogWriter()
        SQLException refused = new SQLException("connection refused");
        driverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                .thenThrow(refused);

        assertDoesNotThrow(() -> BMIResultService.saveResult(70.0, 1.75, 22.86, "en"));
        verify(statement, never()).executeUpdate();
    }

    @Test
    void doesNotThrowWhenInsertFails() throws SQLException {
        SQLException tableMissing = new SQLException("table missing");
        when(statement.executeUpdate()).thenThrow(tableMissing);

        assertDoesNotThrow(() -> BMIResultService.saveResult(70.0, 1.75, 22.86, "en"));
        verify(connection).close();
    }
}
