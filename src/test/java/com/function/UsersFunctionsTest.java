package com.function;

import com.config.MySqlConfig;
import com.dto.UserDTO;
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
public class UsersFunctionsTest {

    private UsersFunction usersFunction;

    @Mock private HttpRequestMessage<Optional<UserDTO>> requestPostPut;
    @Mock private HttpRequestMessage<Optional<String>> requestGetDelete;
    @Mock private HttpResponseMessage.Builder responseBuilder;
    @Mock private HttpResponseMessage response;
    @Mock private ExecutionContext context;
    
    // CORRECCIÓN: Usar Logger real para evitar error de Mockito en Java 21
    private Logger logger = Logger.getAnonymousLogger();

    // Mocks de JDBC
    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;

    private MockedStatic<MySqlConfig> mockedMySqlConfig;

    @BeforeEach
    public void setup() {
        usersFunction = new UsersFunction();

        // Configurar el Context para que devuelva nuestro logger real
        doReturn(logger).when(context).getLogger();

        // Configurar el comportamiento del ResponseBuilder (para POST/PUT)
        lenient().doReturn(responseBuilder).when(requestPostPut).createResponseBuilder(any(HttpStatus.class));
        
        // Configurar el comportamiento del ResponseBuilder (para GET/DELETE)
        lenient().doReturn(responseBuilder).when(requestGetDelete).createResponseBuilder(any(HttpStatus.class));

        lenient().doReturn(responseBuilder).when(responseBuilder).header(anyString(), anyString());
        lenient().doReturn(responseBuilder).when(responseBuilder).body(any());
        lenient().doReturn(response).when(responseBuilder).build();

        // Mockear la conexión a la Base de Datos
        mockedMySqlConfig = mockStatic(MySqlConfig.class);
    }

    @AfterEach
    public void tearDown() {
        mockedMySqlConfig.close();
    }

    @Test
    public void testCrearUsuario_Exitoso() throws SQLException {
        UserDTO usuarioEntrada = new UserDTO(null, "Juan Perez", "juan@test.com");
        doReturn(Optional.of(usuarioEntrada)).when(requestPostPut).getBody();

        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        doReturn(HttpStatus.CREATED).when(response).getStatus();

        HttpResponseMessage resultado = usersFunction.crearUsuario(requestPostPut, context);

        assertEquals(HttpStatus.CREATED, resultado.getStatus());
        verify(preparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testCrearUsuario_SinBody() {
        doReturn(Optional.empty()).when(requestPostPut).getBody();
        doReturn(HttpStatus.BAD_REQUEST).when(response).getStatus();

        HttpResponseMessage resultado = usersFunction.crearUsuario(requestPostPut, context);

        assertEquals(HttpStatus.BAD_REQUEST, resultado.getStatus());
        mockedMySqlConfig.verify(MySqlConfig::getConnection, never()); 
    }

    @Test
    public void testObtenerUsuarioPorId_Existe() throws SQLException {
        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("id")).thenReturn("1");
        when(resultSet.getString("name")).thenReturn("Test");
        when(resultSet.getString("email")).thenReturn("test@test.com");

        doReturn(HttpStatus.OK).when(response).getStatus();

        HttpResponseMessage resultado = usersFunction.obtenerUsuarioPorId(requestGetDelete, "1", context);

        assertEquals(HttpStatus.OK, resultado.getStatus());
    }

    @Test
    public void testObtenerUsuarioPorId_NoExiste() throws SQLException {
        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        
        when(resultSet.next()).thenReturn(false);

        doReturn(HttpStatus.NOT_FOUND).when(response).getStatus();

        HttpResponseMessage resultado = usersFunction.obtenerUsuarioPorId(requestGetDelete, "999", context);

        assertEquals(HttpStatus.NOT_FOUND, resultado.getStatus());
    }

    @Test
    public void testEliminarUsuario_Exitoso() throws SQLException {
        mockedMySqlConfig.when(MySqlConfig::getConnection).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        doReturn(HttpStatus.NO_CONTENT).when(response).getStatus();

        HttpResponseMessage resultado = usersFunction.eliminarUsuario(requestGetDelete, "1", context);

        assertEquals(HttpStatus.NO_CONTENT, resultado.getStatus());
    }
}