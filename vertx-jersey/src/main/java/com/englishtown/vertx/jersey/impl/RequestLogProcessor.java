package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.inject.VertxRequestProcessor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.glassfish.jersey.server.ContainerRequest;

public class RequestLogProcessor implements VertxRequestProcessor {

    @Override
    public void process(HttpServerRequest vertxRequest, ContainerRequest jerseyRequest, Handler<Void> done) {
        long timestamp = System.currentTimeMillis();
        jerseyRequest.setProperty("startTimestamp", timestamp);
        jerseyRequest.setProperty("remoteAddress", vertxRequest.remoteAddress().host());
        jerseyRequest.setProperty("");
    }
}
