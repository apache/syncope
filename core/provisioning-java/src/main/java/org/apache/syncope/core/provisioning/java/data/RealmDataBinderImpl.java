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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.Policy;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;

public class RealmDataBinderImpl implements RealmDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmDataBinder.class);

    protected final AnyTypeDAO anyTypeDAO;

    protected final ImplementationDAO implementationDAO;

    protected final RealmDAO realmDAO;

    protected final PolicyDAO policyDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    public RealmDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final ImplementationDAO implementationDAO,
            final RealmDAO realmDAO,
            final PolicyDAO policyDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory) {

        this.anyTypeDAO = anyTypeDAO;
        this.implementationDAO = implementationDAO;
        this.realmDAO = realmDAO;
        this.policyDAO = policyDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
    }

    protected void setTemplates(final RealmTO realmTO, final Realm realm) {
        // validate JEXL expressions from templates and proceed if fine
        TemplateUtils.check(realmTO.getTemplates(), ClientExceptionType.InvalidRealm);
        realmTO.getTemplates().forEach((key, template) -> {
            AnyType type = anyTypeDAO.find(key);
            if (type == null) {
                LOG.debug("Invalid AnyType {} specified, ignoring...", key);
            } else {
                AnyTemplateRealm anyTemplate = realm.getTemplate(type).orElse(null);
                if (anyTemplate == null) {
                    anyTemplate = entityFactory.newEntity(AnyTemplateRealm.class);
                    anyTemplate.setAnyType(type);
                    anyTemplate.setRealm(realm);

                    realm.add(anyTemplate);
                }
                anyTemplate.set(template);
            }
        });
        // remove all templates not contained in the TO
        realm.getTemplates().
                removeIf(template -> !realmTO.getTemplates().containsKey(template.getAnyType().getKey()));
    }

    @Override
    public Realm create(final Realm parent, final RealmTO realmTO) {
        Realm realm = entityFactory.newEntity(Realm.class);

        realm.setName(realmTO.getName());
        realm.setParent(parent);

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
        if (realmTO.getAuthPolicy() != null) {
            Policy policy = policyDAO.find(realmTO.getAuthPolicy());
            if (policy instanceof AuthPolicy) {
                realm.setAuthPolicy((AuthPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AuthPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }
        if (realmTO.getAccessPolicy() != null) {
            Policy policy = policyDAO.find(realmTO.getAccessPolicy());
            if (policy instanceof AccessPolicy) {
                realm.setAccessPolicy((AccessPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AccessPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }
        if (realmTO.getAttrReleasePolicy() != null) {
            Policy policy = policyDAO.find(realmTO.getAttrReleasePolicy());
            if (policy instanceof AttrReleasePolicy) {
                realm.setAttrReleasePolicy((AttrReleasePolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AttrReleasePolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        realmTO.getActions().forEach(logicActionsKey -> {
            Implementation logicAction = implementationDAO.find(logicActionsKey);
            if (logicAction == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", logicActionsKey);
            } else {
                realm.add(logicAction);
            }
        });

        setTemplates(realmTO, realm);

        realmTO.getResources().forEach(resourceKey -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + " {}, ignoring...", resourceKey);
            } else {
                realm.add(resource);
            }
        });

        return realm;
    }

    @Override
    public PropagationByResource<String> update(final Realm realm, final RealmTO realmTO) {
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

        if (realmTO.getAuthPolicy() == null) {
            realm.setAuthPolicy(null);
        } else {
            Policy policy = policyDAO.find(realmTO.getAuthPolicy());
            if (policy instanceof AuthPolicy) {
                realm.setAuthPolicy((AuthPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AuthPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (realmTO.getAccessPolicy() == null) {
            realm.setAccessPolicy(null);
        } else {
            Policy policy = policyDAO.find(realmTO.getAccessPolicy());
            if (policy instanceof AccessPolicy) {
                realm.setAccessPolicy((AccessPolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AccessPolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        if (realmTO.getAttrReleasePolicy() == null) {
            realm.setAttrReleasePolicy(null);
        } else {
            Policy policy = policyDAO.find(realmTO.getAttrReleasePolicy());
            if (policy instanceof AttrReleasePolicy) {
                realm.setAttrReleasePolicy((AttrReleasePolicy) policy);
            } else {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPolicy);
                sce.getElements().add("Expected " + AttrReleasePolicy.class.getSimpleName()
                        + ", found " + policy.getClass().getSimpleName());
                throw sce;
            }
        }

        realmTO.getActions().forEach(logicActionsKey -> {
            Implementation logicActions = implementationDAO.find(logicActionsKey);
            if (logicActions == null) {
                LOG.debug("Invalid " + Implementation.class.getSimpleName() + " {}, ignoring...", logicActionsKey);
            } else {
                realm.add(logicActions);
            }
        });
        // remove all implementations not contained in the TO
        realm.getActions().
                removeIf(implementation -> !realmTO.getActions().contains(implementation.getKey()));

        setTemplates(realmTO, realm);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        realmTO.getResources().forEach(resourceKey -> {
            ExternalResource resource = resourceDAO.find(resourceKey);
            if (resource == null) {
                LOG.debug("Invalid " + ExternalResource.class.getSimpleName() + " {}, ignoring...", resourceKey);
            } else {
                realm.add(resource);
                propByRes.add(ResourceOperation.CREATE, resource.getKey());
            }
        });
        // remove all resources not contained in the TO
        realm.getResources().removeIf(resource -> {
            boolean contained = realmTO.getResources().contains(resource.getKey());
            if (!contained) {
                propByRes.add(ResourceOperation.DELETE, resource.getKey());
            }
            return !contained;
        });

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
            realmTO.setAuthPolicy(
                    realm.getAuthPolicy() == null ? null : realm.getAuthPolicy().getKey());
            realmTO.setAccessPolicy(realm.getAccessPolicy() == null ? null : realm.getAccessPolicy().getKey());
            realmTO.setAttrReleasePolicy(
                    realm.getAttrReleasePolicy() == null ? null : realm.getAttrReleasePolicy().getKey());

            realm.getActions().forEach(action -> realmTO.getActions().add(action.getKey()));

            realm.getTemplates().forEach(
                    template -> realmTO.getTemplates().put(template.getAnyType().getKey(), template.get()));

            realm.getResources().forEach(resource -> realmTO.getResources().add(resource.getKey()));
        }

        return realmTO;
    }
}
