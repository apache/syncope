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

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.JavaImplInfo;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ImplementationTypesHolder;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
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
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Component
public class SyncopeLogic extends AbstractLogic<EntityTO> {

    private static final Pattern THREADPOOLTASKEXECUTOR_PATTERN = Pattern.compile(
            ".*, pool size = ([0-9]+), "
            + "active threads = ([0-9]+), "
            + "queued tasks = ([0-9]+), "
            + "completed tasks = ([0-9]+).*");

    private static final Object MONITOR = new Object();

    private static PlatformInfo PLATFORM_INFO;

    private static SystemInfo SYSTEM_INFO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

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
    private AnySearchDAO searchDAO;

    @Autowired
    private GroupDataBinder groupDataBinder;

    @Autowired
    private ConfParamOps confParamOps;

    @Autowired
    private ServiceOps serviceOps;

    @Resource(name = "version")
    private String version;

    @Resource(name = "buildNumber")
    private String buildNumber;

    @Autowired
    private ConnIdBundleManager bundleManager;

    @Autowired
    private PropagationTaskExecutor propagationTaskExecutor;

    @Autowired
    private AnyObjectWorkflowAdapter awfAdapter;

    @Autowired
    private ContentExporter exporter;

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
    private NotificationManager notificationManager;

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private PasswordGenerator passwordGenerator;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PlainAttrDAO plainAttrDAO;

    @Autowired
    private PlainAttrValueDAO plainAttrValueDAO;

    @Autowired
    private AnySearchDAO anySearchDAO;

    @Autowired
    private ImplementationLookup implLookup;

    @Resource(name = "asyncConnectorFacadeExecutor")
    private ThreadPoolTaskExecutor asyncConnectorFacadeExecutor;

    @Resource(name = "propagationTaskExecutorAsyncExecutor")
    private ThreadPoolTaskExecutor propagationTaskExecutorAsyncExecutor;

    public boolean isSelfRegAllowed() {
        return confParamOps.get(AuthContextUtils.getDomain(), "selfRegistration.allowed", false, Boolean.class);
    }

    public boolean isPwdResetAllowed() {
        return confParamOps.get(AuthContextUtils.getDomain(), "passwordReset.allowed", false, Boolean.class);
    }

    public boolean isPwdResetRequiringSecurityQuestions() {
        return confParamOps.get(AuthContextUtils.getDomain(), "passwordReset.securityQuestion", true, Boolean.class);
    }

    @PreAuthorize("isAuthenticated()")
    public PlatformInfo platform() {
        synchronized (MONITOR) {
            if (PLATFORM_INFO == null) {
                PLATFORM_INFO = new PlatformInfo();
                PLATFORM_INFO.setVersion(version);
                PLATFORM_INFO.setBuildNumber(buildNumber);
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

        return PLATFORM_INFO;
    }

    private static void initSystemInfo() {
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
    public static void addLoadInstant(final PayloadApplicationEvent<SystemInfo.LoadInstant> event) {
        synchronized (MONITOR) {
            initSystemInfo();
            SYSTEM_INFO.getLoad().add(event.getPayload());
        }
    }

    @PreAuthorize("isAuthenticated()")
    public static SystemInfo system() {
        synchronized (MONITOR) {
            initSystemInfo();
        }

        return SYSTEM_INFO;
    }

    private void setTaskExecutorInfo(final String toString, final NumbersInfo.TaskExecutorInfo info) {
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

    @PreAuthorize("isAuthenticated()")
    public Pair<Integer, List<GroupTO>> searchAssignableGroups(
            final String realm,
            final String term,
            final int page,
            final int size) {

        AssignableCond assignableCond = new AssignableCond();
        assignableCond.setRealmFullPath(realm);

        SearchCond searchCond;
        if (StringUtils.isNotBlank(term)) {
            AnyCond termCond = new AnyCond(AttrCond.Type.ILIKE);
            termCond.setSchema("name");

            String termSearchableValue = (term.startsWith("*") && !term.endsWith("*"))
                    ? term + '%'
                    : (!term.startsWith("*") && term.endsWith("*"))
                    ? '%' + term
                    : (term.startsWith("*") && term.endsWith("*")
                    ? term : '%' + term + '%');
            termCond.setExpression(termSearchableValue);

            searchCond = SearchCond.getAnd(
                    SearchCond.getLeaf(assignableCond),
                    SearchCond.getLeaf(termCond));
        } else {
            searchCond = SearchCond.getLeaf(assignableCond);
        }

        int count = searchDAO.count(SyncopeConstants.FULL_ADMIN_REALMS, searchCond, AnyTypeKind.GROUP);

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.setField("name");
        orderByClause.setDirection(OrderByClause.Direction.ASC);
        List<Group> matching = searchDAO.search(
                SyncopeConstants.FULL_ADMIN_REALMS,
                searchCond,
                page, size,
                List.of(orderByClause), AnyTypeKind.GROUP);
        List<GroupTO> result = matching.stream().
                map(group -> groupDataBinder.getGroupTO(group, false)).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("isAuthenticated()")
    public TypeExtensionTO readTypeExtension(final String groupName) {
        Group group = groupDAO.findByName(groupName);
        if (group == null) {
            throw new NotFoundException("Group " + groupName);
        }
        Optional<? extends TypeExtension> typeExt = group.getTypeExtension(anyTypeDAO.findUser());
        if (typeExt.isEmpty()) {
            throw new NotFoundException("TypeExtension in " + groupName + " for users");
        }

        return groupDataBinder.getTypeExtensionTO(typeExt.get());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.KEYMASTER + "')")
    @Transactional(readOnly = true)
    public void exportInternalStorageContent(final OutputStream os) {
        try {
            exporter.export(
                    AuthContextUtils.getDomain(),
                    os,
                    uwfAdapter.getPrefix(),
                    gwfAdapter.getPrefix(),
                    awfAdapter.getPrefix());
            LOG.debug("Internal storage content successfully exported");
        } catch (Exception e) {
            LOG.error("While exporting internal storage content", e);
        }
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
