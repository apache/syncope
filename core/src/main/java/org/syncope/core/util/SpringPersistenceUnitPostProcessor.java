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
package org.syncope.core.util;

import java.io.IOException;
import javax.persistence.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * Add to JPA persistence context all beans labeled as @Entity from given
 * location.
 * This is needed only when using LocalContainerEntityManagerFactoryBean with
 * non-standard persistence.xml (currently JBoss-only).
 */
public class SpringPersistenceUnitPostProcessor
        implements PersistenceUnitPostProcessor {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SpringPersistenceUnitPostProcessor.class);

    @Autowired
    private ResourcePatternResolver resResolver;

    private String[] locations;

    public void setLocations(final String[] locations) {
        this.locations = locations == null ? new String[0] : locations.clone();
    }

    @Override
    public void postProcessPersistenceUnitInfo(
            final MutablePersistenceUnitInfo mpui) {

        if (locations.length == 0) {
            LOG.warn("No locations provided");
        }

        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        try {
            for (String location : locations) {
                for (Resource resource : resResolver.getResources(location)) {
                    MetadataReader metadataReader =
                            cachingMetadataReaderFactory.getMetadataReader(
                            resource);
                    if (metadataReader.getAnnotationMetadata().
                            isAnnotated(Entity.class.getName())) {

                        mpui.addManagedClassName(
                                metadataReader.getClassMetadata().
                                getClassName());
                    }
                }
            }
            mpui.setExcludeUnlistedClasses(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
