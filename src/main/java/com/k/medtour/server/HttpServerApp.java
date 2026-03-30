package com.k.medtour.server;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerApp {

    private static final Logger log = LoggerFactory.getLogger(HttpServerApp.class);

    private final HttpServer server;

    public HttpServerApp(int port, Router router) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/", router);
        this.server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public void start() {
        server.start();
        log.info("HTTP server started on port {}", server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        log.info("HTTP server stopped");
    }

    public int getPort() {
        return server.getAddress().getPort();
    }
}
