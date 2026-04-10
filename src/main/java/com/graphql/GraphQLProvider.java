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
        String schema = "type ReservaLibro {\n" +
                "  id: ID!\n" +
                "  userId: String\n" +
                "  libroId: String\n" +
                "  fechaReserva: String\n" +
                "  fechaDevolucion: String\n" +
                "  status: String\n" +
                "}\n" +
                "type Query {\n" +
                "  reservas: [ReservaLibro]\n" +
                "  reserva(id: ID!): ReservaLibro\n" +
                "}\n" +
                "type Mutation {\n" +
                "  crearReserva(userId: String!, libroId: String!, fechaReserva: String!, fechaDevolucion: String!, status: String!): ReservaLibro\n"
                +
                "  actualizarReserva(id: ID!, userId: String, libroId: String, fechaReserva: String, fechaDevolucion: String, status: String): ReservaLibro\n"
                +
                "  eliminarReserva(id: ID!): Boolean\n" +
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
                        .dataFetcher("reservas", env -> getReservas())
                        .dataFetcher("reserva", env -> getReserva(env.getArgument("id"))))
                .type("Mutation", typeWiring -> typeWiring
                        .dataFetcher("crearReserva", env -> crearReserva(
                                env.getArgument("userId"),
                                env.getArgument("libroId"),
                                env.getArgument("fechaReserva"),
                                env.getArgument("fechaDevolucion"),
                                env.getArgument("status")))
                        .dataFetcher("actualizarReserva", env -> actualizarReserva(
                                env.getArgument("id"),
                                env.getArgument("userId"),
                                env.getArgument("libroId"),
                                env.getArgument("fechaReserva"),
                                env.getArgument("fechaDevolucion"),
                                env.getArgument("status")))
                        .dataFetcher("eliminarReserva", env -> eliminarReserva(env.getArgument("id"))))
                .build();
    }

    public static GraphQL getGraphQL() {
        return graphQL;
    }

    private static List<Map<String, Object>> getReservas() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id, user_id, libro_id, fecha_reserva, fecha_devolucion, status FROM RESERVAS";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapResultSet(rs));
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
                    return mapResultSet(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error BD buscando reserva " + id + ": " + e.getMessage(), e);
        }
        return null;
    }

    private static Map<String, Object> crearReserva(String userId, String libroId, String fechaReserva,
            String fechaDevolucion, String status) {
        String sql = "INSERT INTO RESERVAS (user_id, libro_id, fecha_reserva, fecha_devolucion, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, userId);
            stmt.setString(2, libroId);
            stmt.setString(3, fechaReserva);
            stmt.setString(4, fechaDevolucion);
            stmt.setString(5, status);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return getReserva(String.valueOf(rs.getInt(1)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error insertando reserva a la BD: " + e.getMessage(), e);
        }
        return null;
    }

    private static Map<String, Object> actualizarReserva(String id, String userId, String libroId, String fechaReserva,
            String fechaDevolucion, String status) {
        Map<String, Object> existing = getReserva(id);
        if (existing == null)
            throw new RuntimeException("Reserva con ID " + id + " no encontrada para actualizar.");

        String uid = userId != null ? userId : (String) existing.get("userId");
        String lid = libroId != null ? libroId : (String) existing.get("libroId");
        String fres = fechaReserva != null ? fechaReserva : (String) existing.get("fechaReserva");
        String fdev = fechaDevolucion != null ? fechaDevolucion : (String) existing.get("fechaDevolucion");
        String st = status != null ? status : (String) existing.get("status");

        String sql = "UPDATE RESERVAS SET user_id = ?, libro_id = ?, fecha_reserva = ?, fecha_devolucion = ?, status = ? WHERE id = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uid);
            stmt.setString(2, lid);
            stmt.setString(3, fres);
            stmt.setString(4, fdev);
            stmt.setString(5, st);
            stmt.setString(6, id);
            stmt.executeUpdate();
            return getReserva(id);
        } catch (Exception e) {
            throw new RuntimeException("Error actualizando reserva BD: " + e.getMessage(), e);
        }
    }

    private static boolean eliminarReserva(String id) {
        String sql = "DELETE FROM RESERVAS WHERE id = ?";
        try (Connection conn = MySqlConfig.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Error BD eliminando reserva: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> mapResultSet(ResultSet rs) throws Exception {
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
