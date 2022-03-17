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
package org.apache.syncope.core.starter.actuate;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.logic.LogicProperties;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.jpa.PersistenceProperties;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.ProvisioningProperties;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class DefaultSyncopeCoreInfoContributor implements SyncopeCoreInfoContributor, InfoContributor {

    protected static final Logger LOG = LoggerFactory.getLogger(DefaultSyncopeCoreInfoContributor.class);

    protected static final Object MONITOR = new Object();

    protected static PlatformInfo PLATFORM_INFO;

    protected static SystemInfo SYSTEM_INFO;

    protected static final Pattern THREADPOOLTASKEXECUTOR_PATTERN = Pattern.compile(
            ".*, pool size = ([0-9]+), "
            + "active threads = ([0-9]+), "
            + "queued tasks = ([0-9]+), "
            + "completed tasks = ([0-9]+).*");

    protected static void setTaskExecutorInfo(final String toString, final NumbersInfo.TaskExecutorInfo info) {
        Matcher matcher = THREADPOOLTASKEXECUTOR_PATTERN.matcher(toString);
        if (matcher.matches() && matcher.groupCount() == 4) {
            try {
                info.setSize(Integer.valueOf(matcher.group(1)));
            } catch (NumberFormatException e) {
                LOG.error("While parsing thread pool size", e);
            }
            try {
                info.setActive(Integer.valueOf(matcher.group(2)));
            } catch (NumberFormatException e) {
                LOG.error("While parsing active threads #", e);
            }
            try {
                info.setQueued(Integer.valueOf(matcher.group(3)));
            } catch (NumberFormatException e) {
                LOG.error("While parsing queued threads #", e);
            }
            try {
                info.setCompleted(Integer.valueOf(matcher.group(4)));
            } catch (NumberFormatException e) {
                LOG.error("While parsing completed threads #", e);
            }
        }
    }

    protected static void initSystemInfo() {
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
                    + ' ' + operatingSystemMXBean.getVersion()
                    + ' ' + operatingSystemMXBean.getArch());
            SYSTEM_INFO.setAvailableProcessors(operatingSystemMXBean.getAvailableProcessors());
            SYSTEM_INFO.setJvm(
                    runtimeMXBean.getVmName()
                    + ' ' + System.getProperty("java.version")
                    + ' ' + runtimeMXBean.getVmVendor());
            SYSTEM_INFO.setStartTime(runtimeMXBean.getStartTime());
        }
    }

    private final SecurityProperties securityProperties;

    private final PersistenceProperties persistenceProperties;

    private final ProvisioningProperties provisioningProperties;

    private final LogicProperties logicProperties;

    private final AnyTypeDAO anyTypeDAO;

    private final AnyTypeClassDAO anyTypeClassDAO;

    private final UserDAO userDAO;

    private final GroupDAO groupDAO;

    private final AnyObjectDAO anyObjectDAO;

    private final ExternalResourceDAO resourceDAO;

    private final ConfParamOps confParamOps;

    private final ServiceOps serviceOps;

    private final ConnIdBundleManager bundleManager;

    private final PropagationTaskExecutor propagationTaskExecutor;

    private final AnyObjectWorkflowAdapter awfAdapter;

    private final UserWorkflowAdapter uwfAdapter;

    private final GroupWorkflowAdapter gwfAdapter;

    private final AnyObjectProvisioningManager aProvisioningManager;

    private final UserProvisioningManager uProvisioningManager;

    private final GroupProvisioningManager gProvisioningManager;

    private final VirAttrCache virAttrCache;

    private final NotificationManager notificationManager;

    private final AuditManager auditManager;

    private final PasswordGenerator passwordGenerator;

    private final EntityFactory entityFactory;

    private final PlainSchemaDAO plainSchemaDAO;

    private final PlainAttrDAO plainAttrDAO;

    private final PlainAttrValueDAO plainAttrValueDAO;

    private final AnySearchDAO anySearchDAO;

    private final ImplementationLookup implLookup;

    private final PolicyDAO policyDAO;

    private final NotificationDAO notificationDAO;

    private final TaskDAO taskDAO;

    private final VirSchemaDAO virSchemaDAO;

    private final RoleDAO roleDAO;

    private final SecurityQuestionDAO securityQuestionDAO;

    private final ThreadPoolTaskExecutor asyncConnectorFacadeExecutor;

    private final ThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor;

    public DefaultSyncopeCoreInfoContributor(
            final SecurityProperties securityProperties,
            final PersistenceProperties persistenceProperties,
            final ProvisioningProperties provisioningProperties,
            final LogicProperties logicProperties,
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final ExternalResourceDAO resourceDAO,
            final ConfParamOps confParamOps,
            final ServiceOps serviceOps,
            final ConnIdBundleManager bundleManager,
            final PropagationTaskExecutor propagationTaskExecutor,
            final AnyObjectWorkflowAdapter awfAdapter,
            final UserWorkflowAdapter uwfAdapter,
            final GroupWorkflowAdapter gwfAdapter,
            final AnyObjectProvisioningManager aProvisioningManager,
            final UserProvisioningManager uProvisioningManager,
            final GroupProvisioningManager gProvisioningManager,
            final VirAttrCache virAttrCache,
            final NotificationManager notificationManager,
            final AuditManager auditManager,
            final PasswordGenerator passwordGenerator,
            final EntityFactory entityFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final PlainAttrDAO plainAttrDAO,
            final PlainAttrValueDAO plainAttrValueDAO,
            final AnySearchDAO anySearchDAO,
            final ImplementationLookup implLookup,
            final PolicyDAO policyDAO,
            final NotificationDAO notificationDAO,
            final TaskDAO taskDAO,
            final VirSchemaDAO virSchemaDAO,
            final RoleDAO roleDAO,
            final SecurityQuestionDAO securityQuestionDAO,
            final ThreadPoolTaskExecutor asyncConnectorFacadeExecutor,
            final ThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor) {

        this.securityProperties = securityProperties;
        this.persistenceProperties = persistenceProperties;
        this.provisioningProperties = provisioningProperties;
        this.logicProperties = logicProperties;
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.resourceDAO = resourceDAO;
        this.confParamOps = confParamOps;
        this.serviceOps = serviceOps;
        this.bundleManager = bundleManager;
        this.propagationTaskExecutor = propagationTaskExecutor;
        this.awfAdapter = awfAdapter;
        this.uwfAdapter = uwfAdapter;
        this.gwfAdapter = gwfAdapter;
        this.aProvisioningManager = aProvisioningManager;
        this.uProvisioningManager = uProvisioningManager;
        this.gProvisioningManager = gProvisioningManager;
        this.virAttrCache = virAttrCache;
        this.notificationManager = notificationManager;
        this.auditManager = auditManager;
        this.passwordGenerator = passwordGenerator;
        this.entityFactory = entityFactory;
        this.plainSchemaDAO = plainSchemaDAO;
        this.plainAttrDAO = plainAttrDAO;
        this.plainAttrValueDAO = plainAttrValueDAO;
        this.anySearchDAO = anySearchDAO;
        this.implLookup = implLookup;
        this.policyDAO = policyDAO;
        this.notificationDAO = notificationDAO;
        this.taskDAO = taskDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.roleDAO = roleDAO;
        this.securityQuestionDAO = securityQuestionDAO;
        this.asyncConnectorFacadeExecutor = asyncConnectorFacadeExecutor;
        this.propagationTaskExecutorAsyncExecutor = propagationTaskExecutorAsyncExecutor;
    }

    protected boolean isSelfRegAllowed() {
        return confParamOps.get(AuthContextUtils.getDomain(), "selfRegistration.allowed", false, Boolean.class);
    }

    protected boolean isPwdResetAllowed() {
        return confParamOps.get(AuthContextUtils.getDomain(), "passwordReset.allowed", false, Boolean.class);
    }

    protected boolean isPwdResetRequiringSecurityQuestions() {
        return confParamOps.get(AuthContextUtils.getDomain(), "passwordReset.securityQuestion", true, Boolean.class);
    }

    protected void buildPlatform() {
        synchronized (this) {
            if (PLATFORM_INFO == null) {
                PLATFORM_INFO = new PlatformInfo();
                PLATFORM_INFO.setKeymasterConfParamOps(AopUtils.getTargetClass(confParamOps).getName());
                PLATFORM_INFO.setKeymasterServiceOps(AopUtils.getTargetClass(serviceOps).getName());

                PLATFORM_INFO.getConnIdLocations().addAll(bundleManager.getLocations().stream().
                        map(URI::toASCIIString).collect(Collectors.toList()));

                PLATFORM_INFO.getWorkflowInfo().
                        setAnyObjectWorkflowAdapter(AopUtils.getTargetClass(awfAdapter).getName());
                PLATFORM_INFO.getWorkflowInfo().
                        setUserWorkflowAdapter(AopUtils.getTargetClass(uwfAdapter).getName());
                PLATFORM_INFO.getWorkflowInfo().
                        setGroupWorkflowAdapter(AopUtils.getTargetClass(gwfAdapter).getName());

                PLATFORM_INFO.getProvisioningInfo().
                        setAnyObjectProvisioningManager(AopUtils.getTargetClass(aProvisioningManager).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setUserProvisioningManager(AopUtils.getTargetClass(uProvisioningManager).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setGroupProvisioningManager(AopUtils.getTargetClass(gProvisioningManager).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setPropagationTaskExecutor(AopUtils.getTargetClass(propagationTaskExecutor).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setVirAttrCache(AopUtils.getTargetClass(virAttrCache).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setNotificationManager(AopUtils.getTargetClass(notificationManager).getName());
                PLATFORM_INFO.getProvisioningInfo().
                        setAuditManager(AopUtils.getTargetClass(auditManager).getName());

                PLATFORM_INFO.setPasswordGenerator(AopUtils.getTargetClass(passwordGenerator).getName());

                PLATFORM_INFO.getPersistenceInfo().
                        setEntityFactory(AopUtils.getTargetClass(entityFactory).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setPlainSchemaDAO(AopUtils.getTargetClass(plainSchemaDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setPlainAttrDAO(AopUtils.getTargetClass(plainAttrDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setPlainAttrValueDAO(AopUtils.getTargetClass(plainAttrValueDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setAnySearchDAO(AopUtils.getTargetClass(anySearchDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setUserDAO(AopUtils.getTargetClass(userDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setGroupDAO(AopUtils.getTargetClass(groupDAO).getName());
                PLATFORM_INFO.getPersistenceInfo().
                        setAnyObjectDAO(AopUtils.getTargetClass(anyObjectDAO).getName());

                ImplementationTypesHolder.getInstance().getValues().forEach((typeName, typeInterface) -> {
                    Set<String> classNames = implLookup.getClassNames(typeName);
                    if (classNames != null) {
                        JavaImplInfo javaImplInfo = new JavaImplInfo();
                        javaImplInfo.setType(typeName);
                        javaImplInfo.getClasses().addAll(classNames);

                        PLATFORM_INFO.getJavaImplInfos().add(javaImplInfo);
                    }
                });
            }

            PLATFORM_INFO.setSelfRegAllowed(isSelfRegAllowed());
            PLATFORM_INFO.setPwdResetAllowed(isPwdResetAllowed());
            PLATFORM_INFO.setPwdResetRequiringSecurityQuestions(isPwdResetRequiringSecurityQuestions());

            PLATFORM_INFO.getEntitlements().clear();
            PLATFORM_INFO.getEntitlements().addAll(EntitlementsHolder.getInstance().getValues());

            PLATFORM_INFO.getImplementationTypes().clear();
            PLATFORM_INFO.getImplementationTypes().addAll(ImplementationTypesHolder.getInstance().getValues().keySet());

            AuthContextUtils.callAsAdmin(AuthContextUtils.getDomain(), () -> {
                PLATFORM_INFO.getAnyTypes().clear();
                PLATFORM_INFO.getAnyTypes().addAll(anyTypeDAO.findAll().stream().
                        map(Entity::getKey).collect(Collectors.toList()));

                PLATFORM_INFO.getUserClasses().clear();
                PLATFORM_INFO.getUserClasses().addAll(anyTypeDAO.findUser().getClasses().stream().
                        map(Entity::getKey).collect(Collectors.toList()));

                PLATFORM_INFO.getAnyTypeClasses().clear();
                PLATFORM_INFO.getAnyTypeClasses().addAll(anyTypeClassDAO.findAll().stream().
                        map(Entity::getKey).collect(Collectors.toList()));

                PLATFORM_INFO.getResources().clear();
                PLATFORM_INFO.getResources().addAll(resourceDAO.findAll().stream().
                        map(Entity::getKey).collect(Collectors.toList()));
                return null;
            });
        }
    }

    protected NumbersInfo buildNumbers() {
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
            } else {
                numbersInfo.setAnyType2(entry.getKey().getKey());
                numbersInfo.setTotalAny2(entry.getValue());
                numbersInfo.getAny2ByRealm().putAll(anyObjectDAO.countByRealm(entry.getKey()));
            }
        }

        numbersInfo.setTotalResources(resourceDAO.count());

        numbersInfo.setTotalRoles(roleDAO.count());

        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.RESOURCE.name(), numbersInfo.getTotalResources() > 0);
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ACCOUNT_POLICY.name(), !policyDAO.find(AccountPolicy.class).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.PASSWORD_POLICY.name(), !policyDAO.find(PasswordPolicy.class).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.NOTIFICATION.name(), !notificationDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.PULL_TASK.name(), !taskDAO.findAll(TaskType.PULL).isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.VIR_SCHEMA.name(), !virSchemaDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ANY_TYPE.name(), !anyObjectNumbers.isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.SECURITY_QUESTION.name(), !securityQuestionDAO.findAll().isEmpty());
        numbersInfo.getConfCompleteness().put(
                NumbersInfo.ConfItem.ROLE.name(), numbersInfo.getTotalRoles() > 0);

        setTaskExecutorInfo(
                asyncConnectorFacadeExecutor.getThreadPoolExecutor().toString(),
                numbersInfo.getAsyncConnectorExecutor());
        setTaskExecutorInfo(
                propagationTaskExecutorAsyncExecutor.getThreadPoolExecutor().toString(),
                numbersInfo.getPropagationTaskExecutor());

        return numbersInfo;
    }

    protected void buildSystem() {
        synchronized (MONITOR) {
            initSystemInfo();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    @Override
    public void contribute(final Info.Builder builder) {
        buildPlatform();
        builder.withDetail("platform", PLATFORM_INFO);

        builder.withDetail("numbers", buildNumbers());

        buildSystem();
        builder.withDetail("system", SYSTEM_INFO);

        builder.withDetail("securityProperties", securityProperties);
        builder.withDetail("persistenceProperties", persistenceProperties);
        builder.withDetail("provisioningProperties", provisioningProperties);
        builder.withDetail("logicProperties", logicProperties);
    }

    @Override
    public void addLoadInstant(final PayloadApplicationEvent<SystemInfo.LoadInstant> event) {
        synchronized (MONITOR) {
            initSystemInfo();
            SYSTEM_INFO.getLoad().add(event.getPayload());
        }
    }
}
