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
package org.apache.syncope.core.persistence.jpa.spring;

import jakarta.persistence.Entity;
import java.util.Objects;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * Allows having JPA entities spread in several JAR files; this is needed in order to support the Syncope extensions.
 */
public class MultiJarAwarePersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MultiJarAwarePersistenceUnitPostProcessor.class);

    @Override
    public void postProcessPersistenceUnitInfo(final MutablePersistenceUnitInfo pui) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

        scanner.findCandidateComponents(AbstractEntity.class.getPackage().getName()).forEach(bd -> {
            LOG.debug("Adding JPA entity {}", bd.getBeanClassName());
            pui.addManagedClassName(Objects.requireNonNull(bd.getBeanClassName()));
        });
    }
}
