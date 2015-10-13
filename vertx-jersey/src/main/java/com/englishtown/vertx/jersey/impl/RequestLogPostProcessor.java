package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.inject.VertxPostResponseProcessor;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.LoggerFactory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import java.time.Clock;
import java.util.Optional;
import java.util.logging.Logger;

import static com.englishtown.vertx.jersey.impl.RequestLogProcessor.HTTP_VERSION;
import static com.englishtown.vertx.jersey.impl.RequestLogProcessor.REMOTE_ADDRESS;
import static com.englishtown.vertx.jersey.impl.RequestLogProcessor.START_TIMESTAMP;

public class RequestLogPostProcessor implements VertxPostResponseProcessor{
    public static final String X_FORWARDED_FOR = "x-forwarded-for";
    public static final char WHITESPACE = ' ';
    private final static Logger LOGGER = Logger.getLogger(RequestLogPostProcessor.class.getName());

    @Override
    public void process(HttpServerResponse vertxResponse, ContainerResponse jerseyResponse) {
        final String logLine = createLogLine(vertxResponse, jerseyResponse);
        LOGGER.info(logLine);
    }

    protected String createLogLine(HttpServerResponse vertxResponse, ContainerResponse jerseyResponse) {
        final StringBuilder buf = new StringBuilder(256);

        ContainerRequest requestContext = jerseyResponse.getRequestContext();

        buf.append(getRemoteAddress(requestContext)).append(WHITESPACE);
        buf.append("-").append(WHITESPACE); //User Identity
        buf.append("-").append(WHITESPACE); //Auth Principal name
        buf.append(getTimestamp()).append(WHITESPACE);

        buf.append('"');
        buf.append(requestContext.getMethod()).append(WHITESPACE);
        buf.append(requestContext.getRequestUri().getRawPath()).append(WHITESPACE);
        buf.append(requestContext.getProperty(HTTP_VERSION));
        buf.append('"').append(WHITESPACE);

        buf.append(vertxResponse.getStatusCode()).append(WHITESPACE);

        String contentLengthValue = getContentLengthValue(jerseyResponse);
        buf.append(contentLengthValue).append(WHITESPACE);

        final long now = System.currentTimeMillis();
        final long startTime = (Long)requestContext.getProperty(START_TIMESTAMP);
        buf.append(now - startTime).append(WHITESPACE);

        buf.append(getReferrer(requestContext)).append(WHITESPACE);
        buf.append(getUserAgent(requestContext)).append(WHITESPACE);
        return buf.toString();
    }

    private String getUserAgent(ContainerRequest requestContext){
        String userAgent = requestContext.getHeaderString(HttpHeaders.USER_AGENT.toString());
        if(userAgent != null){
            return "\"" + userAgent + "\"";
        }
        return "\"-\"";
    }

    private String getReferrer(ContainerRequest requestContext) {
        String referrer = requestContext.getHeaderString(HttpHeaders.REFERER.toString());
        if(referrer != null){
            return "\"" + referrer + "\"";
        }
        return "\"-\"";
    }

    private String getTimestamp() {
        return "[" + Clock.systemUTC().instant().toString() + "]";
    }

    private String getRemoteAddress(ContainerRequest httpServerRequest) {
        String address = httpServerRequest.getHeaderString(X_FORWARDED_FOR);
        if (address == null) {
            address = (String)httpServerRequest.getProperty(REMOTE_ADDRESS);
        }
        return Optional.ofNullable(address).orElse("-");
    }

    private String getContentLengthValue(ContainerResponse event) {
        String s = event.getHeaderString(HttpHeaders.CONTENT_LENGTH.toString());
        try{
            final long responseLength = Long.parseLong(s);
            if (responseLength >= 0) {
                return String.valueOf(responseLength);
            }
            return "-";
        }catch (Exception e){
            return "-";
        }
    }
}
