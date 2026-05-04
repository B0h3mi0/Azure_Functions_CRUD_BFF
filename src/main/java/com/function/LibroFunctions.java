package com.function;

import com.config.MySqlConfig;
import com.dto.LibroDTO;
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

public class LibroFunctions {

    @FunctionName("CrearLibro")
    public HttpResponseMessage crearLibro(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, route = "libros", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<LibroDTO>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando creación de libro...");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El cuerpo de la petición es obligatorio\"}")
                    .build();
        }

        LibroDTO nuevoLibro = request.getBody().get();
        context.getLogger().info("Creando libro con id_libro: " + nuevoLibro.getId_libro());

        String sql = "INSERT INTO LIBROS (id_libro, titulo, escritor, genero, anio, premisa, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, nuevoLibro.getId_libro());
            stmt.setString(2, nuevoLibro.getTitulo());
            stmt.setString(3, nuevoLibro.getEscritor());
            stmt.setString(4, nuevoLibro.getGenero());
            stmt.setInt(5, nuevoLibro.getAnio());
            stmt.setString(6, nuevoLibro.getPremisa());
            stmt.setInt(7, nuevoLibro.getStock());

            stmt.executeUpdate();

        } catch (SQLException e) {
            context.getLogger().severe("Fallo en la base de datos: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno procesando el libro\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.CREATED)
                .header("Content-Type", "application/json")
                .body(nuevoLibro)
                .build();
    }

    @FunctionName("ObtenerTodosLosLibros")
    public HttpResponseMessage obtenerTodosLosLibros(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, route = "libros", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Obteniendo todos los libros desde MySQL...");
        List<LibroDTO> listaLibros = new ArrayList<>();
        String sql = "SELECT id_libro, titulo, escritor, genero, anio, premisa, stock FROM LIBROS";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LibroDTO dto = LibroDTO.builder()
                        .id_libro(rs.getString("id_libro"))
                        .titulo(rs.getString("titulo"))
                        .escritor(rs.getString("escritor"))
                        .genero(rs.getString("genero"))
                        .anio(rs.getInt("anio"))
                        .premisa(rs.getString("premisa"))
                        .stock(rs.getInt("stock"))
                        .build();
                listaLibros.add(dto);
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error listando libros: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error obteniendo los datos\"}")
                    .build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(listaLibros)
                .build();
    }

    @FunctionName("ObtenerLibroPorId")
    public HttpResponseMessage obtenerLibroPorId(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET }, route = "libros/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Buscando libro con ID: " + id);
        String sql = "SELECT id_libro, titulo, escritor, genero, anio, premisa, stock FROM LIBROS WHERE id_libro = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LibroDTO libroEncontrado = LibroDTO.builder()
                            .id_libro(rs.getString("id_libro"))
                            .titulo(rs.getString("titulo"))
                            .escritor(rs.getString("escritor"))
                            .genero(rs.getString("genero"))
                            .anio(rs.getInt("anio"))
                            .premisa(rs.getString("premisa"))
                            .stock(rs.getInt("stock"))
                            .build();

                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(libroEncontrado)
                            .build();
                } else {
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("{\"error\": \"Libro no encontrado\"}")
                            .build();
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error buscando libro: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @FunctionName("ActualizarLibro")
    public HttpResponseMessage actualizarLibro(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.PUT }, route = "libros/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<LibroDTO>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Actualizando libro con ID: " + id);

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Se requieren los datos a actualizar\"}")
                    .build();
        }

        LibroDTO libroAActualizar = request.getBody().get();
        libroAActualizar.setId_libro(id);

        String sql = "UPDATE LIBROS SET titulo = ?, escritor = ?, genero = ?, anio = ?, premisa = ?, stock = ? WHERE id_libro = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, libroAActualizar.getTitulo());
            stmt.setString(2, libroAActualizar.getEscritor());
            stmt.setString(3, libroAActualizar.getGenero());
            stmt.setInt(4, libroAActualizar.getAnio());
            stmt.setString(5, libroAActualizar.getPremisa());
            stmt.setInt(6, libroAActualizar.getStock());
            stmt.setString(7, id);

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Libro no encontrado para actualizar\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error actualizando libro: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(libroAActualizar)
                .build();
    }

    @FunctionName("EliminarLibro")
    public HttpResponseMessage eliminarLibro(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.DELETE }, route = "libros/{id}", authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext context) {

        context.getLogger().info("Eliminando libro con ID: " + id);

        String sql = "DELETE FROM LIBROS WHERE id_libro = ?";

        try (Connection connection = MySqlConfig.getConnection();
                PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, id);
            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("{\"error\": \"Libro no encontrado\"}")
                        .build();
            }

        } catch (SQLException e) {
            context.getLogger().severe("Error eliminando libro: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return request.createResponseBuilder(HttpStatus.NO_CONTENT).build();
    }
}
