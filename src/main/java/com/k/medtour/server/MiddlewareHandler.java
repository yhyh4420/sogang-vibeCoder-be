package com.k.medtour.server;

import com.sun.net.httpserver.HttpExchange;

/**
 * Middleware that runs before route handlers.
 * Returns true to continue processing, false to stop (middleware already sent response).
 */
@FunctionalInterface
public interface MiddlewareHandler {
    boolean handle(RequestContext ctx, HttpExchange exchange) throws Exception;
}
