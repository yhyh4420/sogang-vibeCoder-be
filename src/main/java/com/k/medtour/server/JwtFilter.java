package com.k.medtour.server;

import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.auth.jwt.JwtTokenProvider;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.exception.ErrorCode;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * JWT authentication middleware for the custom HTTP server.
 * Skips authentication for public paths (e.g., /api/v1/auth/**).
 */
public class JwtFilter implements MiddlewareHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String AUTHORIZATION_HEADER = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final Set<String> publicPathPrefixes;

    public JwtFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.publicPathPrefixes = Set.of(
                "/api/v1/auth/"
        );
    }

    @Override
    public boolean handle(RequestContext ctx, HttpExchange exchange) throws Exception {
        String path = ctx.path();

        // Skip authentication for public paths
        for (String prefix : publicPathPrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        // Extract Bearer token
        String authHeader = ctx.header(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorized(exchange, "Missing or invalid Authorization header");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtTokenProvider.validateToken(token)) {
            sendUnauthorized(exchange, "Invalid or expired token");
            return false;
        }

        // Parse token and set UserPrincipal
        Long memberId = jwtTokenProvider.getMemberId(token);
        String role = jwtTokenProvider.getRole(token);
        ctx.setUserPrincipal(new UserPrincipal(memberId, role));

        return true;
    }

    private void sendUnauthorized(HttpExchange exchange, String message) throws Exception {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNAUTHORIZED, message);
        String json = JsonUtil.toJson(response);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(401, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
