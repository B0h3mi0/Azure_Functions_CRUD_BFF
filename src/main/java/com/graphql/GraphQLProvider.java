package com.graphql;

import com.config.MySqlConfig;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLProvider {

    private static GraphQL graphQL;

    static {
        init();
    }

    private static void init() {
        String schema = "type Libro {\n" +
                "  id_libro: ID!\n" +
                "  titulo: String!\n" +
                "  escritor: String!\n" +
                "  genero: String!\n" +
                "  anio: Int!\n" +
                "  premisa: String\n" +
                "  stock: Int!\n" +
                "}\n" +
                "type Reserva {\n" +
                "  id: ID!\n" +
                "  userId: String!\n" +
                "  libroId: String!\n" +
                "  fechaReserva: String!\n" +
                "  fechaDevolucion: String!\n" +
                "  status: String!\n" +
                "}\n" +
                "type Query {\n" +
                "  libros: [Libro]\n" +
                "  libro(id_libro: ID!): Libro\n" +
                "  reservas: [Reserva]\n" +
                "  reserva(id: ID!): Reserva\n" +
                "}\n" +
                "type Mutation {\n" +
                "  crearLibro(id_libro: ID!, titulo: String!, escritor: String!, genero: String!, anio: Int!, premisa: String, stock: Int!): Libro\n"
                +
                "  actualizarLibro(id_libro: ID!, titulo: String, escritor: String, genero: String, anio: Int, premisa: String, stock: Int): Libro\n"
                +
                "  eliminarLibro(id_libro: ID!): Boolean\n" +
                "  crearReserva(userId: String!, libroId: String!, fechaReserva: String!, fechaDevolucion: String!, status: String!): Reserva\n"
                +
                "  actualizarEstadoReserva(id: ID!, status: String!): Reserva\n" +
                "}";

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = buildWiring();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
        graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    private static RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("libros", env -> getLibros())
                        .dataFetcher("libro", env -> getLibro(env.getArgument("id_libro")))
                        .dataFetcher("reservas", env -> getReservas())
                        .dataFetcher("reserva", env -> getReserva(env.getArgument("id"))))
                .type("Mutation", typeWiring -> typeWiring
                        .dataFetcher("crearLibro", env -> crearLibro(
                                env.getArgument("id_libro"),
                                env.getArgument("titulo"),
                                env.getArgument("escritor"),
                                env.getArgument("genero"),
                                env.getArgument("anio"),
                                env.getArgument("premisa"),
                                env.getArgument("stock")))
                        .dataFetcher("actualizarLibro", env -> actualizarLibro(
                                env.getArgument("id_libro"),
                                env.getArgument("titulo"),
                                env.getArgument("escritor"),
                                env.getArgument("genero"),
                                env.getArgument("anio"),
                                env.getArgument("premisa"),
                                env.getArgument("stock")))
                        .dataFetcher("eliminarLibro", env -> eliminarLibro(env.getArgument("id_libro")))
                        .dataFetcher("crearReserva", env -> crearReserva(
                                env.getArgument("userId"),
                                env.getArgument("libroId"),
                                env.getArgument("fechaReserva"),
                                env.getArgument("fechaDevolucion"),
                                env.getArgument("status")))
                        .dataFetcher("actualizarEstadoReserva", env -> actualizarEstadoReserva(
                                env.getArgument("id"),
                                env.getArgument("status"))))
                .build();
    }

    public static GraphQL getGraphQL() {
        return graphQL;
    }

    // --- LIBROS ---

    private static List<Map<String, Object>> getLibros() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_libro, titulo, escritor, genero, anio, premisa, stock FROM LIBROS";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapLibroResultSet(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener libros: " + e.getMessage(), e);
        }
        return lista;
    }

    private static Map<String, Object> getLibro(String id_libro) {
        String sql = "SELECT id_libro, titulo, escritor, genero, anio, premisa, stock FROM LIBROS WHERE id_libro = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id_libro);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapLibroResultSet(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error BD buscando libro " + id_libro + ": " + e.getMessage(), e);
        }
        return null;
    }

    private static Map<String, Object> crearLibro(String id_libro, String titulo, String escritor,
            String genero, Integer anio, String premisa, Integer stock) {
        String sql = "INSERT INTO LIBROS (id_libro, titulo, escritor, genero, anio, premisa, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id_libro);
            stmt.setString(2, titulo);
            stmt.setString(3, escritor);
            stmt.setString(4, genero);
            stmt.setInt(5, anio);
            stmt.setString(6, premisa);
            stmt.setInt(7, stock);
            stmt.executeUpdate();
            return getLibro(id_libro);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando libro a la BD: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> actualizarLibro(String id_libro, String titulo, String escritor, String genero,
            Integer anio, String premisa, Integer stock) {
        Map<String, Object> existing = getLibro(id_libro);
        if (existing == null)
            throw new RuntimeException("Libro con ID " + id_libro + " no encontrado para actualizar.");

        String utitulo = titulo != null ? titulo : (String) existing.get("titulo");
        String uescritor = escritor != null ? escritor : (String) existing.get("escritor");
        String ugenero = genero != null ? genero : (String) existing.get("genero");
        Integer uanio = anio != null ? anio : (Integer) existing.get("anio");
        String upremisa = premisa != null ? premisa : (String) existing.get("premisa");
        Integer ustock = stock != null ? stock : (Integer) existing.get("stock");

        String sql = "UPDATE LIBROS SET titulo = ?, escritor = ?, genero = ?, anio = ?, premisa = ?, stock = ? WHERE id_libro = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, utitulo);
            stmt.setString(2, uescritor);
            stmt.setString(3, ugenero);
            stmt.setInt(4, uanio);
            stmt.setString(5, upremisa);
            stmt.setInt(6, ustock);
            stmt.setString(7, id_libro);
            stmt.executeUpdate();
            return getLibro(id_libro);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando libro BD: " + e.getMessage(), e);
        }
    }

    private static boolean eliminarLibro(String id_libro) {
        String sql = "DELETE FROM LIBROS WHERE id_libro = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id_libro);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error BD eliminando libro: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> mapLibroResultSet(ResultSet rs) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id_libro", rs.getString("id_libro"));
        map.put("titulo", rs.getString("titulo"));
        map.put("escritor", rs.getString("escritor"));
        map.put("genero", rs.getString("genero"));
        map.put("anio", rs.getInt("anio"));
        map.put("premisa", rs.getString("premisa"));
        map.put("stock", rs.getInt("stock"));
        return map;
    }

    // --- RESERVAS ---

    private static List<Map<String, Object>> getReservas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, user_id, libro_id, fecha_reserva, fecha_devolucion, status FROM RESERVAS";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapReservaResultSet(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener reservas: " + e.getMessage(), e);
        }
        return lista;
    }

    private static Map<String, Object> getReserva(String id) {
        String sql = "SELECT id, user_id, libro_id, fecha_reserva, fecha_devolucion, status FROM RESERVAS WHERE id = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservaResultSet(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error BD buscando reserva " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    private static Map<String, Object> crearReserva(String userId, String libroId, String fechaReserva,
            String fechaDevolucion, String status) {
        String id = java.util.UUID.randomUUID().toString();
        String sql = "INSERT INTO RESERVAS (id, user_id, libro_id, fecha_reserva, fecha_devolucion, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, userId);
            stmt.setString(3, libroId);
            stmt.setString(4, fechaReserva);
            stmt.setString(5, fechaDevolucion);
            stmt.setString(6, status);
            stmt.executeUpdate();
            return getReserva(id);
        } catch (Exception e) {
            throw new RuntimeException("Error insertando reserva a la BD: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> actualizarEstadoReserva(String id, String status) {
        Map<String, Object> existing = getReserva(id);
        if (existing == null)
            throw new RuntimeException("Reserva con ID " + id + " no encontrada para actualizar.");

        String sql = "UPDATE RESERVAS SET status = ? WHERE id = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, id);
            stmt.executeUpdate();
            return getReserva(id);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando estado reserva BD: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> mapReservaResultSet(ResultSet rs) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", rs.getString("id"));
        map.put("userId", rs.getString("user_id"));
        map.put("libroId", rs.getString("libro_id"));
        map.put("fechaReserva", rs.getString("fecha_reserva"));
        map.put("fechaDevolucion", rs.getString("fecha_devolucion"));
        map.put("status", rs.getString("status"));
        return map;
    }
}
