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

import static org.apache.syncope.core.logic.AbstractLogic.LOG;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.core.logic.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.misc.spring.ResourceWithFallbackLoader;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.provisioning.api.AttributableTransformer;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.java.notification.NotificationManagerImpl;
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
    private AttributableTransformer attrTransformer;

    @Autowired
    private UserWorkflowAdapter uwfAdapter;

    @Autowired
    private GroupWorkflowAdapter gwfAdapter;

    @Autowired
    private UserProvisioningManager uProvisioningManager;

    @Autowired
    private GroupProvisioningManager gProvisioningManager;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

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

        syncopeTO.setAttributableTransformer(attrTransformer.getClass().getName());

        syncopeTO.setUserWorkflowAdapter(AopUtils.getTargetClass(uwfAdapter).getName());
        syncopeTO.setGroupWorkflowAdapter(AopUtils.getTargetClass(gwfAdapter).getName());

        syncopeTO.setUserProvisioningManager(uProvisioningManager.getClass().getName());
        syncopeTO.setGroupProvisioningManager(gProvisioningManager.getClass().getName());

        syncopeTO.getReportlets().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.REPORTLET));
        syncopeTO.getTaskJobs().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.TASKJOB));
        syncopeTO.getPropagationActions().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.PROPAGATION_ACTIONS));
        syncopeTO.getSyncActions().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_ACTIONS));
        syncopeTO.getPushActions().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.PUSH_ACTIONS));
        syncopeTO.getSyncCorrelationRules().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.SYNC_CORRELATION_RULE));
        syncopeTO.getPushCorrelationRules().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.PUSH_CORRELATION_RULE));
        syncopeTO.getValidators().addAll(
                classNamesLoader.getClassNames(ImplementationClassNamesLoader.Type.VALIDATOR));

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
