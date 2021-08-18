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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class RealmLogic extends AbstractTransactionalLogic<RealmTO> {

    protected final RealmDAO realmDAO;

    protected final AnySearchDAO searchDAO;

    protected final RealmDataBinder binder;

    protected final PropagationManager propagationManager;

    protected final PropagationTaskExecutor taskExecutor;

    public RealmLogic(
            final RealmDAO realmDAO,
            final AnySearchDAO searchDAO,
            final RealmDataBinder binder,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor) {

        this.realmDAO = realmDAO;
        this.searchDAO = searchDAO;
        this.binder = binder;
        this.propagationManager = propagationManager;
        this.taskExecutor = taskExecutor;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_LIST + "')")
    @Transactional(readOnly = true)
    public Pair<Integer, List<RealmTO>> search(final String keyword, final String base) {
        Realm baseRealm = base == null ? realmDAO.getRoot() : realmDAO.findByFullPath(base);
        if (baseRealm == null) {
            LOG.error("Could not find realm '" + base + "'");

            throw new NotFoundException(base);
        }

        Set<String> roots = AuthContextUtils.getAuthorizations().get(IdRepoEntitlement.REALM_LIST).stream().
                filter(auth -> auth.startsWith(baseRealm.getFullPath())).collect(Collectors.toSet());

        Set<Realm> match = realmDAO.findMatching(keyword).stream().
                filter(realm -> roots.stream().anyMatch(root -> realm.getFullPath().startsWith(root))).
                collect(Collectors.toSet());

        int descendants = Math.toIntExact(
                match.stream().flatMap(realm -> realmDAO.findDescendants(realm).stream()).distinct().count());

        return Pair.of(
                descendants,
                match.stream().map(realm -> binder.getRealmTO(realm, true)).
                        sorted(Comparator.comparing(RealmTO::getFullPath)).
                        collect(Collectors.toList()));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<RealmTO> list(final String fullPath) {
        Realm realm = realmDAO.findByFullPath(fullPath);
        if (realm == null) {
            LOG.error("Could not find realm '" + fullPath + '\'');

            throw new NotFoundException(fullPath);
        }

        boolean admin = AuthContextUtils.getAuthorizations().keySet().contains(IdRepoEntitlement.REALM_LIST);
        return realmDAO.findDescendants(realm).stream().
                map(descendant -> binder.getRealmTO(descendant, admin)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_CREATE + "')")
    public ProvisioningResult<RealmTO> create(final String parentPath, final RealmTO realmTO) {
        Realm parent;
        if (StringUtils.isBlank(realmTO.getParent())) {
            parent = realmDAO.findByFullPath(parentPath);
            if (parent == null) {
                LOG.error("Could not find parent realm " + parentPath);

                throw new NotFoundException(parentPath);
            }

            realmTO.setParent(parent.getFullPath());
        } else {
            parent = realmDAO.find(realmTO.getParent());
            if (parent == null) {
                LOG.error("Could not find parent realm " + realmTO.getParent());

                throw new NotFoundException(realmTO.getParent());
            }

            if (!parent.getFullPath().equals(parentPath)) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPath);
                sce.getElements().add("Mismatching parent realm: " + parentPath + " Vs " + parent.getFullPath());
                throw sce;
            }
        }

        String fullPath = StringUtils.appendIfMissing(parent.getFullPath(), "/") + realmTO.getName();
        if (realmDAO.findByFullPath(fullPath) != null) {
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
        Realm realm = realmDAO.findByFullPath(realmTO.getFullPath());
        if (realm == null) {
            LOG.error("Could not find realm '" + realmTO.getFullPath() + '\'');

            throw new NotFoundException(realmTO.getFullPath());
        }
        PropagationByResource<String> propByRes = binder.update(realm, realmTO);
        realm = realmDAO.save(realm);

        List<PropagationTaskInfo> taskInfos = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter =
                taskExecutor.execute(taskInfos, false, AuthContextUtils.getUsername());

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        return result;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.REALM_DELETE + "')")
    public ProvisioningResult<RealmTO> delete(final String fullPath) {
        Realm realm = realmDAO.findByFullPath(fullPath);
        if (realm == null) {
            LOG.error("Could not find realm '" + fullPath + '\'');

            throw new NotFoundException(fullPath);
        }

        if (!realmDAO.findChildren(realm).isEmpty()) {
            throw SyncopeClientException.build(ClientExceptionType.HasChildren);
        }

        Set<String> adminRealms = Set.of(realm.getFullPath());
        AnyCond keyCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        keyCond.setSchema("key");
        SearchCond allMatchingCond = SearchCond.getLeaf(keyCond);
        int users = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.USER);
        int groups = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.GROUP);
        int anyObjects = searchDAO.count(adminRealms, allMatchingCond, AnyTypeKind.ANY_OBJECT);

        if (users + groups + anyObjects > 0) {
            SyncopeClientException containedAnys = SyncopeClientException.build(ClientExceptionType.AssociatedAnys);
            containedAnys.getElements().add(users + " user(s)");
            containedAnys.getElements().add(groups + " group(s)");
            containedAnys.getElements().add(anyObjects + " anyObject(s)");
            throw containedAnys;
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
                if (args[i] instanceof String) {
                    fullPath = (String) args[i];
                } else if (args[i] instanceof RealmTO) {
                    fullPath = ((RealmTO) args[i]).getFullPath();
                }
            }
        }

        if (fullPath != null) {
            try {
                return binder.getRealmTO(realmDAO.findByFullPath(fullPath), true);
            } catch (Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
