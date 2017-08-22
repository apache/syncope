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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationReporter;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class RealmLogic extends AbstractTransactionalLogic<RealmTO> {

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private AnySearchDAO searchDAO;

    @Autowired
    private RealmDataBinder binder;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @PreAuthorize("isAuthenticated()")
    public List<RealmTO> list(final String fullPath) {
        Realm realm = realmDAO.findByFullPath(fullPath);
        if (realm == null) {
            LOG.error("Could not find realm '" + fullPath + "'");

            throw new NotFoundException(fullPath);
        }

        final boolean admin = AuthContextUtils.getAuthorizations().keySet().contains(StandardEntitlement.REALM_LIST);
        return realmDAO.findDescendants(realm).stream().
                map(descendant -> binder.getRealmTO(descendant, admin)).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REALM_CREATE + "')")
    public ProvisioningResult<RealmTO> create(final String parentPath, final RealmTO realmTO) {
        String fullPath = StringUtils.appendIfMissing(parentPath, "/") + realmTO.getName();
        if (realmDAO.findByFullPath(fullPath) != null) {
            throw new DuplicateException(fullPath);
        }

        Realm realm = realmDAO.save(binder.create(parentPath, realmTO));

        PropagationByResource propByRes = new PropagationByResource();
        realm.getResourceKeys().forEach(resource -> {
            propByRes.add(ResourceOperation.CREATE, resource);
        });
        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, false);

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REALM_UPDATE + "')")
    public ProvisioningResult<RealmTO> update(final RealmTO realmTO) {
        Realm realm = realmDAO.findByFullPath(realmTO.getFullPath());
        if (realm == null) {
            LOG.error("Could not find realm '" + realmTO.getFullPath() + "'");

            throw new NotFoundException(realmTO.getFullPath());
        }

        PropagationByResource propByRes = binder.update(realm, realmTO);
        realm = realmDAO.save(realm);

        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, false);

        ProvisioningResult<RealmTO> result = new ProvisioningResult<>();
        result.setEntity(binder.getRealmTO(realm, true));
        result.getPropagationStatuses().addAll(propagationReporter.getStatuses());

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.REALM_DELETE + "')")
    public ProvisioningResult<RealmTO> delete(final String fullPath) {
        Realm realm = realmDAO.findByFullPath(fullPath);
        if (realm == null) {
            LOG.error("Could not find realm '" + fullPath + "'");

            throw new NotFoundException(fullPath);
        }

        if (!realmDAO.findChildren(realm).isEmpty()) {
            throw SyncopeClientException.build(ClientExceptionType.HasChildren);
        }

        Set<String> adminRealms = Collections.singleton(realm.getFullPath());
        AnyCond keyCond = new AnyCond(AttributeCond.Type.ISNOTNULL);
        keyCond.setSchema("key");
        SearchCond allMatchingCond = SearchCond.getLeafCond(keyCond);
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

        PropagationByResource propByRes = new PropagationByResource();
        realm.getResourceKeys().forEach(resource -> {
            propByRes.add(ResourceOperation.DELETE, resource);
        });
        List<PropagationTask> tasks = propagationManager.createTasks(realm, propByRes, null);
        PropagationReporter propagationReporter = taskExecutor.execute(tasks, false);

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
