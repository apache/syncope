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
package org.apache.syncope.core.spring;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ResourceWithFallbackLoader implements ResourceLoaderAware, ResourcePatternResolver {

    private ResourcePatternResolver resolver;

    private String primary;

    private String fallback;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resolver = (ResourcePatternResolver) resourceLoader;
    }

    public void setPrimary(final String primary) {
        this.primary = primary;
    }

    public void setFallback(final String fallback) {
        this.fallback = fallback;
    }

    @Override
    public Resource getResource(final String location) {
        Resource resource = resolver.getResource(primary + location);
        if (!resource.exists()) {
            resource = resolver.getResource(fallback + location);
        }

        return resource;
    }

    public Resource getResource() {
        return getResource(StringUtils.EMPTY);
    }

    @Override
    public Resource[] getResources(final String locationPattern) throws IOException {
        Resource[] resources = resolver.getResources(primary + locationPattern);
        if (ArrayUtils.isEmpty(resources)) {
            resources = resolver.getResources(fallback + locationPattern);
        }

        return resources;
    }

    public Resource[] getResources() throws IOException {
        return getResources(StringUtils.EMPTY);
    }

    @Override
    public ClassLoader getClassLoader() {
        return resolver.getClassLoader();
    }
}
