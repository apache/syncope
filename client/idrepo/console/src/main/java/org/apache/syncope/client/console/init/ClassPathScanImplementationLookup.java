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
import org.apache.syncope.client.console.annotations.AMPage;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.client.console.pages.BaseExtPage;
import org.apache.syncope.client.ui.commons.annotations.BinaryPreview;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.syncope.client.console.annotations.IdMPage;
import org.apache.syncope.client.ui.commons.annotations.Resource;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.ui.commons.panels.BaseSSOLoginFormPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.preview.AbstractBinaryPreviewer;
import org.apache.syncope.client.console.widgets.BaseExtWidget;
import org.apache.syncope.client.console.widgets.ExtAlertWidget;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.request.resource.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.apache.syncope.client.console.wizards.any.UserFormFinalizer;
import org.apache.syncope.client.console.annotations.UserFormFinalize;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;

public class ClassPathScanImplementationLookup {

    protected static final Logger LOG = LoggerFactory.getLogger(ClassPathScanImplementationLookup.class);

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

    private Map<String, List<Class<?>>> classes;

    private List<Class<? extends BasePage>> idRepoPages;

    private List<Class<? extends BasePage>> idmPages;

    private List<Class<? extends BasePage>> amPages;

    private Map<String, Class<? extends ReportletConf>> reportletConfs;

    private Map<String, Class<? extends AccountRuleConf>> accountRuleConfs;

    private Map<String, Class<? extends PasswordRuleConf>> passwordRuleConfs;

    private Map<String, Class<? extends PullCorrelationRuleConf>> pullCorrelationRuleConfs;

    private Map<String, Class<? extends PushCorrelationRuleConf>> pushCorrelationRuleConfs;

    /**
     * This method can be overridden by subclasses to customize classpath scan.
     *
     * @return basePackage for classpath scanning
     */
    protected static String getBasePackage() {
        return DEFAULT_BASE_PACKAGE;
    }

