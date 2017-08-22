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
import org.apache.commons.lang3.ObjectUtils;
import org.apache.syncope.client.console.pages.BaseExtPage;
import org.apache.syncope.client.console.annotations.BinaryPreview;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.annotations.ExtWidget;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SSOLoginFormPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.syncope.client.console.widgets.BaseExtWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class ClassPathScanImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope.client.console";

    private List<Class<? extends BasePage>> pages;

    private List<Class<? extends AbstractBinaryPreviewer>> previewers;

    private List<Class<? extends BaseExtPage>> extPages;

    private List<Class<? extends BaseExtWidget>> extWidgets;

    private List<Class<? extends SSOLoginFormPanel>> ssoLoginFormPanels;

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
        pages = new ArrayList<>();
        previewers = new ArrayList<>();
        extPages = new ArrayList<>();
        extWidgets = new ArrayList<>();
        ssoLoginFormPanels = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractBinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtPage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SSOLoginFormPanel.class));

        scanner.findCandidateComponents(getBasePackage()).forEach(bd -> {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (!isAbsractClazz) {
                    if (BaseExtPage.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(ExtPage.class)) {
                            extPages.add((Class<? extends BaseExtPage>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    ExtPage.class.getName(), clazz.getName());
                        }
                    } else if (BaseExtWidget.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(ExtWidget.class)) {
                            extWidgets.add((Class<? extends BaseExtWidget>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    ExtWidget.class.getName(), clazz.getName());
                        }
                    } else if (BasePage.class.isAssignableFrom(clazz)) {
                        pages.add((Class<? extends BasePage>) clazz);
                    } else if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz)) {
                        previewers.add((Class<? extends AbstractBinaryPreviewer>) clazz);
                    } else if (SSOLoginFormPanel.class.isAssignableFrom(clazz)) {
                        ssoLoginFormPanels.add((Class<? extends SSOLoginFormPanel>) clazz);
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });
        pages = Collections.unmodifiableList(pages);
        previewers = Collections.unmodifiableList(previewers);

        Collections.sort(extPages, (o1, o2)
                -> ObjectUtils.compare(
                        o1.getAnnotation(ExtPage.class).priority(),
                        o2.getAnnotation(ExtPage.class).priority()));
        extPages = Collections.unmodifiableList(extPages);

        Collections.sort(extWidgets, (o1, o2)
                -> ObjectUtils.compare(
                        o1.getAnnotation(ExtWidget.class).priority(),
                        o2.getAnnotation(ExtWidget.class).priority()));
        extWidgets = Collections.unmodifiableList(extWidgets);

        ssoLoginFormPanels = Collections.unmodifiableList(ssoLoginFormPanels);

        LOG.debug("Binary previewers found: {}", previewers);
        LOG.debug("Extension pages found: {}", extPages);
        LOG.debug("Extension widgets found: {}", extWidgets);
        LOG.debug("SSO Login pages found: {}", ssoLoginFormPanels);
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

    public List<Class<? extends BasePage>> getPageClasses() {
        return pages;
    }

    public List<Class<? extends AbstractBinaryPreviewer>> getPreviewerClasses() {
        return previewers;
    }

    public List<Class<? extends BaseExtPage>> getExtPageClasses() {
        return extPages;
    }

    public List<Class<? extends BaseExtWidget>> getExtWidgetClasses() {
        return extWidgets;
    }

    public List<Class<? extends SSOLoginFormPanel>> getSSOLoginFormPanels() {
        return ssoLoginFormPanels;
    }

}
