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
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.pages.AbstractExtPage;
import org.apache.syncope.client.console.annotations.BinaryPreview;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class ClassPathScanImplementationLookup {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

    private List<Class<? extends BasePage>> pages;

    private List<Class<? extends AbstractBinaryPreviewer>> previewers;

    private List<Class<? extends AbstractExtPage>> extPages;

    @SuppressWarnings("unchecked")
    public void load() {
        pages = new ArrayList<>();
        previewers = new ArrayList<>();
        extPages = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractBinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractExtPage.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.EMPTY)) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        bd.getBeanClassName(), ClassUtils.getDefaultClassLoader());
                boolean isAbsractClazz = Modifier.isAbstract(clazz.getModifiers());

                if (!isAbsractClazz) {
                    if (AbstractExtPage.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(ExtPage.class)) {
                            extPages.add((Class<? extends AbstractExtPage>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    ExtPage.class.getName(), clazz.getName());
                        }
                    } else if (BasePage.class.isAssignableFrom(clazz)) {
                        pages.add((Class<? extends BasePage>) clazz);
                    } else if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz)) {
                        previewers.add((Class<? extends AbstractBinaryPreviewer>) clazz);
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }
        pages = Collections.unmodifiableList(pages);
        previewers = Collections.unmodifiableList(previewers);

        Collections.sort(extPages, new Comparator<Class<? extends AbstractExtPage>>() {

            @Override
            public int compare(
                    final Class<? extends AbstractExtPage> o1,
                    final Class<? extends AbstractExtPage> o2) {

                int prio1 = o1.getAnnotation(ExtPage.class).priority();
                int prio2 = o2.getAnnotation(ExtPage.class).priority();

                return prio1 > prio2
                        ? 1
                        : prio1 == prio2
                                ? 0
                                : -1;
            }
        });
        extPages = Collections.unmodifiableList(extPages);

        LOG.debug("Binary previewers found: {}", previewers);
        LOG.debug("Extension pages found: {}", extPages);
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

    public List<Class<? extends AbstractExtPage>> getExtPageClasses() {
        return extPages;
    }

}
