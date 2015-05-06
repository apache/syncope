/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.notification;

import java.io.IOException;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Similar to Spring's equivalent (<tt>org.springframework.ui.velocity.VelocityEngineFactoryBean</tt>), does not
 * implement {@link org.springframework.context.ResourceLoaderAware} thus allowing custom injection.
 */
public class VelocityEngineFactoryBean implements FactoryBean<VelocityEngine>, InitializingBean {

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    private boolean overrideLogging = true;

    private VelocityEngine velocityEngine;

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public boolean isOverrideLogging() {
        return overrideLogging;
    }

    /**
     * Configure Velocity to use Commons Logging (true by default).
     *
     * @param overrideLogging whether default Velocity logging should be overriden or not.
     */
    public void setOverrideLogging(final boolean overrideLogging) {
        this.overrideLogging = overrideLogging;
    }

    private void createVelocityEngine() throws IOException {
        velocityEngine = new VelocityEngine();

        velocityEngine.setProperty(
                RuntimeConstants.RESOURCE_LOADER, SpringVelocityResourceLoader.NAME);
        velocityEngine.setProperty(
                SpringVelocityResourceLoader.SPRING_RESOURCE_LOADER_CLASS,
                SpringVelocityResourceLoader.class.getName());
        velocityEngine.setProperty(
                SpringVelocityResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");
        velocityEngine.setApplicationAttribute(
                SpringVelocityResourceLoader.SPRING_RESOURCE_LOADER, getResourceLoader());

        if (this.overrideLogging) {
            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new CommonsLogLogChute());
        }

        velocityEngine.init();
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        createVelocityEngine();
    }

    @Override
    public VelocityEngine getObject() {
        return this.velocityEngine;
    }

    @Override
    public Class<? extends VelocityEngine> getObjectType() {
        return VelocityEngine.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
