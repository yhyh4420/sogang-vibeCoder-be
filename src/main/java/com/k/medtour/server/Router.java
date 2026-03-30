package com.k.medtour.server;

import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(Router.class);

    private final List<Route> routes = new ArrayList<>();
    private final List<MiddlewareHandler> middlewares = new ArrayList<>();

    public void addMiddleware(MiddlewareHandler middleware) {
        middlewares.add(middleware);
    }

    public void get(String path, RouteHandler handler) {
        routes.add(new Route("GET", path, handler));
    }

    public void post(String path, RouteHandler handler) {
        routes.add(new Route("POST", path, handler));
    }

    public void put(String path, RouteHandler handler) {
        routes.add(new Route("PUT", path, handler));
    }

    public void patch(String path, RouteHandler handler) {
        routes.add(new Route("PATCH", path, handler));
    }

    public void delete(String path, RouteHandler handler) {
        routes.add(new Route("DELETE", path, handler));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String requestPath = exchange.getRequestURI().getPath();

        // CORS preflight
        if ("OPTIONS".equals(method)) {
            setCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        setCorsHeaders(exchange);

        try {
            // Find matching route
            RouteMatch match = findRoute(method, requestPath);
            if (match == null) {
                sendJsonResponse(exchange, 404,
                        ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND, "Route not found: " + method + " " + requestPath));
                return;
            }

            // Build RequestContext
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getRawQuery());
            Map<String, String> headers = parseHeaders(exchange);
            String body = readBody(exchange);

            RequestContext ctx = new RequestContext(method, requestPath, match.pathParams(), queryParams, headers, body);

            // Apply middlewares
            for (MiddlewareHandler mw : middlewares) {
                boolean proceed = mw.handle(ctx, exchange);
                if (!proceed) {
                    return; // middleware already sent response
                }
            }

            // Execute handler
            Object result = match.route().handler().handle(ctx);

            // Send response
            if (result instanceof ApiResponse<?> apiResp) {
                int status = apiResp.success() ? 200 : 400;
                sendJsonResponse(exchange, status, apiResp);
            } else if (result == null) {
                sendJsonResponse(exchange, 200, ApiResponse.success(null));
            } else {
                sendJsonResponse(exchange, 200, ApiResponse.success(result));
            }

        } catch (BusinessException e) {
            log.warn("Business exception: {}", e.getMessage());
            ErrorCode ec = e.getErrorCode();
            sendJsonResponse(exchange, ec.getHttpStatusCode(),
                    ApiResponse.error(ec, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            sendJsonResponse(exchange, 400,
                    ApiResponse.error(ErrorCode.INVALID_INPUT, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected exception", e);
            sendJsonResponse(exchange, 500,
                    ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        }
    }

    private RouteMatch findRoute(String method, String path) {
        for (Route route : routes) {
            if (!route.method().equals(method)) continue;
            Map<String, String> params = matchPath(route.pathPattern(), path);
            if (params != null) {
                return new RouteMatch(route, params);
            }
        }
        return null;
    }

    /**
     * Matches a path pattern (e.g., /api/v1/journeys/{journeyId}/items/{itemId})
     * against an actual path. Returns extracted path parameters or null if no match.
     */
    static Map<String, String> matchPath(String pattern, String actual) {
        // Convert pattern to regex
        // /api/v1/journeys/{journeyId} -> /api/v1/journeys/(?<journeyId>[^/]+)
        StringBuilder regex = new StringBuilder("^");
        List<String> paramNames = new ArrayList<>();
        String[] patternParts = pattern.split("/", -1);
        String[] actualParts = actual.split("/", -1);

        if (patternParts.length != actualParts.length) {
            return null;
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < patternParts.length; i++) {
            String pp = patternParts[i];
            String ap = actualParts[i];
            if (pp.startsWith("{") && pp.endsWith("}")) {
                String paramName = pp.substring(1, pp.length() - 1);
                params.put(paramName, ap);
            } else {
                if (!pp.equals(ap)) {
                    return null;
                }
            }
        }
        return params;
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return params;
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            } else {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }

    private Map<String, String> parseHeaders(HttpExchange exchange) {
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                headers.put(key.toLowerCase(), values.getFirst());
            }
        });
        return headers;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept-Language");
        headers.set("Access-Control-Max-Age", "3600");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        String json = JsonUtil.toJson(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Internal records
    private record Route(String method, String pathPattern, RouteHandler handler) {
    }

    private record RouteMatch(Route route, Map<String, String> pathParams) {
    }
}
