package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.inject.VertxRequestProcessor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import org.glassfish.jersey.server.ContainerRequest;

public class RequestLogProcessor implements VertxRequestProcessor {

    public static final String START_TIMESTAMP = "startTimestamp";
    public static final String REMOTE_ADDRESS = "remoteAddress";
    public static final String HTTP_VERSION = "httpVersion";

    @Override
    public void process(HttpServerRequest vertxRequest, ContainerRequest jerseyRequest, Handler<Void> done) {
        long timestamp = System.currentTimeMillis();
        jerseyRequest.setProperty(START_TIMESTAMP, timestamp);
        jerseyRequest.setProperty(REMOTE_ADDRESS, vertxRequest.remoteAddress().host());
        jerseyRequest.setProperty(HTTP_VERSION, getVersion(vertxRequest.version()));
        done.handle(null);
    }

    private String getVersion(HttpVersion version){
        switch (version){
            case HTTP_1_0:
                return "HTTP/1.0";
            case HTTP_1_1:
                return  "HTTP/1.1";
            default:
                return "";
        }
    }
}
