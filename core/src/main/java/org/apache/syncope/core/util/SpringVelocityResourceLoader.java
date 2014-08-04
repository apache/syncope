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
package org.apache.syncope.core.util;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Velocity ResourceLoader adapter that loads via a Spring ResourceLoader.
 * Similar to {@link org.springframework.ui.velocity.SpringResourceLoader} but more integrated with
 * {@link VelocityEngineFactoryBean}.
 */
public class SpringVelocityResourceLoader extends ResourceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SpringVelocityResourceLoader.class);

    public static final String NAME = "spring";

    public static final String SPRING_RESOURCE_LOADER_CLASS = "spring.resource.loader.class";

    public static final String SPRING_RESOURCE_LOADER_CACHE = "spring.resource.loader.cache";

    public static final String SPRING_RESOURCE_LOADER = "spring.resource.loader";

    private org.springframework.core.io.ResourceLoader resourceLoader;

    @Override
    public void init(ExtendedProperties configuration) {
        this.resourceLoader =
                (org.springframework.core.io.ResourceLoader) this.rsvc.getApplicationAttribute(SPRING_RESOURCE_LOADER);
        if (this.resourceLoader == null) {
            throw new IllegalArgumentException(
                    "'" + SPRING_RESOURCE_LOADER + "' application attribute must be present for SpringResourceLoader");
        }

        LOG.info("SpringResourceLoader for Velocity: using resource loader [" + this.resourceLoader + "]");
    }

    @Override
    public InputStream getResourceStream(final String source) throws ResourceNotFoundException {
        LOG.debug("Looking for Velocity resource with name [{}]", source);

        org.springframework.core.io.Resource resource = this.resourceLoader.getResource(source);
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            LOG.debug("Could not find Velocity resource: " + resource, e);
        }
        throw new ResourceNotFoundException("Could not find resource [" + source + "]");
    }

    @Override
    public boolean isSourceModified(final Resource resource) {
        return false;
    }

    @Override
    public long getLastModified(final Resource resource) {
        return 0;
    }

}
