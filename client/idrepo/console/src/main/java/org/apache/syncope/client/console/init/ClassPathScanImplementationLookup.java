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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.syncope.client.console.pages.BaseExtPage;
import org.apache.syncope.client.console.annotations.BinaryPreview;
import org.apache.syncope.client.console.annotations.ExtPage;
import org.apache.syncope.client.console.annotations.ExtWidget;
import org.apache.syncope.client.console.annotations.IdMPage;
import org.apache.syncope.client.console.annotations.Resource;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.EmptyAnyDirectoryPanelAditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.EmptyAnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.EmptyExternalResourceProvider;
import org.apache.syncope.client.console.commons.EmptyStatusProvider;
import org.apache.syncope.client.console.commons.EmptyVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SSOLoginFormPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.preview.AbstractBinaryPreviewer;
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
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;

public class ClassPathScanImplementationLookup {

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

    private ExternalResourceProvider resourceProvider = new EmptyExternalResourceProvider();

    private AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps =
            new EmptyAnyWizardBuilderAdditionalSteps();

    private StatusProvider statusProvider = new EmptyStatusProvider();

    private VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider = new EmptyVirSchemaDetailsPanelProvider();

    private AnyDirectoryPanelAditionalActionLinksProvider anyDirectoryPanelAditionalActionLinksProvider =
            new EmptyAnyDirectoryPanelAditionalActionLinksProvider();

    private List<Class<? extends BasePage>> pages;

    private List<Class<? extends AbstractBinaryPreviewer>> previewers;

    private List<Class<? extends BasePage>> idmPages;

    private List<Class<? extends BaseExtPage>> extPages;

    private List<Class<? extends BaseExtWidget>> extWidgets;

    private List<Class<? extends ExtAlertWidget<?>>> extAlertWidgets;

    private List<Class<? extends SSOLoginFormPanel>> ssoLoginFormPanels;

    private Map<String, Class<? extends ReportletConf>> reportletConfs;

    private Map<String, Class<? extends AccountRuleConf>> accountRuleConfs;

    private Map<String, Class<? extends PasswordRuleConf>> passwordRuleConfs;

    private Map<String, Class<? extends PullCorrelationRuleConf>> pullCorrelationRuleConfs;

    private Map<String, Class<? extends PushCorrelationRuleConf>> pushCorrelationRuleConfs;

