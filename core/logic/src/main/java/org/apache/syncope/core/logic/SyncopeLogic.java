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
import org.apache.syncope.core.provisioning.api.EntitlementsHolder;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.ImplementationLookup.Type;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.ConfDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
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

@Transactional(readOnly = true)
@Component
public class SyncopeLogic extends AbstractLogic<AbstractBaseBean> {

    private static final Object MONITOR = new Object();

    private static PlatformInfo PLATFORM_INFO;

    private static SystemInfo SYSTEM_INFO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private SecurityQuestionDAO securityQuestionDAO;

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

    public boolean isSelfRegAllowed() {
        return confDAO.find("selfRegistration.allowed", "false").getValues().get(0).getBooleanValue();
    }

    public boolean isPwdResetAllowed() {
        return confDAO.find("passwordReset.allowed", "false").getValues().get(0).getBooleanValue();
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        return confDAO.find("passwordReset.securityQuestion", "true").getValues().get(0).getBooleanValue();
    }

    @PreAuthorize("isAuthenticated()")
    public PlatformInfo platform() {
        synchronized (MONITOR) {
            if (PLATFORM_INFO == null) {
                PLATFORM_INFO = new PlatformInfo();
                PLATFORM_INFO.setVersion(version);

                if (bundleManager.getLocations() != null) {
                    for (URI location : bundleManager.getLocations()) {
                        PLATFORM_INFO.getConnIdLocations().add(location.toASCIIString());
                    }
                }

                PLATFORM_INFO.setAnyObjectWorkflowAdapter(AopUtils.getTargetClass(awfAdapter).getName());
                PLATFORM_INFO.setUserWorkflowAdapter(AopUtils.getTargetClass(uwfAdapter).getName());
                PLATFORM_INFO.setGroupWorkflowAdapter(AopUtils.getTargetClass(gwfAdapter).getName());

                PLATFORM_INFO.setAnyObjectProvisioningManager(aProvisioningManager.getClass().getName());
                PLATFORM_INFO.setUserProvisioningManager(uProvisioningManager.getClass().getName());
                PLATFORM_INFO.setGroupProvisioningManager(gProvisioningManager.getClass().getName());
                PLATFORM_INFO.setVirAttrCache(virAttrCache.getClass().getName());
                PLATFORM_INFO.setPasswordGenerator(passwordGenerator.getClass().getName());

                PLATFORM_INFO.getReportletConfs().addAll(implLookup.getClassNames(Type.REPORTLET_CONF));
                PLATFORM_INFO.getAccountRules().addAll(implLookup.getClassNames(Type.ACCOUNT_RULE_CONF));
                PLATFORM_INFO.getPasswordRules().addAll(implLookup.getClassNames(Type.PASSWORD_RULE_CONF));
                PLATFORM_INFO.getMappingItemTransformers().addAll(
                        implLookup.getClassNames(Type.MAPPING_ITEM_TRANSFORMER));
                PLATFORM_INFO.getTaskJobs().addAll(implLookup.getClassNames(Type.TASKJOBDELEGATE));
                PLATFORM_INFO.getReconciliationFilterBuilders().
                        addAll(implLookup.getClassNames(Type.RECONCILIATION_FILTER_BUILDER));
                PLATFORM_INFO.getLogicActions().addAll(implLookup.getClassNames(Type.LOGIC_ACTIONS));
                PLATFORM_INFO.getPropagationActions().addAll(implLookup.getClassNames(Type.PROPAGATION_ACTIONS));
                PLATFORM_INFO.getPullActions().addAll(implLookup.getClassNames(Type.PULL_ACTIONS));
                PLATFORM_INFO.getPushActions().addAll(implLookup.getClassNames(Type.PUSH_ACTIONS));
                PLATFORM_INFO.getPullCorrelationRules().addAll(implLookup.getClassNames(Type.PULL_CORRELATION_RULE));
                PLATFORM_INFO.getValidators().addAll(implLookup.getClassNames(Type.VALIDATOR));
                PLATFORM_INFO.getNotificationRecipientsProviders().
                        addAll(implLookup.getClassNames(Type.NOTIFICATION_RECIPIENTS_PROVIDER));
            }

            PLATFORM_INFO.setSelfRegAllowed(isSelfRegAllowed());
            PLATFORM_INFO.setPwdResetAllowed(isPwdResetAllowed());
            PLATFORM_INFO.setPwdResetRequiringSecurityQuestions(isPwdResetRequiringSecurityQuestions());

            PLATFORM_INFO.getEntitlements().clear();
            PLATFORM_INFO.getEntitlements().addAll(EntitlementsHolder.getInstance().getValues());
        }

        return PLATFORM_INFO;
    }

