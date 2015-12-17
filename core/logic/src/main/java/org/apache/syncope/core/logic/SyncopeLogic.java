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
package org.apache.syncope.core.logic;

import org.apache.syncope.core.misc.EntitlementsHolder;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.core.misc.security.PasswordGenerator;
import org.apache.syncope.core.misc.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.ImplementationLookup.Type;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.java.notification.NotificationManagerImpl;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SyncopeLogic extends AbstractLogic<SyncopeTO> {

    @Autowired
    private ConfDAO confDAO;

    @Resource(name = "version")
    private String version;

    @Autowired
    private ConnIdBundleManager bundleManager;

    @Autowired
    private AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private GroupWorkflowAdapter gwfAdapter;

    @Autowired
    private AnyObjectProvisioningManager aProvisioningManager;

    @Autowired
    private UserProvisioningManager uProvisioningManager;

    @Autowired
    private GroupProvisioningManager gProvisioningManager;

    @Autowired
    private VirAttrCache virAttrCache;

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private ImplementationLookup implLookup;

    @Resource(name = "velocityResourceLoader")
    private ResourceWithFallbackLoader resourceLoader;

    @Transactional(readOnly = true)
    public boolean isSelfRegAllowed() {
        return confDAO.find("selfRegistration.allowed", "false").getValues().get(0).getBooleanValue();
    }

    @Transactional(readOnly = true)
    public boolean isPwdResetAllowed() {
        return confDAO.find("passwordReset.allowed", "false").getValues().get(0).getBooleanValue();
    }

    @Transactional(readOnly = true)
    public boolean isPwdResetRequiringSecurityQuestions() {
        return confDAO.find("passwordReset.securityQuestion", "true").getValues().get(0).getBooleanValue();
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public SyncopeTO info() {
        SyncopeTO syncopeTO = new SyncopeTO();
        syncopeTO.setVersion(version);

        syncopeTO.setSelfRegAllowed(isSelfRegAllowed());
        syncopeTO.setPwdResetAllowed(isPwdResetAllowed());
        syncopeTO.setPwdResetRequiringSecurityQuestions(isPwdResetRequiringSecurityQuestions());

        if (bundleManager.getLocations() != null) {
            for (URI location : bundleManager.getLocations()) {
                syncopeTO.getConnIdLocations().add(location.toASCIIString());
            }
        }

        syncopeTO.setAnyObjectWorkflowAdapter(AopUtils.getTargetClass(awfAdapter).getName());
        syncopeTO.setUserWorkflowAdapter(AopUtils.getTargetClass(uwfAdapter).getName());
        syncopeTO.setGroupWorkflowAdapter(AopUtils.getTargetClass(gwfAdapter).getName());

        syncopeTO.setAnyObjectProvisioningManager(aProvisioningManager.getClass().getName());
        syncopeTO.setUserProvisioningManager(uProvisioningManager.getClass().getName());
        syncopeTO.setGroupProvisioningManager(gProvisioningManager.getClass().getName());
        syncopeTO.setVirAttrCache(virAttrCache.getClass().getName());
        syncopeTO.setPasswordGenerator(passwordGenerator.getClass().getName());

        syncopeTO.getEntitlements().addAll(EntitlementsHolder.getInstance().getValues());

        syncopeTO.getReportlets().addAll(implLookup.getClassNames(Type.REPORTLET));
        syncopeTO.getAccountRules().addAll(implLookup.getClassNames(Type.ACCOUNT_RULE));
        syncopeTO.getPasswordRules().addAll(implLookup.getClassNames(Type.PASSWORD_RULE));
        syncopeTO.getMappingItemTransformers().addAll(implLookup.getClassNames(Type.MAPPING_ITEM_TRANSFORMER));
        syncopeTO.getTaskJobs().addAll(implLookup.getClassNames(Type.TASKJOBDELEGATE));
        syncopeTO.getReconciliationFilterBuilders().
                addAll(implLookup.getClassNames(Type.RECONCILIATION_FILTER_BUILDER));
        syncopeTO.getLogicActions().addAll(implLookup.getClassNames(Type.LOGIC_ACTIONS));
        syncopeTO.getPropagationActions().addAll(implLookup.getClassNames(Type.PROPAGATION_ACTIONS));
        syncopeTO.getSyncActions().addAll(implLookup.getClassNames(Type.SYNC_ACTIONS));
        syncopeTO.getPushActions().addAll(implLookup.getClassNames(Type.PUSH_ACTIONS));
        syncopeTO.getSyncCorrelationRules().addAll(implLookup.getClassNames(Type.SYNC_CORRELATION_RULE));
        syncopeTO.getValidators().addAll(implLookup.getClassNames(Type.VALIDATOR));
        syncopeTO.getNotificationRecipientsProviders().
                addAll(implLookup.getClassNames(Type.NOTIFICATION_RECIPIENTS_PROVIDER));

        Set<String> htmlTemplates = new HashSet<>();
        Set<String> textTemplates = new HashSet<>();
        try {
            for (org.springframework.core.io.Resource resource : resourceLoader.getResources(
                    NotificationManagerImpl.MAIL_TEMPLATES + "*.vm")) {

                String template = resource.getURL().toExternalForm();
                if (template.endsWith(NotificationManagerImpl.MAIL_TEMPLATE_HTML_SUFFIX)) {
                    htmlTemplates.add(template.substring(template.indexOf(NotificationManagerImpl.MAIL_TEMPLATES) + 14,
                            template.indexOf(NotificationManagerImpl.MAIL_TEMPLATE_HTML_SUFFIX)));
                } else if (template.endsWith(NotificationManagerImpl.MAIL_TEMPLATE_TEXT_SUFFIX)) {
                    textTemplates.add(template.substring(template.indexOf(NotificationManagerImpl.MAIL_TEMPLATES) + 14,
                            template.indexOf(NotificationManagerImpl.MAIL_TEMPLATE_TEXT_SUFFIX)));
                } else {
                    LOG.warn("Unexpected template found: {}, ignoring...", template);
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for mail templates", e);
        }
        // Only templates available both as HTML and TEXT are considered
        htmlTemplates.retainAll(textTemplates);
        syncopeTO.getMailTemplates().addAll(htmlTemplates);

        return syncopeTO;
    }

    @Override
    protected SyncopeTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