    private List<Class<? extends AbstractResource>> resources;

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
        idmPages = new ArrayList<>();
        extPages = new ArrayList<>();
        extWidgets = new ArrayList<>();
        extAlertWidgets = new ArrayList<>();
        ssoLoginFormPanels = new ArrayList<>();
        reportletConfs = new HashMap<>();
        accountRuleConfs = new HashMap<>();
        passwordRuleConfs = new HashMap<>();
        pullCorrelationRuleConfs = new HashMap<>();
        pushCorrelationRuleConfs = new HashMap<>();
        resources = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(BasePage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractBinaryPreviewer.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtPage.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseExtWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExtAlertWidget.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(SSOLoginFormPanel.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ReportletConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccountRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PasswordRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PullCorrelationRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(PushCorrelationRuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AbstractResource.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(ExternalResourceProvider.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AnyWizardBuilderAdditionalSteps.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(StatusProvider.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(VirSchemaDetailsPanelProvider.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AnyDirectoryPanelAditionalActionLinksProvider.class));

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
                    } else if (ExtAlertWidget.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(ExtWidget.class)) {
                            extAlertWidgets.add((Class<? extends ExtAlertWidget<?>>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    ExtWidget.class.getName(), clazz.getName());
                        }
                    } else if (BasePage.class.isAssignableFrom(clazz)) {
                        if (clazz.isAnnotationPresent(IdMPage.class)) {
                            idmPages.add((Class<? extends BasePage>) clazz);
                        } else {
                            pages.add((Class<? extends BasePage>) clazz);
                        }
                    } else if (AbstractBinaryPreviewer.class.isAssignableFrom(clazz)) {
                        previewers.add((Class<? extends AbstractBinaryPreviewer>) clazz);
                    } else if (SSOLoginFormPanel.class.isAssignableFrom(clazz)) {
                        ssoLoginFormPanels.add((Class<? extends SSOLoginFormPanel>) clazz);
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
                            resources.add((Class<? extends AbstractResource>) clazz);
                        } else {
                            LOG.error("Could not find annotation {} in {}, ignoring",
                                    Resource.class.getName(), clazz.getName());
                        }
                    } else if (ExternalResourceProvider.class.isAssignableFrom(clazz)) {
                        if (!clazz.equals(EmptyExternalResourceProvider.class)
                                && !clazz.isInstance(resourceProvider)) {

                            resourceProvider = (ExternalResourceProvider) clazz.
                                    getConstructor().newInstance();
                        }
                    } else if (AnyWizardBuilderAdditionalSteps.class.isAssignableFrom(clazz)) {
                        if (!clazz.equals(EmptyAnyWizardBuilderAdditionalSteps.class)
                                && !clazz.isInstance(anyWizardBuilderAdditionalSteps)) {

                            anyWizardBuilderAdditionalSteps = (AnyWizardBuilderAdditionalSteps) clazz.
                                    getConstructor().newInstance();
                        }
                    } else if (StatusProvider.class.isAssignableFrom(clazz)) {
                        if (!clazz.equals(EmptyStatusProvider.class)
                                && !clazz.isInstance(statusProvider)) {

                            statusProvider = (StatusProvider) clazz.
                                    getConstructor().newInstance();
                        }
                    } else if (VirSchemaDetailsPanelProvider.class.isAssignableFrom(clazz)) {
                        if (!clazz.equals(EmptyVirSchemaDetailsPanelProvider.class)
                                && !clazz.isInstance(virSchemaDetailsPanelProvider)) {

                            virSchemaDetailsPanelProvider = (VirSchemaDetailsPanelProvider) clazz.
                                    getConstructor().newInstance();
                        }
                    } else if (AnyDirectoryPanelAditionalActionLinksProvider.class.isAssignableFrom(clazz)) {
                        if (!clazz.equals(EmptyAnyDirectoryPanelAditionalActionLinksProvider.class)
                                && !clazz.isInstance(anyDirectoryPanelAditionalActionLinksProvider)) {

                            anyDirectoryPanelAditionalActionLinksProvider =
                                    (AnyDirectoryPanelAditionalActionLinksProvider) clazz.
                                            getConstructor().newInstance();

                        }
                    }
                }
            } catch (Throwable t) {
                LOG.warn("Could not inspect class {}", bd.getBeanClassName(), t);
            }
        });
        pages = Collections.unmodifiableList(pages);
        previewers = Collections.unmodifiableList(previewers);

        idmPages.sort((o1, o2) -> ObjectUtils.compare(
                o1.getAnnotation(IdMPage.class).priority(),
                o2.getAnnotation(IdMPage.class).priority()));
        idmPages = Collections.unmodifiableList(idmPages);

        extPages.sort((o1, o2) -> ObjectUtils.compare(
                o1.getAnnotation(ExtPage.class).priority(),
                o2.getAnnotation(ExtPage.class).priority()));
        extPages = Collections.unmodifiableList(extPages);

        extWidgets.sort((o1, o2) -> ObjectUtils.compare(
                o1.getAnnotation(ExtWidget.class).priority(),
                o2.getAnnotation(ExtWidget.class).priority()));
        extWidgets = Collections.unmodifiableList(extWidgets);

        extAlertWidgets.sort((o1, o2) -> ObjectUtils.compare(
                o1.getAnnotation(ExtWidget.class).priority(),
                o2.getAnnotation(ExtWidget.class).priority()));
        extAlertWidgets = Collections.unmodifiableList(extAlertWidgets);

        ssoLoginFormPanels = Collections.unmodifiableList(ssoLoginFormPanels);

        reportletConfs = Collections.unmodifiableMap(reportletConfs);
        accountRuleConfs = Collections.unmodifiableMap(accountRuleConfs);
        passwordRuleConfs = Collections.unmodifiableMap(passwordRuleConfs);
        pullCorrelationRuleConfs = Collections.unmodifiableMap(pullCorrelationRuleConfs);
        pushCorrelationRuleConfs = Collections.unmodifiableMap(pushCorrelationRuleConfs);

        resources = Collections.unmodifiableList(resources);

        LOG.debug("Binary previewers found: {}", previewers);
        LOG.debug("Extension pages found: {}", extPages);
        LOG.debug("Extension widgets found: {}", extWidgets);
        LOG.debug("Extension alert widgets found: {}", extAlertWidgets);
        LOG.debug("SSO Login pages found: {}", ssoLoginFormPanels);
        LOG.debug("Reportlet configurations found: {}", reportletConfs);
        LOG.debug("Account Rule configurations found: {}", accountRuleConfs);
        LOG.debug("Password Rule configurations found: {}", passwordRuleConfs);
        LOG.debug("Pull Correlation Rule configurations found: {}", pullCorrelationRuleConfs);
        LOG.debug("Push Correlation Rule configurations found: {}", pushCorrelationRuleConfs);
        LOG.debug("Wicket Resources found: {}", resources);
        LOG.debug("{}: {}", ExternalResourceProvider.class.getSimpleName(),
                resourceProvider.getClass().getSimpleName());
        LOG.debug("{}: {}", AnyWizardBuilderAdditionalSteps.class.getSimpleName(),
                anyWizardBuilderAdditionalSteps.getClass().getSimpleName());
        LOG.debug("{}: {}", StatusProvider.class.getSimpleName(),
                statusProvider.getClass().getSimpleName());
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

    public ExternalResourceProvider getResourceProvider() {
        return resourceProvider;
    }

    public AnyWizardBuilderAdditionalSteps getAnyWizardBuilderAdditionalSteps() {
        return anyWizardBuilderAdditionalSteps;
    }

    public StatusProvider getStatusProvider() {
        return statusProvider;
    }

    public VirSchemaDetailsPanelProvider getVirSchemaDetailsPanelProvider() {
        return virSchemaDetailsPanelProvider;
    }

    public AnyDirectoryPanelAditionalActionLinksProvider getAnyDirectoryPanelAditionalActionLinksProvider() {
        return anyDirectoryPanelAditionalActionLinksProvider;
    }

    public List<Class<? extends BasePage>> getPageClasses() {
        return pages;
    }

    public List<Class<? extends BasePage>> getIdMPageClasses() {
        return idmPages;
    }

    public List<Class<? extends BaseExtPage>> getExtPageClasses() {
        return extPages;
    }

    public List<Class<? extends BaseExtWidget>> getExtWidgetClasses() {
        return extWidgets;
    }

    public List<Class<? extends ExtAlertWidget<?>>> getExtAlertWidgetClasses() {
        return extAlertWidgets;
    }

    public List<Class<? extends SSOLoginFormPanel>> getSSOLoginFormPanels() {
        return ssoLoginFormPanels;
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

    public List<Class<? extends AbstractResource>> getResources() {
        return resources;
    }
}
