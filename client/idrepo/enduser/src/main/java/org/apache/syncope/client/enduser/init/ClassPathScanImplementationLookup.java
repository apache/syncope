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

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.client.enduser.pages.BaseExtPage;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.BinaryPreviewer;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class ClassPathScanImplementationLookup implements Serializable {

    private static final long serialVersionUID = -4944986595429290116L;

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope";

    private List<Class<? extends BaseSSOLoginFormPanel>> ssoLoginFormPanels;

    private List<Class<? extends BasePage>> extPages;

    private List<Class<? extends BinaryPreviewer>> previewers;

    /**
     * This method can be overridden by subclasses to customize classpath scan.
     *
     * @return basePackage for classpath scanning
     */
    protected static String getBasePackage() {
        return DEFAULT_BASE_PACKAGE;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        extPages = new ArrayList<>();
        ssoLoginFormPanels = new ArrayList<>();
        previewers = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseSSOLoginFormPanel.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BinaryPreviewer.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(getBasePackage())) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(Objects.requireNonNull(bd.getBeanClassName()),
                        ClassUtils.getDefaultClassLoader());
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                if (BaseExtPage.class.isAssignableFrom(clazz)) {
                    if (clazz.isAnnotationPresent(ExtPage.class)) {
                        extPages.add((Class<? extends BasePage>) clazz);
                    } else {
                        LOG.error("Could not find annotation {} in {}, ignoring",
                                ExtPage.class.getName(), clazz.getName());
                    }
                } else if (BaseSSOLoginFormPanel.class.isAssignableFrom(clazz)) {
                    ssoLoginFormPanels.add((Class<? extends BaseSSOLoginFormPanel>) clazz);
                } else if (BinaryPreviewer.class.isAssignableFrom(clazz)) {
                    previewers.add((Class<? extends BinaryPreviewer>) clazz);
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }

        ssoLoginFormPanels = Collections.unmodifiableList(ssoLoginFormPanels);

        LOG.debug("Extension pages found: {}", extPages);
        LOG.debug("SSO Login pages found: {}", ssoLoginFormPanels);
        LOG.debug("Binary previewers found: {}", previewers);
    }

    public List<Class<? extends BaseSSOLoginFormPanel>> getSSOLoginFormPanels() {
        return this.ssoLoginFormPanels;
    }

    public List<Class<? extends BasePage>> getExtPageClasses() {
        return extPages;
    }

    public Class<? extends BinaryPreviewer> getPreviewerClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);
        Class<? extends BinaryPreviewer> previewer = null;
        for (Class<? extends BinaryPreviewer> candidate : previewers) {
            LOG.debug("Evaluating previewer class {} for MIME type {}", candidate.getName(), mimeType);
            if (candidate.isAnnotationPresent(BinaryPreview.class)
                    && ArrayUtils.contains(candidate.getAnnotation(BinaryPreview.class).mimeTypes(), mimeType)) {
                LOG.debug("Found existing previewer for MIME type {}: {}", mimeType, candidate.getName());
                previewer = candidate;
            }
        }
        return previewer;
    }
}