    private void initSystemInfo() {
        if (SYSTEM_INFO == null) {
            OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

            SYSTEM_INFO = new SystemInfo();
            try {
                SYSTEM_INFO.setHostname(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException e) {
                LOG.error("Could not get host name", e);
            }

            SYSTEM_INFO.setOs(operatingSystemMXBean.getName()
                    + " " + operatingSystemMXBean.getVersion()
                    + " " + operatingSystemMXBean.getArch());
            SYSTEM_INFO.setAvailableProcessors(operatingSystemMXBean.getAvailableProcessors());
            SYSTEM_INFO.setJvm(
                    runtimeMXBean.getVmName()
                    + " " + System.getProperty("java.version")
                    + " " + runtimeMXBean.getVmVendor());
            SYSTEM_INFO.setStartTime(runtimeMXBean.getStartTime());
        }
    }

    public void addLoadInstant(final SystemInfo.LoadInstant instant) {
        synchronized (MONITOR) {
            initSystemInfo();
            SYSTEM_INFO.getLoad().add(instant);
        }
    }

    @PreAuthorize("isAuthenticated()")
    public SystemInfo system() {
        synchronized (MONITOR) {
            initSystemInfo();
        }

        return SYSTEM_INFO;
    }

    @PreAuthorize("isAuthenticated()")
    public NumbersInfo numbers() {
        NumbersInfo numbersInfo = new NumbersInfo();

        numbersInfo.setTotalUsers(userDAO.count());
        numbersInfo.getUsersByRealm().putAll(userDAO.countByRealm());
        numbersInfo.getUsersByStatus().putAll(userDAO.countByStatus());

        numbersInfo.setTotalGroups(groupDAO.count());
        numbersInfo.getGroupsByRealm().putAll(groupDAO.countByRealm());

        Map<AnyType, Integer> anyObjectNumbers = anyObjectDAO.countByType();
        int i = 0;
        for (Iterator<Map.Entry<AnyType, Integer>> itor = anyObjectNumbers.entrySet().iterator();
                i < 2 && itor.hasNext(); i++) {

            Map.Entry<AnyType, Integer> entry = itor.next();
            if (i == 0) {
                numbersInfo.setAnyType1(entry.getKey().getKey());
                numbersInfo.setTotalAny1(entry.getValue());
                numbersInfo.getAny1ByRealm().putAll(anyObjectDAO.countByRealm(entry.getKey()));
            } else if (i == 1) {
                numbersInfo.setAnyType2(entry.getKey().getKey());
                numbersInfo.setTotalAny2(entry.getValue());
                numbersInfo.getAny2ByRealm().putAll(anyObjectDAO.countByRealm(entry.getKey()));
            }
        }

        numbersInfo.setTotalResources(resourceDAO.count());

        numbersInfo.setTotalRoles(roleDAO.count());

        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.RESOURCE, numbersInfo.getTotalResources() > 0);
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ACCOUNT_POLICY, !policyDAO.find(AccountPolicy.class).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.PASSWORD_POLICY, !policyDAO.find(PasswordPolicy.class).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.NOTIFICATION, !notificationDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.SYNC_TASK, !taskDAO.findAll(TaskType.PULL).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.VIR_SCHEMA, !virSchemaDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ANY_TYPE, !anyObjectNumbers.isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.SECURITY_QUESTION, !securityQuestionDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ROLE, numbersInfo.getTotalRoles() > 0);

        return numbersInfo;
    }

    @Override
    protected AbstractBaseBean resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
