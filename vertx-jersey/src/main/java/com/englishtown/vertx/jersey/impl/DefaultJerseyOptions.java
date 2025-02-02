/*
 * The MIT License (MIT)
 * Copyright © 2013 Englishtown <opensource@englishtown.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.ApplicationHandlerDelegate;
import com.englishtown.vertx.jersey.JerseyOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;

/**
 * Default {@link com.englishtown.vertx.jersey.JerseyOptions} implementation
 */
public class DefaultJerseyOptions implements JerseyOptions {

    final static String CONFIG_HOST = "host";
    final static String CONFIG_PORT = "port";
    final static String CONFIG_SSL = "ssl";
    final static String CONFIG_JKS_OPTIONS = "jks_options";
    final static String CONFIG_RECEIVE_BUFFER_SIZE = "receive_buffer_size";
    final static String CONFIG_BACKLOG_SIZE = "backlog_size";
    final static String CONFIG_RESOURCE_CONFIG = "resource_config";
    final static String CONFIG_COMPRESSION_SUPPORTED = "compression_supported";

    public static final String CONFIG_BASE_PATH = "base_path";
    public static final String CONFIG_MAX_BODY_SIZE = "max_body_size";
    public static final String CONFIG_RESOURCES = "resources";
    public static final String CONFIG_PACKAGES = "packages";
    public static final String CONFIG_FEATURES = "features";
    public static final String CONFIG_COMPONENTS = "components";
    public static final String CONFIG_BINDERS = "binders";
    public static final String CONFIG_INSTANCES = "instances";
    public static final int DEFAULT_MAX_BODY_SIZE = 1024 * 1000; // Default max body size to 1MB

    private JsonObject config;

    @Override
    public void init(JsonObject config) {
        if (config == null) {
            throw new IllegalStateException("The provided configuration was null");
        }
        this.config = config;
    }

    /**
     * Returns a list of packages to be scanned for resources and components
     *
     * @return
     */
    @Override
    public List<String> getPackages() {
        checkState();
        List<String> list = new ArrayList<>();

        Consumer<JsonArray> reader = array -> {
            if ((array != null && !array.isEmpty())) {
                for (int i = 0; i < array.size(); i++) {
                    list.add(array.getString(i));
                }
            }
        };

        JsonArray resources = config.getJsonArray(CONFIG_RESOURCES, null);
        JsonArray packages = config.getJsonArray(CONFIG_PACKAGES, null);

        reader.accept(resources);
        reader.accept(packages);

        return list;
    }

    /**
     * Optional additional properties to be applied to Jersey resource configuration
     *
     * @return
     */
    @Override
    public Map<String, Object> getProperties() {
        checkState();
        JsonObject json = config.getJsonObject(CONFIG_RESOURCE_CONFIG);
        return json == null ? null : json.getMap();
    }

    /**
     * List of components to be registered (features etc.)
     *
     * @return
     */
    @Override
    public Set<Class<?>> getComponents() {
        checkState();
        Set<Class<?>> set = new HashSet<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Consumer<JsonArray> reader = array -> {
            if (array != null && array.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    try {
                        set.add(cl.loadClass(array.getString(i)));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        JsonArray features = config.getJsonArray(CONFIG_FEATURES, null);
        JsonArray components = config.getJsonArray(CONFIG_COMPONENTS, null);

        reader.accept(features);
        reader.accept(components);

        return set;
    }

    /**
     * Optional list of singleton instances to be registered (hk2 binders etc.)
     *
     * @return
     */
    @Override
    public Set<Object> getInstances() {
        Set<Object> set = new HashSet<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        Consumer<JsonArray> reader = array -> {
            if (array != null && array.size() > 0) {
                for (int i = 0; i < array.size(); i++) {
                    try {
                        Class<?> clazz = cl.loadClass(array.getString(i));
                        set.add(clazz.newInstance());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        JsonArray binders = config.getJsonArray(CONFIG_BINDERS, null);
        JsonArray instances = config.getJsonArray(CONFIG_INSTANCES, null);

        reader.accept(binders);
        reader.accept(instances);

        return set;
    }

    /**
     * The http web server host
     *
     * @return the http web server host to listen to
     */
    @Override
    public String getHost() {
        checkState();
        return config.getString(CONFIG_HOST, "0.0.0.0");
    }

    /**
     * The http web server port
     *
     * @return the http web server port to listen to
     */
    @Override
    public int getPort() {
        checkState();
        return config.getInteger(CONFIG_PORT, 80);
    }

    /**
     * Whether the web server should be https.
     *
     * @return whether the web server should be https.
     */
    @Override
    public boolean getSSL() {
        return config.getBoolean(CONFIG_SSL, false);
    }

    /**
     * Vert.x http server key store options
     *
     * @return Java key store options
     */
    @Override
    public JksOptions getKeyStoreOptions() {
        JsonObject json = config.getJsonObject(CONFIG_JKS_OPTIONS);
        return json == null ? null : new JksOptions(json);
    }

    /**
     * The TCP receive buffer size for connections in bytes
     *
     * @return buffer size in bytes
     */
    @Override
    public Integer getReceiveBufferSize() {
        checkState();
        return config.getInteger(CONFIG_RECEIVE_BUFFER_SIZE);
    }

    /**
     * The accept backlog
     *
     * @return the accept backlog
     */
    @Override
    public int getAcceptBacklog() {
        checkState();
        return config.getInteger(CONFIG_BACKLOG_SIZE, 10000);
    }

    /**
     * Returns the base URI used by Jersey
     *
     * @return base URI
     */
    @Override
    public URI getBaseUri() {
        checkState();
        String basePath = config.getString(CONFIG_BASE_PATH, "/");
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        return URI.create(basePath);
    }

    /**
     * Returns the Jersey {@link org.glassfish.jersey.server.ApplicationHandler} instance
     *
     * @return the application handler instance
     * @deprecated
     */
    @Deprecated
    @Override
    public ApplicationHandlerDelegate getApplicationHandler() {
        throw new UnsupportedOperationException("getApplicatinoHandler is deprecated");
    }

    /**
     * The max body size in bytes when reading the vert.x input stream
     *
     * @return the max body size bytes
     */
    @Override
    public int getMaxBodySize() {
        checkState();
        return config.getInteger(CONFIG_MAX_BODY_SIZE, DEFAULT_MAX_BODY_SIZE);
    }

    /**
     * Gets whether the server supports compression (defaults to false)
     *
     * @return whether compression is supported
     */
    @Override
    public boolean getCompressionSupported() {
        checkState();
        return config.getBoolean(CONFIG_COMPRESSION_SUPPORTED, false);
    }

    private void checkState() {
        if (config == null) {
            throw new IllegalStateException("The jersey options have not been initialized.");
        }
    }

}
