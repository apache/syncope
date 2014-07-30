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

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class ResourceWithFallbackLoader implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    private String primary;

    private String fallback;

    @Override
    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setPrimary(final String primary) {
        this.primary = primary;
    }

    public void setFallback(final String fallback) {
        this.fallback = fallback;
    }

    public Resource getResource() {
        Resource resource = resourceLoader.getResource(primary);
        if (!resource.exists()) {
            resource = resourceLoader.getResource(fallback);
        }
        if (!resource.exists()) {
            throw new IllegalArgumentException("Neither " + primary + " nor " + fallback + " were found.");
        }

        return resource;
    }

}
