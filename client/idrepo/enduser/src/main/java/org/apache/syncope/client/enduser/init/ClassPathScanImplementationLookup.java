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
package org.apache.syncope.client.enduser.init;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.client.enduser.annotations.Resource;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.syncope.client.ui.commons.panels.SSOLoginFormPanel;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class ClassPathScanImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope.client.enduser";

    private List<Class<? extends SSOLoginFormPanel>> ssoLoginFormPanels = new ArrayList<>();

    private List<Class<? extends AbstractResource>> resources = new ArrayList<>();

    private List<Class<? extends AbstractBinaryPreviewer>> previewers = new ArrayList<>();

    /**
     * This method can be overridden by subclasses to customize classpath scan.
     *
     * @return basePackage for classpath scanning
     */
    protected String getBasePackage() {
        return DEFAULT_BASE_PACKAGE;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        resources = new ArrayList<>();
        previewers = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractResource.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(getBasePackage())) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (!isAbsractClazz) {
                    if (AbstractResource.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(Resource.class)) {
                            resources.add((Class<? extends AbstractResource>) clazz);
                        } else if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz)) {
                            previewers.add((Class<? extends AbstractBinaryPreviewer>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    Resource.class.getName(), clazz.getName());
                        }
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        resources = Collections.unmodifiableList(resources);
    }

    public Class<? extends AbstractBinaryPreviewer> getPreviewerClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);
        Class<? extends AbstractBinaryPreviewer> previewer = null;
        for (Class<? extends AbstractBinaryPreviewer> candidate : previewers) {
            LOG.debug("Evaluating previewer class {} for MIME type {}", candidate.getName(), mimeType);
            if (candidate.isAnnotationPresent(BinaryPreview.class)
                    && ArrayUtils.contains(candidate.getAnnotation(BinaryPreview.class).mimeTypes(), mimeType)) {
                LOG.debug("Found existing previewer for MIME type {}: {}", mimeType, candidate.getName());
                previewer = candidate;
            }
        }
        return previewer;
    }

    public List<Class<? extends AbstractResource>> getResources() {
        return resources;
    }

    public List<Class<? extends SSOLoginFormPanel>> getSSOLoginFormPanels() {
        return this.ssoLoginFormPanels;
    }

}
