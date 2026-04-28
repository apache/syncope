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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.collections.CircularFifoQueue;
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

            String hostname = null;
            try {
                hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOG.error("Could not get host name", e);
            }

            SYSTEM_INFO = new SystemInfo(
                    hostname,
                    operatingSystemMXBean.getName()
                    + ' ' + operatingSystemMXBean.getVersion()
                    + ' ' + operatingSystemMXBean.getArch(),
                    runtimeMXBean.getVmName()
                    + ' ' + System.getProperty("java.version")
                    + ' ' + runtimeMXBean.getVmVendor(),
                    operatingSystemMXBean.getAvailableProcessors(),
                    runtimeMXBean.getStartTime(),
                    new CircularFifoQueue<>(10));
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

    protected final SecurityQuestionDAO securityQuestionDAO;

    protected final NotificationDAO notificationDAO;

    protected final PersistenceInfoDAO persistenceInfoDAO;

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
            final SecurityQuestionDAO securityQuestionDAO,
            final PersistenceInfoDAO persistenceInfoDAO,
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
        this.securityQuestionDAO = securityQuestionDAO;
        this.persistenceInfoDAO = persistenceInfoDAO;
        this.bundleManager = bundleManager;
        this.implLookup = implLookup;
    }

    protected void buildPlatform() {
        synchronized (this) {
            if (PLATFORM_INFO == null) {
                PLATFORM_INFO = new PlatformInfo(
                        new HashSet<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new HashSet<>(),
                        new HashSet<>(),
                        new HashSet<>());

                PLATFORM_INFO.connIdLocations().addAll(
                        bundleManager.getLocations().stream().map(URI::toASCIIString).toList());

                ImplementationTypesHolder.getInstance().getValues().forEach((typeName, typeInterface) -> {
                    Optional.ofNullable(implLookup.getClassNames(typeName)).
                            ifPresent(classNames -> PLATFORM_INFO.javaImplInfos().
                            add(new JavaImplInfo(typeName, classNames)));
                });
            }

            PLATFORM_INFO.entitlements().clear();
            PLATFORM_INFO.entitlements().addAll(EntitlementsHolder.getInstance().getValues());

            PLATFORM_INFO.implementationTypes().clear();
            PLATFORM_INFO.implementationTypes().addAll(ImplementationTypesHolder.getInstance().getValues().keySet());

            AuthContextUtils.runAsAdmin(AuthContextUtils.getDomain(), () -> {
                PLATFORM_INFO.anyTypes().clear();
                PLATFORM_INFO.anyTypes().addAll(anyTypeDAO.findAll().stream().
                        map(AnyType::getKey).toList());

                PLATFORM_INFO.userClasses().clear();
                PLATFORM_INFO.userClasses().addAll(anyTypeDAO.getUser().getClasses().stream().
                        map(AnyTypeClass::getKey).toList());

                PLATFORM_INFO.anyTypeClasses().clear();
                PLATFORM_INFO.anyTypeClasses().addAll(anyTypeClassDAO.findAll().stream().
                        map(AnyTypeClass::getKey).toList());

                PLATFORM_INFO.resources().clear();
                PLATFORM_INFO.resources().addAll(resourceDAO.findAll().stream().
                        map(ExternalResource::getKey).toList());
            });
        }
    }

    protected NumbersInfo buildNumbers(final String domain) {
        return AuthContextUtils.callAsAdmin(domain, () -> {
            String anyType1 = null;
            long totalAny1 = 0;
            Map<String, Long> any1ByRealm = Map.of();
            String anyType2 = null;
            long totalAny2 = 0;
            Map<String, Long> any2ByRealm = Map.of();

            Map<String, Long> anyObjectNumbers = anyObjectDAO.countByType();
            int i = 0;
            for (Iterator<Map.Entry<String, Long>> itor = anyObjectNumbers.entrySet().iterator();
                    i < 2 && itor.hasNext(); i++) {

                Map.Entry<String, Long> entry = itor.next();
                if (i == 0) {
                    anyType1 = entry.getKey();
                    totalAny1 = entry.getValue();
                    any1ByRealm = anyObjectDAO.countByRealm(entry.getKey());
                } else {
                    anyType2 = entry.getKey();
                    totalAny2 = entry.getValue();
                    any2ByRealm = anyObjectDAO.countByRealm(entry.getKey());
                }
            }

            NumbersInfo numbersInfo = new NumbersInfo(
                    userDAO.count(),
                    userDAO.countByRealm(),
                    userDAO.countByStatus(),
                    groupDAO.count(),
                    groupDAO.countByRealm(),
                    anyType1,
                    totalAny1,
                    any1ByRealm,
                    anyType2,
                    totalAny2,
                    any2ByRealm,
                    resourceDAO.count(),
                    roleDAO.count(),
                    new HashMap<>());

            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.RESOURCE.name(), numbersInfo.totalResources() > 0);
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.ACCOUNT_POLICY.name(), !policyDAO.findAll(AccountPolicy.class).isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.PASSWORD_POLICY.name(), !policyDAO.findAll(PasswordPolicy.class).isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.NOTIFICATION.name(), !notificationDAO.findAll().isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.PULL_TASK.name(), !taskDAO.findAll(TaskType.PULL).isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.ANY_TYPE.name(), !anyObjectNumbers.isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.SECURITY_QUESTION.name(), !securityQuestionDAO.findAll().isEmpty());
            numbersInfo.confCompleteness().put(
                    NumbersInfo.ConfItem.ROLE.name(), numbersInfo.totalRoles() > 0);

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
            SYSTEM_INFO.load().add(event.getPayload());
        }
    }
}
