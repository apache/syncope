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

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class RealmLogic extends AbstractTransactionalLogic<RealmTO> {

    protected final RealmDAO realmDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final AnySearchDAO searchDAO;

    protected final TaskDAO taskDAO;

    protected final CASSPClientAppDAO casSPClientAppDAO;

    protected final OIDCRPClientAppDAO oidcRPClientAppDAO;

    protected final SAML2SPClientAppDAO saml2SPClientAppDAO;

    protected final RealmDataBinder binder;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    public RealmLogic(
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnySearchDAO searchDAO,
            final TaskDAO taskDAO,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO,
            final RealmDataBinder binder,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor) {

        this.realmDAO = realmDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.searchDAO = searchDAO;
        this.taskDAO = taskDAO;
        this.casSPClientAppDAO = casSPClientAppDAO;
        this.oidcRPClientAppDAO = oidcRPClientAppDAO;
        this.saml2SPClientAppDAO = saml2SPClientAppDAO;
        this.binder = binder;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
    }

    protected void securityChecks(final Set<String> effectiveRealms, final String realm) {
        boolean authorized = effectiveRealms.stream().anyMatch(realm::startsWith);
        if (!authorized) {
            throw new DelegatedAdministrationException(
                    realm, User.class.getSimpleName(), AuthContextUtils.getUsername());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public Page<RealmTO> search(
            final String keyword,
            final Set<String> bases,
            final Pageable pageable) {

        Set<String> baseRealms = new HashSet<>();
        if (CollectionUtils.isEmpty(bases)) {
            baseRealms.add(SyncopeConstants.ROOT_REALM);
        } else {
            for (String base : bases) {
                baseRealms.add(realmSearchDAO.findByFullPath(base).map(Realm::getFullPath).
                        orElseThrow(() -> new NotFoundException("Realm " + base)));
            }
        }

        long count = realmSearchDAO.countDescendants(baseRealms, keyword);

        Set<String> authorizations = AuthContextUtils.getAuthorizations().
                getOrDefault(IdRepoEntitlement.REALM_SEARCH, Set.of());
        List<RealmTO> result = realmSearchDAO.findDescendants(baseRealms, keyword, pageable).stream().
                map(realm -> binder.getRealmTO(
                realm, authorizations.stream().
                        anyMatch(auth -> realm.getFullPath().startsWith(auth)))).
                sorted(Comparator.comparing(RealmTO::getFullPath)).
                toList();

        return new SyncopePage<>(result, pageable, count);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_CREATE + "')")
    public ProvisioningResult<RealmTO> create(final String parentPath, final RealmTO realmTO) {
        Realm parent;
        if (StringUtils.isBlank(realmTO.getParent())) {
            parent = realmSearchDAO.findByFullPath(parentPath).
                    orElseThrow(() -> new NotFoundException("Realm " + parentPath));

            realmTO.setParent(parent.getFullPath());
        } else {
            parent = realmDAO.findById(realmTO.getParent()).
                    orElseThrow(() -> new NotFoundException("Realm " + realmTO.getParent()));

            if (!parent.getFullPath().equals(parentPath)) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPath);
                sce.getElements().add("Mismatching parent realm: " + parentPath + " Vs " + parent.getFullPath());
                throw sce;
            }
        }

        securityChecks(AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.REALM_CREATE), parent.getFullPath());

        String fullPath = StringUtils.appendIfMissing(parent.getFullPath(), "/") + realmTO.getName();
        if (realmSearchDAO.findByFullPath(fullPath).isPresent()) {
            throw new DuplicateException(fullPath);
        }

        Realm realm = realmDAO.save(binder.create(parent, realmTO));
        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.CREATE, realm.getResourceKeys());
        List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter =
                taskExecutor.execute(taskInfos, false, AuthContextUtils.getUsername());

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_UPDATE + "')")
    public ProvisioningResult<RealmTO> update(final RealmTO realmTO) {
        Realm realm = realmSearchDAO.findByFullPath(realmTO.getFullPath()).
                orElseThrow(() -> new NotFoundException("Realm " + realmTO.getFullPath()));

        securityChecks(AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.REALM_UPDATE), realm.getFullPath());

        Map<Pair<String, String>, Set<Attribute>> beforeAttrs = propagationManager.prepareAttrs(realm);

        PropagationByResource<String> propByRes = binder.update(realm, realmTO);
        realm = realmDAO.save(realm);

        List<PropagationTaskInfo> taskInfos = propagationManager.setAttributeDeltas(
                propagationManager.createTasks(realm, propByRes, null),
                beforeAttrs);
        PropagationReporter propagationReporter =
                taskExecutor.execute(taskInfos, false, AuthContextUtils.getUsername());

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_DELETE + "')")
    public ProvisioningResult<RealmTO> delete(final String fullPath) {
        Realm realm = realmSearchDAO.findByFullPath(fullPath).
                orElseThrow(() -> new NotFoundException("Realm " + fullPath));

        securityChecks(AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.REALM_DELETE), realm.getFullPath());

        if (!realmSearchDAO.findChildren(realm).isEmpty()) {
            throw SyncopeClientException.build(ClientExceptionType.RealmContains);
        }

        Set<String> adminRealms = Set.of(realm.getFullPath());
        AnyCond keyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        keyCond.setSchema("key");
        SearchCond allMatchingCond = SearchCond.of(keyCond);
        long users = searchDAO.count(realm, true, adminRealms, allMatchingCond, AnyTypeKind.USER);
        long groups = searchDAO.count(realm, true, adminRealms, allMatchingCond, AnyTypeKind.GROUP);
        long anyObjects = searchDAO.count(realm, true, adminRealms, allMatchingCond, AnyTypeKind.ANY_OBJECT);
        long macroTasks = taskDAO.findByRealm(realm).size();
        long clientApps = casSPClientAppDAO.findAllByRealm(realm).size()
                + saml2SPClientAppDAO.findAllByRealm(realm).size()
                + oidcRPClientAppDAO.findAllByRealm(realm).size();

        if (users + groups + anyObjects + macroTasks + clientApps > 0) {
            SyncopeClientException realmContains = SyncopeClientException.build(ClientExceptionType.RealmContains);
            realmContains.getElements().add(users + " user(s)");
            realmContains.getElements().add(groups + " group(s)");
            realmContains.getElements().add(anyObjects + " anyObject(s)");
            realmContains.getElements().add(macroTasks + " command task(s)");
            realmContains.getElements().add(clientApps + " client app(s)");
            throw realmContains;
        }

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        propByRes.addAll(ResourceOperation.DELETE, realm.getResourceKeys());
        List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter =
                taskExecutor.execute(taskInfos, false, AuthContextUtils.getUsername());

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        realmDAO.delete(realm);

        return result;
    }

    @Override
    protected RealmTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String fullPath = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; fullPath == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    fullPath = string;
                } else if (args[i] instanceof RealmTO realmTO) {
                    fullPath = realmTO.getFullPath();
                }
            }
        }

        if (fullPath != null) {
            try {
                return binder.getRealmTO(realmSearchDAO.findByFullPath(fullPath).orElseThrow(), true);
            } catch (Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
