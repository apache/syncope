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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import org.apache.syncope.core.misc.EntitlementsHolder;
import java.lang.reflect.Method;
import java.net.URI;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.to.PlatformTO;
import org.apache.syncope.common.lib.to.SyncopeTO;
import org.apache.syncope.core.misc.security.PasswordGenerator;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.ImplementationLookup.Type;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
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

    private static final int MB = 1024 * 1024;

    private static final SyncopeTO SYNCOPE_TO = new SyncopeTO();

    private static final PlatformTO PLATFORM_TO = new PlatformTO();

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
        synchronized (SYNCOPE_TO) {
            SYNCOPE_TO.setVersion(version);

            SYNCOPE_TO.setSelfRegAllowed(isSelfRegAllowed());
            SYNCOPE_TO.setPwdResetAllowed(isPwdResetAllowed());
            SYNCOPE_TO.setPwdResetRequiringSecurityQuestions(isPwdResetRequiringSecurityQuestions());

            if (bundleManager.getLocations() != null) {
                for (URI location : bundleManager.getLocations()) {
                    SYNCOPE_TO.getConnIdLocations().add(location.toASCIIString());
                }
            }

            SYNCOPE_TO.setAnyObjectWorkflowAdapter(AopUtils.getTargetClass(awfAdapter).getName());
            SYNCOPE_TO.setUserWorkflowAdapter(AopUtils.getTargetClass(uwfAdapter).getName());
            SYNCOPE_TO.setGroupWorkflowAdapter(AopUtils.getTargetClass(gwfAdapter).getName());

            SYNCOPE_TO.setAnyObjectProvisioningManager(aProvisioningManager.getClass().getName());
            SYNCOPE_TO.setUserProvisioningManager(uProvisioningManager.getClass().getName());
            SYNCOPE_TO.setGroupProvisioningManager(gProvisioningManager.getClass().getName());
            SYNCOPE_TO.setVirAttrCache(virAttrCache.getClass().getName());
            SYNCOPE_TO.setPasswordGenerator(passwordGenerator.getClass().getName());

            SYNCOPE_TO.getEntitlements().addAll(EntitlementsHolder.getInstance().getValues());

            SYNCOPE_TO.getReportlets().addAll(implLookup.getClassNames(Type.REPORTLET));
            SYNCOPE_TO.getAccountRules().addAll(implLookup.getClassNames(Type.ACCOUNT_RULE));
            SYNCOPE_TO.getPasswordRules().addAll(implLookup.getClassNames(Type.PASSWORD_RULE));
            SYNCOPE_TO.getMappingItemTransformers().addAll(implLookup.getClassNames(Type.MAPPING_ITEM_TRANSFORMER));
            SYNCOPE_TO.getTaskJobs().addAll(implLookup.getClassNames(Type.TASKJOBDELEGATE));
            SYNCOPE_TO.getReconciliationFilterBuilders().
                    addAll(implLookup.getClassNames(Type.RECONCILIATION_FILTER_BUILDER));
            SYNCOPE_TO.getLogicActions().addAll(implLookup.getClassNames(Type.LOGIC_ACTIONS));
            SYNCOPE_TO.getPropagationActions().addAll(implLookup.getClassNames(Type.PROPAGATION_ACTIONS));
            SYNCOPE_TO.getSyncActions().addAll(implLookup.getClassNames(Type.SYNC_ACTIONS));
            SYNCOPE_TO.getPushActions().addAll(implLookup.getClassNames(Type.PUSH_ACTIONS));
            SYNCOPE_TO.getSyncCorrelationRules().addAll(implLookup.getClassNames(Type.SYNC_CORRELATION_RULE));
            SYNCOPE_TO.getValidators().addAll(implLookup.getClassNames(Type.VALIDATOR));
            SYNCOPE_TO.getNotificationRecipientsProviders().
                    addAll(implLookup.getClassNames(Type.NOTIFICATION_RECIPIENTS_PROVIDER));
        }

        return SYNCOPE_TO;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public PlatformTO platform() {

        synchronized (PLATFORM_TO) {
            PlatformTO.PlatformLoad instant = new PlatformTO.PlatformLoad();

            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            PLATFORM_TO.setOs(operatingSystemMXBean.getName()
                    + " " + operatingSystemMXBean.getVersion()
                    + " " + operatingSystemMXBean.getArch());
            PLATFORM_TO.setAvailableProcessors(operatingSystemMXBean.getAvailableProcessors());
            instant.setSystemLoadAverage(operatingSystemMXBean.getSystemLoadAverage());

            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            PLATFORM_TO.setJvm(
                    runtimeMXBean.getVmName()
                    + " " + System.getProperty("java.version")
                    + " " + runtimeMXBean.getVmVendor());
            instant.setUptime(runtimeMXBean.getUptime());

            Runtime runtime = Runtime.getRuntime();
            instant.setTotalMemory(runtime.totalMemory() / MB);
            instant.setMaxMemory(runtime.maxMemory() / MB);
            instant.setFreeMemory(runtime.freeMemory() / MB);

            PLATFORM_TO.getLoad().add(instant);
        }

        return PLATFORM_TO;
    }

    @Override
    protected SyncopeTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
