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
package org.apache.syncope.core.provisioning.java.data;

import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplate;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Policy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RealmDataBinderImpl implements RealmDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(RealmDataBinder.class);

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private TemplateUtils templateUtils;

    private void setTemplates(final RealmTO realmTO, final Realm realm) {
        // validate JEXL expressions from templates and proceed if fine
        templateUtils.check(realmTO.getTemplates(), ClientExceptionType.InvalidPullTask);
        realmTO.getTemplates().entrySet().forEach(entry -> {
            AnyType type = anyTypeDAO.find(entry.getKey());
            if (type == null) {
                LOG.debug("Invalid AnyType {} specified, ignoring...", entry.getKey());
            } else {
                AnyTemplateRealm anyTemplate = realm.getTemplate(type).orElse(null);
                if (anyTemplate == null) {
                    anyTemplate = entityFactory.newEntity(AnyTemplateRealm.class);
                    anyTemplate.setAnyType(type);
                    anyTemplate.setRealm(realm);

                    realm.add(anyTemplate);
                }
                anyTemplate.set(entry.getValue());
            }
        });
        // remove all templates not contained in the TO
        realm.getTemplates().removeAll(
                realm.getTemplates().stream().
                        filter(anyTemplate -> !realmTO.getTemplates().containsKey(anyTemplate.getAnyType().getKey())).
                        collect(Collectors.toList()));
    }

    @Override
    public Realm create(final String parentPath, final RealmTO realmTO) {
        Realm realm = entityFactory.newEntity(Realm.class);

        realm.setName(realmTO.getName());
        realm.setParent(realmDAO.findByFullPath(parentPath));

        if (realmTO.getPasswordPolicy() != null) {
            Policy policy = policyDAO.find(realmTO.getPasswordPolicy());
            if (policy instanceof PasswordPolicy) {
                realm.setPasswordPolicy((PasswordPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + PasswordPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }
        if (realmTO.getAccountPolicy() != null) {
            Policy policy = policyDAO.find(realmTO.getAccountPolicy());
            if (policy instanceof AccountPolicy) {
                realm.setAccountPolicy((AccountPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AccountPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        realm.getActionsClassNames().addAll(realmTO.getActionsClassNames());

        setTemplates(realmTO, realm);

        realmTO.getResources().forEach(resourceKey -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + "{}, ignoring...", resourceKey);
            } else {
                realm.add(resource);
            }
        });

        return realm;
    }

    @Override
    public PropagationByResource update(final Realm realm, final RealmTO realmTO) {
        realm.setName(realmTO.getName());
        realm.setParent(realmTO.getParent() == null ? null : realmDAO.find(realmTO.getParent()));

        if (realmTO.getAccountPolicy() == null) {
            realm.setAccountPolicy(null);
        } else {
            Policy policy = policyDAO.find(realmTO.getAccountPolicy());
            if (policy instanceof AccountPolicy) {
                realm.setAccountPolicy((AccountPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AccountPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (realmTO.getPasswordPolicy() == null) {
            realm.setPasswordPolicy(null);
        } else {
            Policy policy = policyDAO.find(realmTO.getPasswordPolicy());
            if (policy instanceof PasswordPolicy) {
                realm.setPasswordPolicy((PasswordPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + PasswordPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        realm.getActionsClassNames().clear();
        realm.getActionsClassNames().addAll(realmTO.getActionsClassNames());

        setTemplates(realmTO, realm);

        final PropagationByResource propByRes = new PropagationByResource();
        realmTO.getResources().forEach(resourceKey -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + "{}, ignoring...", resourceKey);
            } else {
                realm.add(resource);
                propByRes.add(ResourceOperation.CREATE, resource.getKey());
            }
        });
        // remove all resources not contained in the TO
        realm.getResources().removeAll(
                realm.getResources().stream().filter(resource -> {
                    boolean contained = realmTO.getResources().contains(resource.getKey());
                    if (!contained) {
                        propByRes.add(ResourceOperation.DELETE, resource.getKey());
                    }
                    return !contained;
                }).collect(Collectors.toList()));

        return propByRes;
    }

    @Override
    public RealmTO getRealmTO(final Realm realm, final boolean admin) {
        RealmTO realmTO = new RealmTO();

        realmTO.setKey(realm.getKey());
        realmTO.setName(realm.getName());
        realmTO.setParent(realm.getParent() == null ? null : realm.getParent().getKey());
        realmTO.setFullPath(realm.getFullPath());

        if (admin) {
            realmTO.setAccountPolicy(realm.getAccountPolicy() == null ? null : realm.getAccountPolicy().getKey());
            realmTO.setPasswordPolicy(realm.getPasswordPolicy() == null ? null : realm.getPasswordPolicy().getKey());
            realmTO.getActionsClassNames().addAll(realm.getActionsClassNames());

            for (AnyTemplate template : realm.getTemplates()) {
                realmTO.getTemplates().put(template.getAnyType().getKey(), template.get());
            }

            for (ExternalResource resource : realm.getResources()) {
                realmTO.getResources().add(resource.getKey());
            }
        }

        return realmTO;
    }

}
