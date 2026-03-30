package com.k.medtour.server;

@FunctionalInterface
public interface RouteHandler {
    Object handle(RequestContext ctx) throws Exception;
}
