package com.function;

import com.config.MySqlConfig;
import com.dto.UserDTO;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsersFunction {

    @FunctionName("CrearUsuario")
    public HttpResponseMessage crearUsuario(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "users",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<UserDTO>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando creación de usuario...");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El cuerpo de la petición es obligatorio para crear un usuario\"}")
                    .build();
        }

        UserDTO nuevoUsuario = request.getBody().get();
        context.getLogger().info("Creando usuario con email: " + nuevoUsuario.getEmail());

        String sql = "INSERT INTO USUARIOS (name, email) VALUES (?, ?)";

        try (Connection connection = MySqlConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, nuevoUsuario.getName());
            stmt.setString(2, nuevoUsuario.getEmail());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    nuevoUsuario.setId(String.valueOf(generatedKeys.getInt(1)));
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Fallo en la base de datos: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno creando el usuario\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(nuevoUsuario)
                .build();
    }

    @FunctionName("ObtenerTodosLosUsuarios")
    public HttpResponseMessage obtenerTodosLosUsuarios(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "users",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Obteniendo todos los usuarios...");

        List<UserDTO> listaUsuarios = new ArrayList<>();
        String sql = "SELECT id, name, email FROM USUARIOS";

        try (Connection connection = MySqlConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UserDTO dto = UserDTO.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .build();
                listaUsuarios.add(dto);
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error listando usuarios: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error obteniendo los datos\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(listaUsuarios)
                .build();
    }

    @FunctionName("ObtenerUsuarioPorId")
    public HttpResponseMessage obtenerUsuarioPorId(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "users/{id}",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Buscando usuario con ID: " + id);

        String sql = "SELECT id, name, email FROM usuarios WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UserDTO usuarioEncontrado = UserDTO.builder()
                            .id(rs.getString("id"))
                            .name(rs.getString("name"))
                            .email(rs.getString("email"))
                            .build();

                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(usuarioEncontrado)
                            .build();
                } else {
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("{\"error\": \"Usuario no encontrado\"}")
                            .build();
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error buscando usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @FunctionName("ActualizarUsuario")
    public HttpResponseMessage actualizarUsuario(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.PUT},
                    route = "users/{id}",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<UserDTO>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Actualizando usuario con ID: " + id);

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Se requieren los datos a actualizar\"}")
                    .build();
        }

        UserDTO usuarioAActualizar = request.getBody().get();
        usuarioAActualizar.setId(id);

        String sql = "UPDATE USUARIOS SET name = ?, email = ? WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, usuarioAActualizar.getName());
            stmt.setString(2, usuarioAActualizar.getEmail());
            stmt.setString(3, id);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Usuario no encontrado para actualizar\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error actualizando usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(usuarioAActualizar)
                .build();
    }

    @FunctionName("EliminarUsuario")
    public HttpResponseMessage eliminarUsuario(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.DELETE},
                    route = "users/{id}",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Eliminando usuario con ID: " + id);

        String sql = "DELETE FROM USUARIOS WHERE id = ?";

        try (Connection connection = MySqlConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Usuario no encontrado\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error eliminando usuario: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}