    protected ClassPathScanningCandidateComponentProvider scanner() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractBinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(UserFormFinalizer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtPage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExtAlertWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseSSOLoginFormPanel.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ReportletConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PullCorrelationRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PushCorrelationRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractResource.class));

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

    protected void additional(final Class<?> clazz) {
        // nothing to do
    }

    @SuppressWarnings("unchecked")
    public void load() {
        classes = new HashMap<>();
        idRepoPages = new ArrayList<>();
        idmPages = new ArrayList<>();
        amPages = new ArrayList<>();
        reportletConfs = new HashMap<>();
        accountRuleConfs = new HashMap<>();
        passwordRuleConfs = new HashMap<>();
        pullCorrelationRuleConfs = new HashMap<>();
        pushCorrelationRuleConfs = new HashMap<>();

        scanner().findCandidateComponents(getBasePackage()).forEach(bd -> {
            try {
                Class<?> clazz = ClassUtils.resolveClassName(
                        Objects.requireNonNull(bd.getBeanClassName()), ClassUtils.getDefaultClassLoader());
                if (!Modifier.isAbstract(clazz.getModifiers())) {
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
                            idmPages.add((Class<? extends BasePage>) clazz);
                        } else if (clazz.isAnnotationPresent(AMPage.class)) {
                            amPages.add((Class<? extends BasePage>) clazz);
                        } else {
                            idRepoPages.add((Class<? extends BasePage>) clazz);
                        }
                    } else if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz)) {
                        addClass(AbstractBinaryPreviewer.class.getName(), clazz);
                    } else if (UserFormFinalizer.class.isAssignableFrom(clazz)) {
                        addClass(UserFormFinalizer.class.getName(), clazz);
                    } else if (BaseSSOLoginFormPanel.class.isAssignableFrom(clazz)) {
                        addClass(BaseSSOLoginFormPanel.class.getName(), clazz);
                    } else if (ReportletConf.class.isAssignableFrom(clazz)) {
                        reportletConfs.put(clazz.getName(), (Class<? extends ReportletConf>) clazz);
                    } else if (AccountRuleConf.class.isAssignableFrom(clazz)) {
                        accountRuleConfs.put(clazz.getName(), (Class<? extends AccountRuleConf>) clazz);
                    } else if (PasswordRuleConf.class.isAssignableFrom(clazz)) {
                        passwordRuleConfs.put(clazz.getName(), (Class<? extends PasswordRuleConf>) clazz);
                    } else if (PullCorrelationRuleConf.class.isAssignableFrom(clazz)) {
                        pullCorrelationRuleConfs.put(clazz.getName(), (Class<? extends PullCorrelationRuleConf>) clazz);
                    } else if (PushCorrelationRuleConf.class.isAssignableFrom(clazz)) {
                        pushCorrelationRuleConfs.put(clazz.getName(), (Class<? extends PushCorrelationRuleConf>) clazz);
                    } else if (AbstractResource.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(Resource.class)) {
                            addClass(AbstractResource.class.getName(), clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    Resource.class.getName(), clazz.getName());
                        }
                    } else {
                        additional(clazz);
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });

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

        reportletConfs = Collections.unmodifiableMap(reportletConfs);
        accountRuleConfs = Collections.unmodifiableMap(accountRuleConfs);
        passwordRuleConfs = Collections.unmodifiableMap(passwordRuleConfs);
        pullCorrelationRuleConfs = Collections.unmodifiableMap(pullCorrelationRuleConfs);
        pushCorrelationRuleConfs = Collections.unmodifiableMap(pushCorrelationRuleConfs);

        LOG.debug("Reportlet configurations found: {}", reportletConfs);
        LOG.debug("Account Rule configurations found: {}", accountRuleConfs);
        LOG.debug("Password Rule configurations found: {}", passwordRuleConfs);
        LOG.debug("Pull Correlation Rule configurations found: {}", pullCorrelationRuleConfs);
        LOG.debug("Push Correlation Rule configurations found: {}", pushCorrelationRuleConfs);
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

    public Class<? extends AbstractBinaryPreviewer> getPreviewerClass(final String mimeType) {
        LOG.debug("Searching for previewer class for MIME type: {}", mimeType);

        Class<? extends AbstractBinaryPreviewer> previewer = null;
        for (Class<? extends AbstractBinaryPreviewer> candidate : getClasses(AbstractBinaryPreviewer.class)) {
            LOG.debug("Evaluating previewer class {} for MIME type {}", candidate.getName(), mimeType);
            if (candidate.isAnnotationPresent(BinaryPreview.class)
                    && ArrayUtils.contains(candidate.getAnnotation(BinaryPreview.class).mimeTypes(), mimeType)) {
                LOG.debug("Found existing previewer for MIME type {}: {}", mimeType, candidate.getName());
                previewer = candidate;
            }
        }
        return previewer;
    }

    @SuppressWarnings("unchecked")
    public List<Class<? extends ExtAlertWidget<?>>> getExtAlertWidgetClasses() {
        return classes.getOrDefault(ExtAlertWidget.class.getName(), List.of()).stream().
                map(clazz -> (Class<? extends ExtAlertWidget<?>>) clazz).
                collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Class<? extends UserFormFinalizer>> getUserFormFinalizerClasses(final AjaxWizard.Mode mode) {
        return classes.getOrDefault(UserFormFinalizer.class.getName(), List.of()).stream().
                filter(candidate -> candidate.isAnnotationPresent(UserFormFinalize.class)
                && candidate.getAnnotation(UserFormFinalize.class).mode() == mode).
                map(clazz -> (Class<? extends UserFormFinalizer>) clazz).
                collect(Collectors.toList());
    }

    public Map<String, Class<? extends ReportletConf>> getReportletConfs() {
        return reportletConfs;
    }

    public Map<String, Class<? extends AccountRuleConf>> getAccountRuleConfs() {
        return accountRuleConfs;
    }

    public Map<String, Class<? extends PasswordRuleConf>> getPasswordRuleConfs() {
        return passwordRuleConfs;
    }

    public Map<String, Class<? extends PullCorrelationRuleConf>> getPullCorrelationRuleConfs() {
        return pullCorrelationRuleConfs;
    }

    public Map<String, Class<? extends PushCorrelationRuleConf>> getPushCorrelationRuleConfs() {
        return pushCorrelationRuleConfs;
    }
}
