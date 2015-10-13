package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.inject.VertxPostResponseProcessor;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import javax.ws.rs.core.Request;
import java.time.Clock;
import java.util.Optional;

public class RequestLogPostProcessor implements VertxPostResponseProcessor{
    public static final String X_FORWARDED_FOR = "xforwardedfor";
    private final Logger LOGGER = LoggerFactory.getLogger(RequestLogPostProcessor.class);


    @Override
    public void process(HttpServerResponse vertxResponse, ContainerResponse jerseyResponse) {
        final StringBuilder buf = new StringBuilder(256);
        ContainerRequest requestContext = jerseyResponse.getRequestContext();
        String address = getRemoteAddress(requestContext);
        buf.append(address);
        buf.append("  "); //User Identity
        buf.append("  "); //Auth Principal name

        String timestamp = getTimestamp();
        buf.append(timestamp);
        buf.append(' ');

        buf.append('"');
        buf.append(requestContext.getMethod());
        buf.append(' ');
        buf.append(requestContext.getBaseUri().toString());
        buf.append(' ');
        buf.append(requestContext.get));
        buf.append("\" ");

        int status = httpServerResponse.getStatusCode();
        buf.append(status);

        Optional<Long> contentLengthValue = getContentLengthValue(httpServerResponse);
        if(contentLengthValue.isPresent()) {
            final long responseLength = contentLengthValue.get();
            if (responseLength >= 0) {
                buf.append(' ');
                if (responseLength > 99999) {
                    buf.append(responseLength);
                } else {
                    if (responseLength > 9999) {
                        buf.append((char) ('0' + ((responseLength / 10000) % 10)));
                    }
                    if (responseLength > 999) {
                        buf.append((char) ('0' + ((responseLength / 1000) % 10)));
                    }
                    if (responseLength > 99) {
                        buf.append((char) ('0' + ((responseLength / 100) % 10)));
                    }
                    if (responseLength > 9) {
                        buf.append((char) ('0' + ((responseLength / 10) % 10)));
                    }
                    buf.append((char) ('0' + (responseLength % 10)));
                }
                buf.append(' ');
            }
        }else {
            buf.append("  ");
        }

        buf.append(' ');
        final long now = System.currentTimeMillis();
        buf.append(now  startTime);

        buf.append(' ');

        String referer = httpServerRequest.headers().get(HttpHeaders.REFERER);
        if (referer != null)
            buf.append("\"" + referer + "\" ");
        else
        {
            buf.append("\"\"");
        }

        String userAgent = httpServerRequest.headers().get(HttpHeaders.USER_AGENT);

        if(userAgent != null){
            buf.append('"');
            buf.append(userAgent);
            buf.append('"');
        }else{
            buf.append("\"\"");
        }

        LOGGER.info(buf.toString());
    }

    private String getTimestamp() {
        return " [" + Clock.systemUTC().instant().toString() + "] ";
    }

    private String getRemoteAddress(ContainerRequest httpServerRequest) {
        String address = httpServerRequest.getHeaderString(X_FORWARDED_FOR);
        if (address == null) {
            address = (String)httpServerRequest.getProperty("remoteAddress");
        }
        return address;
    }

    private Optional<Long> getContentLengthValue(HttpServerResponse event) {
        String s = event.headers().get(HttpHeaders.CONTENT_LENGTH);
        try{
            return Optional.of(Long.parseLong(s));
        }catch (Exception e){
            return Optional.empty();
        }

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
