package com.function;

import com.config.MySqlConfig;
import com.dto.ReservaLibroDTO;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservaFunctions {

    @FunctionName("CrearReserva")
    public HttpResponseMessage crearReserva(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, route = "reservas", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<ReservaLibroDTO>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando creación de reserva...");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El cuerpo de la petición es obligatorio\"}")
                    .build();
        }

        ReservaLibroDTO nuevaReserva = request.getBody().get();
        context.getLogger().info("Creando reserva para el usuario: " + nuevaReserva.getUserId());

        String sql = "INSERT INTO RESERVAS (user_id, libro_id, fecha_reserva, fecha_devolucion, status) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nuevaReserva.getUserId());
            stmt.setString(2, nuevaReserva.getLibroId());
            stmt.setString(3, nuevaReserva.getFechaReserva());
            stmt.setString(4, nuevaReserva.getFechaDevolucion());
            stmt.setString(5, nuevaReserva.getStatus());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    nuevaReserva.setId(String.valueOf(generatedKeys.getInt(1)));
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Fallo en la base de datos: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno procesando la reserva\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(nuevaReserva)
                .build();
    }

    @FunctionName("ObtenerTodasLasReservas")
    public HttpResponseMessage obtenerTodasLasReservas(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, route = "reservas", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Obteniendo todas las reservas desde MySQL...");
        List<ReservaLibroDTO> listaReservas = new ArrayList<>();
        String sql = "SELECT id, user_id, libro_id, fecha_reserva, fecha_devolucion, status FROM RESERVAS";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ReservaLibroDTO dto = ReservaLibroDTO.builder()
                        .id(rs.getString("id"))
                        .userId(rs.getString("user_id"))
                        .libroId(rs.getString("libro_id"))
                        .fechaReserva(rs.getString("fecha_reserva"))
                        .fechaDevolucion(rs.getString("fecha_devolucion"))
                        .status(rs.getString("status"))
                        .build();
                listaReservas.add(dto);
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error listando reservas: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error obteniendo los datos\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(listaReservas)
                .build();
    }

    @FunctionName("ObtenerReservaPorId")
    public HttpResponseMessage obtenerReservaPorId(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, route = "reservas/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Buscando reserva con ID: " + id);
        String sql = "SELECT id, user_id, libro_id, fecha_reserva, fecha_devolucion, status FROM RESERVAS WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ReservaLibroDTO reservaEncontrada = ReservaLibroDTO.builder()
                            .id(rs.getString("id"))
                            .userId(rs.getString("user_id"))
                            .libroId(rs.getString("libro_id"))
                            .fechaReserva(rs.getString("fecha_reserva"))
                            .fechaDevolucion(rs.getString("fecha_devolucion"))
                            .status(rs.getString("status"))
                            .build();

                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(reservaEncontrada)
                            .build();
                } else {
                    // Si el ResultSet está vacío, no existe la reserva
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("{\"error\": \"Reserva no encontrada\"}")
                            .build();
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error buscando reserva: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @FunctionName("ActualizarReserva")
    public HttpResponseMessage actualizarReserva(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.PUT }, route = "reservas/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<ReservaLibroDTO>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Actualizando reserva con ID: " + id);

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Se requieren los datos a actualizar\"}")
                    .build();
        }

        ReservaLibroDTO reservaAActualizar = request.getBody().get();
        reservaAActualizar.setId(id);

        String sql = "UPDATE RESERVAS SET user_id = ?, libro_id = ?, fecha_reserva = ?, fecha_devolucion = ?, status = ? WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, reservaAActualizar.getUserId());
            stmt.setString(2, reservaAActualizar.getLibroId());
            stmt.setString(3, reservaAActualizar.getFechaReserva());
            stmt.setString(4, reservaAActualizar.getFechaDevolucion());
            stmt.setString(5, reservaAActualizar.getStatus());
            stmt.setString(6, id);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Reserva no encontrada para actualizar\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error actualizando reserva: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(reservaAActualizar)
                .build();
    }

    @FunctionName("EliminarReserva")
    public HttpResponseMessage eliminarReserva(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, route = "reservas/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Eliminando reserva con ID: " + id);

        String sql = "DELETE FROM RESERVAS WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);
            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Reserva no encontrada\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error eliminando reserva: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}