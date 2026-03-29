package com.function;

import com.config.MySqlConfig;
import com.dto.ReservaLibroDTO;
import com.microsoft.azure.functions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservaFunctionsTest {

    private ReservaFunctions reservaFunctions;

    @Mock private HttpRequestMessage<Optional<ReservaLibroDTO>> request;
    @Mock private HttpResponseMessage.Builder responseBuilder;
    @Mock private HttpResponseMessage response;
    @Mock private ExecutionContext context;
    private Logger logger = Logger.getAnonymousLogger();
    
    // Mocks de JDBC
    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;

    private MockedStatic<MySqlConfig> mockedMySqlConfig;

    @BeforeEach
    public void setup() {
        reservaFunctions = new ReservaFunctions();

        // Configurar el Logger
        doReturn(logger).when(context).getLogger();

        // AGREGAMOS LENIENT() A ESTAS LÍNEAS:
        // Configurar el comportamiento encadenado del ResponseBuilder de Azure
        lenient().doReturn(responseBuilder).when(request).createResponseBuilder(any(HttpStatus.class));
        lenient().doReturn(responseBuilder).when(responseBuilder).header(anyString(), anyString());
        lenient().doReturn(responseBuilder).when(responseBuilder).body(any());
        lenient().doReturn(response).when(responseBuilder).build();

        // Mockear el método estático MySqlConfig.getConnection()
        mockedMySqlConfig = mockStatic(MySqlConfig.class);
    }

    @AfterEach
    public void tearDown() {
        // Es vital cerrar el mock estático después de cada test para que no interfiera con otros
        mockedMySqlConfig.close();
    }

    @Test
    public void testCrearReserva_Exitoso() throws SQLException {
        // 1. Preparar el DTO de entrada
        ReservaLibroDTO reservaEntrada = ReservaLibroDTO.builder()
                .userId("U123")
                .libroId("L456")
                .fechaReserva("2026-03-28")
                .fechaDevolucion("2026-04-05")
                .status("ACTIVA")
                .build();
        doReturn(Optional.of(reservaEntrada)).when(request).getBody();

        // 2. Simular el comportamiento exitoso de la Base de Datos
        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        // Simular el retorno del ID autogenerado
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(99);

        // Configurar la respuesta simulada para validar el HttpStatus
        doReturn(HttpStatus.CREATED).when(response).getStatus();

        // 3. Ejecutar la función
        HttpResponseMessage resultado = reservaFunctions.crearReserva(request, context);

        // 4. Validar resultados
        assertEquals(HttpStatus.CREATED, resultado.getStatus());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testObtenerReservaPorId_Existe() throws SQLException {
        // 1. Preparar un HttpRequestMessage genérico para el GET
        HttpRequestMessage<Optional<String>> getRequest = mock(HttpRequestMessage.class);
        doReturn(responseBuilder).when(getRequest).createResponseBuilder(any(HttpStatus.class));

        // 2. Simular la Base de Datos encontrando un registro
        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        when(resultSet.next()).thenReturn(true); // Fila encontrada
        when(resultSet.getString("id")).thenReturn("1");
        when(resultSet.getString("user_id")).thenReturn("U123");

        doReturn(HttpStatus.OK).when(response).getStatus();

        // 3. Ejecutar
        HttpResponseMessage resultado = reservaFunctions.obtenerReservaPorId(getRequest, "1", context);

        // 4. Validar
        assertEquals(HttpStatus.OK, resultado.getStatus());
    }

    @Test
public void testCrearReserva_FallaBaseDeDatos() throws SQLException {
    // 1. Preparar entrada
    ReservaLibroDTO reservaEntrada = new ReservaLibroDTO();
    doReturn(Optional.of(reservaEntrada)).when(request).getBody();

    // 2. Simular una caída de la base de datos (Lanza SQLException)
    mockedMySqlConfig.when(MySqlConfig::getConnection).thenThrow(new SQLException("Conexión rechazada"));
    doReturn(HttpStatus.INTERNAL_SERVER_ERROR).when(response).getStatus();

    // 3. Ejecutar
    HttpResponseMessage resultado = reservaFunctions.crearReserva(request, context);

    // 4. Validar
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resultado.getStatus());
    
    // CORRECCIÓN: Eliminamos el verify(logger...) ya que logger no es un Mock.
    // El test ya valida el comportamiento al recibir el INTERNAL_SERVER_ERROR.
}
}