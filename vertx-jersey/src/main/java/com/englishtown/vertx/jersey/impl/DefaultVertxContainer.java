package com.englishtown.vertx.jersey.impl;

import com.englishtown.vertx.jersey.ApplicationHandlerDelegate;
import com.englishtown.vertx.jersey.JerseyOptions;
import com.englishtown.vertx.jersey.VertxContainer;
import com.englishtown.vertx.jersey.inject.InternalVertxJerseyBinder;
import io.vertx.core.Vertx;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.jvnet.hk2.annotations.Optional;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vert.x implementation of {@link Container}
 */
public class DefaultVertxContainer implements VertxContainer {

    private final Vertx vertx;
    private final ServiceLocator locator;
    private JerseyOptions options;
    private ApplicationHandlerDelegate applicationHandlerDelegate;

    @Inject
    public DefaultVertxContainer(Vertx vertx, @Optional ServiceLocator locator) {
        this.vertx = vertx;
        this.locator = locator;
    }

    @Override
    public void init(JerseyOptions options) {
        this.options = options;
        ResourceConfig rc = createConfiguration();
        ApplicationHandler applicationHandler = new ApplicationHandler(rc, null, locator);
        applicationHandler.onStartup(this);
        applicationHandlerDelegate = new DefaultApplicationHandlerDelegate(applicationHandler);
    }

    /**
     * Returns the current vertx instance
     *
     * @return the {@link Vertx} instance
     */
    @Override
    public Vertx getVertx() {
        return vertx;
    }

    @Override
    public JerseyOptions getOptions() {
        return options;
    }

    @Override
    public ApplicationHandlerDelegate getApplicationHandlerDelegate() {
        return applicationHandlerDelegate;
    }

    /**
     * Return an immutable representation of the current {@link ResourceConfig
     * configuration}.
     *
     * @return current configuration of the hosted Jersey application.
     */
    @Override
    public ResourceConfig getConfiguration() {
        ApplicationHandler handler = getApplicationHandler();
        return handler == null ? null : handler.getConfiguration();
    }

    /**
     * Get the Jersey server-side application handler associated with the container.
     *
     * @return Jersey server-side application handler associated with the container.
     */
    @Override
    public ApplicationHandler getApplicationHandler() {
        return applicationHandlerDelegate == null ? null : applicationHandlerDelegate.getApplicationHandler();
    }

    /**
     * Reload the hosted Jersey application using the current {@link ResourceConfig
     * configuration}.
     */
    @Override
    public void reload() {
        reload(getConfiguration());
    }

    /**
     * Reload the hosted Jersey application using a new {@link ResourceConfig
     * configuration}.
     *
     * @param configuration new configuration used for the reload.
     */
    @Override
    public void reload(ResourceConfig configuration) {
        ApplicationHandler applicationHandler = new ApplicationHandler(configuration, null, locator);
        getApplicationHandler().onReload(this);
        applicationHandler.onStartup(this);
        applicationHandlerDelegate = new DefaultApplicationHandlerDelegate(applicationHandler);
    }

    protected ResourceConfig createConfiguration() {

        ResourceConfig rc = new ResourceConfig();

        List<String> packages = options.getPackages();
        if (packages == null || packages.size() == 0) {
            throw new IllegalStateException("At least one resource package name must be specified");
        }
        rc.packages(packages.toArray(new String[packages.size()]));

        Set<Class<?>> components = options.getComponents();
        if (components != null) {
            rc.registerClasses(components);
        }

        // Always register the InternalVertxJerseyBinder
        rc.register(new InternalVertxJerseyBinder(vertx));

        // Register configured binders
        Set<Object> instances = options.getInstances();
        if (instances != null) {
            rc.registerInstances(instances);
        }

        Map<String, Object> properties = options.getProperties();
        if (properties != null) {
            rc.addProperties(properties);
        }

        return rc;
    }

}
