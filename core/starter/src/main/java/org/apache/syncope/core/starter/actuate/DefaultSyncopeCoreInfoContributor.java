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

import jakarta.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.PayloadApplicationEvent;
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

    @Autowired
    protected HttpServletRequest request;

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final RoleDAO roleDAO;

    protected final PolicyDAO policyDAO;

    protected final TaskDAO taskDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final SecurityQuestionDAO securityQuestionDAO;

    protected final NotificationDAO notificationDAO;

    protected final PersistenceInfoDAO persistenceInfoDAO;

    protected final ConfParamOps confParamOps;

    protected final ConnIdBundleManager bundleManager;

    protected final ImplementationLookup implLookup;

    public DefaultSyncopeCoreInfoContributor(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ExternalResourceDAO resourceDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final RoleDAO roleDAO,
            final PolicyDAO policyDAO,
            final NotificationDAO notificationDAO,
            final TaskDAO taskDAO,
            final VirSchemaDAO virSchemaDAO,
            final SecurityQuestionDAO securityQuestionDAO,
            final PersistenceInfoDAO persistenceInfoDAO,
            final ConfParamOps confParamOps,
            final ConnIdBundleManager bundleManager,
            final ImplementationLookup implLookup) {

        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.resourceDAO = resourceDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.roleDAO = roleDAO;
        this.policyDAO = policyDAO;
        this.notificationDAO = notificationDAO;
        this.taskDAO = taskDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.securityQuestionDAO = securityQuestionDAO;
        this.persistenceInfoDAO = persistenceInfoDAO;
        this.confParamOps = confParamOps;
        this.bundleManager = bundleManager;
        this.implLookup = implLookup;
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

                PLATFORM_INFO.getConnIdLocations().addAll(bundleManager.getLocations().stream().
                        map(URI::toASCIIString).toList());

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

            AuthContextUtils.runAsAdmin(AuthContextUtils.getDomain(), () -> {
                PLATFORM_INFO.getAnyTypes().clear();
                PLATFORM_INFO.getAnyTypes().addAll(anyTypeDAO.findAll().stream().
                        map(AnyType::getKey).toList());

                PLATFORM_INFO.getUserClasses().clear();
                PLATFORM_INFO.getUserClasses().addAll(anyTypeDAO.getUser().getClasses().stream().
                        map(AnyTypeClass::getKey).toList());

                PLATFORM_INFO.getAnyTypeClasses().clear();
                PLATFORM_INFO.getAnyTypeClasses().addAll(anyTypeClassDAO.findAll().stream().
                        map(AnyTypeClass::getKey).toList());

                PLATFORM_INFO.getResources().clear();
                PLATFORM_INFO.getResources().addAll(resourceDAO.findAll().stream().
                        map(ExternalResource::getKey).toList());
            });
        }
    }

    protected NumbersInfo buildNumbers(final String domain) {
        return AuthContextUtils.callAsAdmin(domain, () -> {
            NumbersInfo numbersInfo = new NumbersInfo();

            numbersInfo.setTotalUsers(userDAO.count());
            numbersInfo.getUsersByRealm().putAll(userDAO.countByRealm());
            numbersInfo.getUsersByStatus().putAll(userDAO.countByStatus());

            numbersInfo.setTotalGroups(groupDAO.count());
            numbersInfo.getGroupsByRealm().putAll(groupDAO.countByRealm());

            Map<AnyType, Long> anyObjectNumbers = anyObjectDAO.countByType();
            int i = 0;
            for (Iterator<Map.Entry<AnyType, Long>> itor = anyObjectNumbers.entrySet().iterator();
                    i < 2 && itor.hasNext(); i++) {

                Map.Entry<AnyType, Long> entry = itor.next();
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
                    NumbersInfo.ConfItem.ACCOUNT_POLICY.name(), !policyDAO.findAll(AccountPolicy.class).isEmpty());
            numbersInfo.getConfCompleteness().put(
                    NumbersInfo.ConfItem.PASSWORD_POLICY.name(), !policyDAO.findAll(PasswordPolicy.class).isEmpty());
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

            return numbersInfo;
        });
    }

    protected void buildSystem() {
        synchronized (MONITOR) {
            initSystemInfo();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void contribute(final Info.Builder builder) {
        buildPlatform();
        builder.withDetail("platform", PLATFORM_INFO);

        builder.withDetail("persistence", persistenceInfoDAO.info());

        builder.withDetail(
                "numbers",
                buildNumbers(Optional.ofNullable(request.getHeader(RESTHeaders.DOMAIN)).
                        orElse(SyncopeConstants.MASTER_DOMAIN)));

        buildSystem();
        builder.withDetail("system", SYSTEM_INFO);
    }

    @Override
    public void addLoadInstant(final PayloadApplicationEvent<SystemInfo.LoadInstant> event) {
        synchronized (MONITOR) {
            initSystemInfo();
            SYSTEM_INFO.getLoad().add(event.getPayload());
        }
    }
}
