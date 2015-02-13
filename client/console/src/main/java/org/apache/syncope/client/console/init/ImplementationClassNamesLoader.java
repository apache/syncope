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
package org.apache.syncope.client.console.init;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.AbstractExtensionPanel;
import org.apache.syncope.client.console.BinaryPreview;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class ImplementationClassNamesLoader implements SyncopeConsoleLoader {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImplementationClassNamesLoader.class);

    private List<Class<? extends AbstractBinaryPreviewer>> previewers;

    private List<Class<? extends AbstractExtensionPanel>> extPanels;

    @Override
    public Integer getPriority() {
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        previewers = new ArrayList<>();
        extPanels = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractBinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractExtensionPanel.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.EMPTY)) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    previewers.add((Class<? extends AbstractBinaryPreviewer>) clazz);
                } else if (AbstractExtensionPanel.class.isAssignableFrom(clazz) && !isAbsractClazz) {
                    extPanels.add((Class<? extends AbstractExtensionPanel>) clazz);
                }

            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        previewers = Collections.unmodifiableList(previewers);
        extPanels = Collections.unmodifiableList(extPanels);

        LOG.debug("Binary previewers found: {}", previewers);
        LOG.debug("Extension panels found: {}", extPanels);
    }

    public Class<? extends AbstractBinaryPreviewer> getPreviewerClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);
        Class<? extends AbstractBinaryPreviewer> previewer = null;
        for (Class<? extends AbstractBinaryPreviewer> candidate : previewers) {
            LOG.debug("Evaluating previewer class {} for MIME type {}", candidate.getName(), mimeType);
            if (ArrayUtils.contains(candidate.getAnnotation(BinaryPreview.class).mimeTypes(), mimeType)) {
                LOG.debug("Found existing previewer for MIME type {}: {}", mimeType, candidate.getName());
                previewer = candidate;
            }
        }
        return previewer;
    }

    public List<Class<? extends AbstractBinaryPreviewer>> getPreviewerClasses() {
        return previewers;
    }

    public List<Class<? extends AbstractExtensionPanel>> getExtPanelClasses() {
        return extPanels;
    }

}
