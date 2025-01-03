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

import java.util.Optional;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        realmTO.getTemplates().forEach((key, template) -> anyTypeDAO.findById(key).ifPresentOrElse(
                type -> {
                    AnyTemplateRealm anyTemplate = realm.getTemplate(type).orElse(null);
                    if (anyTemplate == null) {
                        anyTemplate = entityFactory.newEntity(AnyTemplateRealm.class);
                        anyTemplate.setAnyType(type);
                        anyTemplate.setRealm(realm);

                        realm.add(anyTemplate);
                    }
                    anyTemplate.set(template);
                },
                () -> LOG.debug("Invalid AnyType {} specified, ignoring...", key)));
        // remove all templates not contained in the TO
        realm.getTemplates().
                removeIf(template -> !realmTO.getTemplates().containsKey(template.getAnyType().getKey()));
    }

    @Override
    public Realm create(final Realm parent, final RealmTO realmTO) {
        Realm realm = entityFactory.newEntity(Realm.class);

        realm.setName(realmTO.getName());
        realm.setParent(parent);

        realm.setPasswordPolicy(realmTO.getPasswordPolicy() == null
                ? null : policyDAO.findById(realmTO.getPasswordPolicy(), PasswordPolicy.class).orElse(null));

        realm.setAccountPolicy(realmTO.getAccountPolicy() == null
                ? null : policyDAO.findById(realmTO.getAccountPolicy(), AccountPolicy.class).orElse(null));

        realm.setAuthPolicy(realmTO.getAuthPolicy() == null
                ? null : policyDAO.findById(realmTO.getAuthPolicy(), AuthPolicy.class).orElse(null));

        realm.setAccessPolicy(realmTO.getAccessPolicy() == null
                ? null : policyDAO.findById(realmTO.getAccessPolicy(), AccessPolicy.class).orElse(null));

        realm.setAttrReleasePolicy(realmTO.getAttrReleasePolicy() == null
                ? null : policyDAO.findById(realmTO.getAttrReleasePolicy(), AttrReleasePolicy.class).orElse(null));

        realm.setTicketExpirationPolicy(realmTO.getTicketExpirationPolicy() == null
                ? null
                : policyDAO.findById(realmTO.getTicketExpirationPolicy(), TicketExpirationPolicy.class).orElse(null));

        realmTO.getActions().forEach(key -> implementationDAO.findById(key).ifPresentOrElse(
                realm::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), key)));

        setTemplates(realmTO, realm);

        realmTO.getResources().forEach(key -> resourceDAO.findById(key).ifPresentOrElse(
                realm::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", ExternalResource.class.getSimpleName(), key)));

        return realm;
    }

    @Override
    public PropagationByResource<String> update(final Realm realm, final RealmTO realmTO) {
        realm.setName(realmTO.getName());
        realm.setParent(realmTO.getParent() == null ? null : realmDAO.findById(realmTO.getParent()).orElse(null));

        realm.setPasswordPolicy(realmTO.getPasswordPolicy() == null
                ? null : policyDAO.findById(realmTO.getPasswordPolicy(), PasswordPolicy.class).orElse(null));

        realm.setAccountPolicy(realmTO.getAccountPolicy() == null
                ? null : policyDAO.findById(realmTO.getAccountPolicy(), AccountPolicy.class).orElse(null));

        realm.setAuthPolicy(realmTO.getAuthPolicy() == null
                ? null : policyDAO.findById(realmTO.getAuthPolicy(), AuthPolicy.class).orElse(null));

        realm.setAccessPolicy(realmTO.getAccessPolicy() == null
                ? null : policyDAO.findById(realmTO.getAccessPolicy(), AccessPolicy.class).orElse(null));

        realm.setAttrReleasePolicy(realmTO.getAttrReleasePolicy() == null
                ? null : policyDAO.findById(realmTO.getAttrReleasePolicy(), AttrReleasePolicy.class).orElse(null));

        realm.setTicketExpirationPolicy(realmTO.getTicketExpirationPolicy() == null
                ? null
                : policyDAO.findById(realmTO.getTicketExpirationPolicy(), TicketExpirationPolicy.class).orElse(null));

        realmTO.getActions().forEach(key -> implementationDAO.findById(key).ifPresentOrElse(
                realm::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), key)));
        // remove all implementations not contained in the TO
        realm.getActions().
                removeIf(implementation -> !realmTO.getActions().contains(implementation.getKey()));

        setTemplates(realmTO, realm);

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        realmTO.getResources().forEach(key -> resourceDAO.findById(key).ifPresentOrElse(
                resource -> {
                    realm.add(resource);
                    propByRes.add(ResourceOperation.CREATE, resource.getKey());
                },
                () -> LOG.debug("Invalid {} {}, ignoring...", ExternalResource.class.getSimpleName(), key)));
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
        Optional.ofNullable(realm.getParent()).ifPresent(parent -> realmTO.setParent(parent.getKey()));
        realmTO.setFullPath(realm.getFullPath());

        if (admin) {
            Optional.ofNullable(realm.getAccountPolicy()).
                    ifPresent(policy -> realmTO.setAccountPolicy(policy.getKey()));
            Optional.ofNullable(realm.getPasswordPolicy()).
                    ifPresent(policy -> realmTO.setPasswordPolicy(policy.getKey()));
            Optional.ofNullable(realm.getAuthPolicy()).
                    ifPresent(policy -> realmTO.setAuthPolicy(policy.getKey()));
            Optional.ofNullable(realm.getAccessPolicy()).
                    ifPresent(policy -> realmTO.setAccessPolicy(policy.getKey()));
            Optional.ofNullable(realm.getAttrReleasePolicy()).
                    ifPresent(policy -> realmTO.setAttrReleasePolicy(policy.getKey()));
            Optional.ofNullable(realm.getTicketExpirationPolicy()).
                    ifPresent(policy -> realmTO.setTicketExpirationPolicy(policy.getKey()));

            realm.getActions().forEach(action -> realmTO.getActions().add(action.getKey()));

            realm.getTemplates().forEach(
                    template -> realmTO.getTemplates().put(template.getAnyType().getKey(), template.get()));

            realm.getResources().forEach(resource -> realmTO.getResources().add(resource.getKey()));
        }

        return realmTO;
    }
}
