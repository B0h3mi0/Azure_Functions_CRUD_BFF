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

    @FunctionName("GraphQLReservaGet")
    public HttpResponseMessage graphQLReservaGet(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    route = "graphql/reservas",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando petición GET en GraphQLReservaGet");

        String query = request.getQueryParameters().get("query");
        if (query == null || query.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"El parametro 'query' es obligatorio en un GET\"}")
                    .build();
        }

        String operationName = request.getQueryParameters().get("operationName");
        String variablesStr = request.getQueryParameters().get("variables");

        Map<String, Object> variables = new HashMap<>();
        if (variablesStr != null && !variablesStr.isEmpty()) {
            try {
                variables = objectMapper.readValue(variablesStr, Map.class);
            } catch (Exception e) {
                context.getLogger().warning("Error parseando variables vacias o invalidas: " + e.getMessage());
            }
        }

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .operationName(operationName)
                .variables(variables)
                .build();

        ExecutionResult executionResult = GraphQLProvider.getGraphQL().execute(executionInput);
        
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(executionResult.toSpecification())
                .build();
    }

    @FunctionName("GraphQLReservaPost")
    public HttpResponseMessage graphQLReservaPost(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    route = "graphql/reservas",
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Iniciando petición POST en GraphQLReservaPost");

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
