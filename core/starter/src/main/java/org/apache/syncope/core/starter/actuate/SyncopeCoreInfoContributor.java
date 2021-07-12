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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.annotation.Resource;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.TaskType;
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
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class SyncopeCoreInfoContributor implements InfoContributor {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeCoreInfoContributor.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

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

    @EventListener
    public void addLoadInstant(final PayloadApplicationEvent<SystemInfo.LoadInstant> event) {
        synchronized (MONITOR) {
            initSystemInfo();
            SYSTEM_INFO.getLoad().add(event.getPayload());
        }
    }

    @Autowired
    protected AnyTypeDAO anyTypeDAO;

    @Autowired
    protected AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected ExternalResourceDAO resourceDAO;

    @Autowired
    protected ConfParamOps confParamOps;

    @Autowired
    protected ServiceOps serviceOps;

    @Autowired
    protected ConnIdBundleManager bundleManager;

    @Autowired
    protected PropagationTaskExecutor propagationTaskExecutor;

    @Autowired
    protected AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    protected UserWorkflowAdapter uwfAdapter;

    @Autowired
    protected GroupWorkflowAdapter gwfAdapter;

    @Autowired
    protected AnyObjectProvisioningManager aProvisioningManager;

    @Autowired
    protected UserProvisioningManager uProvisioningManager;

    @Autowired
    protected GroupProvisioningManager gProvisioningManager;

    @Autowired
    protected VirAttrCache virAttrCache;

    @Autowired
    protected NotificationManager notificationManager;

    @Autowired
    protected AuditManager auditManager;

    @Autowired
    protected PasswordGenerator passwordGenerator;

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected PlainAttrDAO plainAttrDAO;

    @Autowired
    protected PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    protected AnySearchDAO anySearchDAO;

    @Autowired
    protected ImplementationLookup implLookup;

    @Autowired
    protected PolicyDAO policyDAO;

    @Autowired
    protected NotificationDAO notificationDAO;

    @Autowired
    protected TaskDAO taskDAO;

    @Autowired
    protected VirSchemaDAO virSchemaDAO;

    @Autowired
    protected RoleDAO roleDAO;

    @Autowired
    protected SecurityQuestionDAO securityQuestionDAO;

    @Resource(name = "asyncConnectorFacadeExecutor")
    protected ThreadPoolTaskExecutor asyncConnectorFacadeExecutor;

    @Resource(name = "propagationTaskExecutorAsyncExecutor")
    protected ThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor;

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

                if (bundleManager.getLocations() != null) {
                    PLATFORM_INFO.getConnIdLocations().addAll(bundleManager.getLocations().stream().
                            map(URI::toASCIIString).collect(Collectors.toList()));
                }

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
        builder.withDetail(
                "platform",
                MAPPER.convertValue(PLATFORM_INFO, new TypeReference<Map<String, Object>>() {
                }));

        builder.withDetail(
                "numbers",
                MAPPER.convertValue(buildNumbers(), new TypeReference<Map<String, Object>>() {
                }));

        buildSystem();
        builder.withDetail(
                "system",
                MAPPER.convertValue(SYSTEM_INFO, new TypeReference<Map<String, Object>>() {
                }));
    }
}
