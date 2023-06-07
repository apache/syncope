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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.client.console.ConsoleProperties;
import org.apache.syncope.client.console.annotations.AMPage;
import org.apache.syncope.client.console.annotations.IdMPage;
import org.apache.syncope.client.console.pages.BaseExtPage;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.widgets.BaseExtWidget;
import org.apache.syncope.client.console.widgets.ExtAlertWidget;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.BinaryPreviewer;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

public class ClassPathScanImplementationLookup implements Serializable {

    private static final long serialVersionUID = 5047756409117925203L;

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

    private static final String DEFAULT_BASE_PACKAGE = "org.apache.syncope";

    public static final Set<String> USER_FIELD_NAMES = new HashSet<>();

    public static final Set<String> GROUP_FIELD_NAMES = new HashSet<>();

    public static final Set<String> ANY_OBJECT_FIELD_NAMES = new HashSet<>();

    static {
        initFieldNames(UserTO.class, USER_FIELD_NAMES);
        initFieldNames(GroupTO.class, GROUP_FIELD_NAMES);
        initFieldNames(AnyObjectTO.class, ANY_OBJECT_FIELD_NAMES);
    }

    private static void initFieldNames(final Class<?> entityClass, final Set<String> keys) {
        List<Class<?>> classes = org.apache.commons.lang3.ClassUtils.getAllSuperclasses(entityClass);
        classes.add(entityClass);
        classes.forEach(clazz -> {
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())
                        && !Collection.class.isAssignableFrom(field.getType())
                        && !Map.class.isAssignableFrom(field.getType())) {

                    keys.add(field.getName());
                }
            }
        });
    }

    /**
     * This method can be overridden by subclasses to customize classpath scan.
     *
     * @return basePackage for classpath scanning
     */
    protected static String getBasePackage() {
        return DEFAULT_BASE_PACKAGE;
    }

    private final Collection<ClassPathScanImplementationContributor> contributors;

    private Map<String, List<Class<?>>> classes;

    private List<Class<? extends BasePage>> idRepoPages;

    private List<Class<? extends BasePage>> idmPages;

    private List<Class<? extends BasePage>> amPages;

    private final ConsoleProperties props;

    public ClassPathScanImplementationLookup(
            final Collection<ClassPathScanImplementationContributor> contributors,
            final ConsoleProperties props) {

        this.contributors = contributors;
        this.props = props;
    }

    protected ClassPathScanningCandidateComponentProvider scanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtPage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExtAlertWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseSSOLoginFormPanel.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ReportConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRuleConf.class));

        contributors.forEach(contributor -> contributor.extend(scanner));

        return scanner;
    }

    protected void addClass(final String category, final Class<?> clazz) {
        List<Class<?>> clazzes = classes.get(category);
        if (clazzes == null) {
            clazzes = new ArrayList<>();
            classes.put(category, clazzes);
        }
        clazzes.add(clazz);
    }

    @SuppressWarnings("unchecked")
    public void load() {
        classes = new HashMap<>();
        idRepoPages = new ArrayList<>();
        idmPages = new ArrayList<>();
        amPages = new ArrayList<>();

        for (BeanDefinition bd : scanner().findCandidateComponents(getBasePackage())) {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        Objects.requireNonNull(bd.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                if (BaseExtPage.class.isAssignableFrom(clazz)) {
                    if (clazz.isAnnotationPresent(ExtPage.class)) {
                        addClass(BaseExtPage.class.getName(), clazz);
                    } else {
                        LOG.error("Could not find annotation {} in {}, ignoring",
                                ExtPage.class.getName(), clazz.getName());
                    }
                } else if (BaseExtWidget.class.isAssignableFrom(clazz)) {
                    if (clazz.isAnnotationPresent(ExtWidget.class)) {
                        addClass(BaseExtWidget.class.getName(), clazz);
                    } else {
                        LOG.error("Could not find annotation {} in {}, ignoring",
                                ExtWidget.class.getName(), clazz.getName());
                    }
                } else if (ExtAlertWidget.class.isAssignableFrom(clazz)) {
                    if (clazz.isAnnotationPresent(ExtWidget.class)) {
                        addClass(ExtAlertWidget.class.getName(), clazz);
                    } else {
                        LOG.error("Could not find annotation {} in {}, ignoring",
                                ExtAlertWidget.class.getName(), clazz.getName());
                    }
                } else if (BasePage.class.isAssignableFrom(clazz)) {
                    if (clazz.isAnnotationPresent(IdMPage.class)) {
                        if (!clazz.getName().endsWith("Topology")
                                || (clazz.getName().equals(props.getPage().get("topology").getName()))) {
                            idmPages.add((Class<? extends BasePage>) clazz);
                        }
                    } else if (clazz.isAnnotationPresent(AMPage.class)) {
                        amPages.add((Class<? extends BasePage>) clazz);
                    } else {
                        idRepoPages.add((Class<? extends BasePage>) clazz);
                    }
                } else if (BaseSSOLoginFormPanel.class.isAssignableFrom(clazz)) {
                    addClass(BaseSSOLoginFormPanel.class.getName(), clazz);
                } else if (BinaryPreviewer.class.isAssignableFrom(clazz)) {
                    addClass(BinaryPreviewer.class.getName(), clazz);
                } else if (ReportConf.class.isAssignableFrom(clazz)) {
                    addClass(ReportConf.class.getName(), clazz);
                } else if (AccountRuleConf.class.isAssignableFrom(clazz)) {
                    addClass(AccountRuleConf.class.getName(), clazz);
                } else if (PasswordRuleConf.class.isAssignableFrom(clazz)) {
                    addClass(PasswordRuleConf.class.getName(), clazz);
                } else {
                    contributors.forEach(contributor -> contributor.getLabel(clazz).
                            ifPresent(label -> addClass(label, clazz)));
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        }

        idRepoPages = Collections.unmodifiableList(idRepoPages);

        idmPages.sort(Comparator.comparing(o -> o.getAnnotation(IdMPage.class).priority()));
        idmPages = Collections.unmodifiableList(idmPages);

        amPages.sort(Comparator.comparing(o -> o.getAnnotation(AMPage.class).priority()));
        amPages = Collections.unmodifiableList(amPages);

        if (classes.containsKey(BaseExtPage.class.getName())) {
            classes.get(BaseExtPage.class.getName()).
                    sort(Comparator.comparing(o -> o.getAnnotation(ExtPage.class).priority()));
        }

        if (classes.containsKey(BaseExtWidget.class.getName())) {
            classes.get(BaseExtWidget.class.getName()).
                    sort(Comparator.comparing(o -> o.getAnnotation(ExtWidget.class).priority()));
        }

        if (classes.containsKey(ExtAlertWidget.class.getName())) {
            classes.get(ExtAlertWidget.class.getName()).
                    sort(Comparator.comparing(o -> o.getAnnotation(ExtWidget.class).priority()));
        }

        classes.forEach((category, clazzes) -> LOG.debug("{} found: {}", category, clazzes));
    }

    public List<Class<? extends BasePage>> getIdRepoPageClasses() {
        return idRepoPages;
    }

    public List<Class<? extends BasePage>> getIdMPageClasses() {
        return idmPages;
    }

    public List<Class<? extends BasePage>> getAMPageClasses() {
        return amPages;
    }

    @SuppressWarnings("unchecked")
    public <T> List<Class<? extends T>> getClasses(final Class<T> reference) {
        return classes.getOrDefault(reference.getName(), List.of()).stream().
                map(clazz -> (Class<? extends T>) clazz).
                collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Class<? extends ExtAlertWidget<?>>> getExtAlertWidgetClasses() {
        return classes.getOrDefault(ExtAlertWidget.class.getName(), List.of()).stream().
                map(clazz -> (Class<? extends ExtAlertWidget<?>>) clazz).
                collect(Collectors.toList());
    }

    public Class<? extends BinaryPreviewer> getPreviewerClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);

        Class<? extends BinaryPreviewer> previewer = null;
        for (Class<? extends BinaryPreviewer> candidate : getClasses(BinaryPreviewer.class)) {
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
