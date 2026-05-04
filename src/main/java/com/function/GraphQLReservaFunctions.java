package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphql.GraphQLProvider;
import com.graphql.GraphQLRequest;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GraphQLReservaFunctions {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("GraphQLCrearReserva")
    public HttpResponseMessage graphQLCrearReserva(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "graphql/reservas/crear",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando petición POST en GraphQLCrearReserva");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El cuerpo de la petición es obligatorio\"}")
                    .build();
        }

        try {
            GraphQLRequest graphQLRequest = objectMapper.readValue(request.getBody().get(), GraphQLRequest.class);

            ExecutionInput.Builder builder = ExecutionInput.newExecutionInput()
                    .query(graphQLRequest.getQuery())
                    .operationName(graphQLRequest.getOperationName());

            if (graphQLRequest.getVariables() != null) {
                builder.variables(graphQLRequest.getVariables());
            }

            ExecutionResult executionResult = GraphQLProvider.getGraphQL().execute(builder.build());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(executionResult.toSpecification())
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error procesando GraphQL execution: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno procesando respuesta GraphQL: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @FunctionName("GraphQLActualizarEstadoReserva")
    public HttpResponseMessage graphQLActualizarEstadoReserva(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "graphql/reservas/actualizar-estado",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando petición POST en GraphQLActualizarEstadoReserva");

        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El cuerpo de la petición es obligatorio\"}")
                    .build();
        }

        try {
            GraphQLRequest graphQLRequest = objectMapper.readValue(request.getBody().get(), GraphQLRequest.class);

            ExecutionInput.Builder builder = ExecutionInput.newExecutionInput()
                    .query(graphQLRequest.getQuery())
                    .operationName(graphQLRequest.getOperationName());

            if (graphQLRequest.getVariables() != null) {
                builder.variables(graphQLRequest.getVariables());
            }

            ExecutionResult executionResult = GraphQLProvider.getGraphQL().execute(builder.build());

            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(executionResult.toSpecification())
                    .build();
            
        } catch (Exception e) {
            context.getLogger().severe("Error procesando GraphQL execution: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error interno procesando respuesta GraphQL: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
