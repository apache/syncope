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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;

public class RealmDataBinderImpl extends AttributableDataBinder implements RealmDataBinder {

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final ImplementationDAO implementationDAO;

    protected final RealmDAO realmDAO;

    protected final PolicyDAO policyDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final EntityFactory entityFactory;

    protected final TemplateUtils templateUtils;

    public RealmDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ImplementationDAO implementationDAO,
            final RealmDAO realmDAO,
            final PolicyDAO policyDAO,
            final ExternalResourceDAO resourceDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final DerAttrHandler derAttrHandler,
            final PlainAttrValidationManager validator,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final JexlTools jexlTools,
            final TemplateUtils templateUtils) {

        super(plainSchemaDAO, validator, derAttrHandler, mappingManager, intAttrNameParser, jexlTools);
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.implementationDAO = implementationDAO;
        this.realmDAO = realmDAO;
        this.policyDAO = policyDAO;
        this.resourceDAO = resourceDAO;
        this.entityFactory = entityFactory;
        this.templateUtils = templateUtils;
    }

    protected void fill(
            final RealmTO realmTO,
            final Realm realm,
            final SyncopeClientCompositeException scce) {

        realm.getPlainAttrs().forEach(realm::remove);

        SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

        realmTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getValues().isEmpty()).
                forEach(attrTO -> getPlainSchema(attrTO.getSchema()).ifPresent(schema -> {

            PlainAttr attr = realm.getPlainAttr(schema.getKey()).orElseGet(() -> {
                PlainAttr newAttr = new PlainAttr();
                newAttr.setPlainSchema(schema);
                return newAttr;
            });
            fillAttr(realmTO, attrTO.getValues(), schema, attr, invalidValues);

            if (!attr.getValuesAsStrings().isEmpty()) {
                realm.add(attr);
            }
        }));

        if (!invalidValues.isEmpty()) {
            scce.addException(invalidValues);
        }

        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
        // Check if there is some mandatory schema defined for which no value has been provided
        realm.getAnyTypeClass().getPlainSchemas().forEach(schema -> checkMandatory(
                schema, realm.getPlainAttr(schema.getKey()).orElse(null), realm, reqValMissing));
        if (!reqValMissing.isEmpty()) {
            scce.addException(reqValMissing);
        }
    }

    protected void bind(final Realm realm, final RealmTO realmTO, final SyncopeClientCompositeException scce) {
        realm.setName(realmTO.getName());

        if (realmTO.getAnyTypeClass() == null) {
            realm.setAnyTypeClass(null);
        } else {
            anyTypeClassDAO.findById(realmTO.getAnyTypeClass()).ifPresentOrElse(
                    realm::setAnyTypeClass,
                    () -> LOG.debug("Invalid {} {}, ignoring...",
                            AnyTypeClass.class.getSimpleName(), realmTO.getAnyTypeClass()));
        }
        if (realm.getAnyTypeClass() != null) {
            fill(realmTO, realm, scce);
        }

        realm.setAccessPolicy(Optional.ofNullable(realmTO.getAccessPolicy()).
                flatMap(p -> policyDAO.findById(p, AccessPolicy.class)).orElse(null));
        realm.setAccountPolicy(Optional.ofNullable(realmTO.getAccountPolicy()).
                flatMap(p -> policyDAO.findById(p, AccountPolicy.class)).orElse(null));
        realm.setAttrReleasePolicy(Optional.ofNullable(realmTO.getAttrReleasePolicy()).
                flatMap(p -> policyDAO.findById(p, AttrReleasePolicy.class)).orElse(null));
        realm.setAuthPolicy(Optional.ofNullable(realmTO.getAuthPolicy()).
                flatMap(p -> policyDAO.findById(p, AuthPolicy.class)).orElse(null));
        realm.setPasswordPolicy(Optional.ofNullable(realmTO.getPasswordPolicy()).
                flatMap(p -> policyDAO.findById(p, PasswordPolicy.class)).orElse(null));
        realm.setTicketExpirationPolicy(Optional.ofNullable(realmTO.getTicketExpirationPolicy()).
                flatMap(p -> policyDAO.findById(p, TicketExpirationPolicy.class)).orElse(null));

        realmTO.getActions().forEach(key -> implementationDAO.findById(key).ifPresentOrElse(
                realm::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", Implementation.class.getSimpleName(), key)));
        // remove all implementations not contained in the TO
        realm.getActions().
                removeIf(implementation -> !realmTO.getActions().contains(implementation.getKey()));

        // validate JEXL expressions from templates and proceed if fine
        templateUtils.check(realmTO.getTemplates(), ClientExceptionType.InvalidRealm);
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

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected List<String> evaluateMandatoryCondition(final ExternalResource resource, final Realm realm) {
        if (resource.getOrgUnit() == null) {
            return List.of();
        }

        List<String> missingAttrNames = new ArrayList<>();

        MappingUtils.getPropagationItems(resource.getOrgUnit().getItems().stream()).forEach(item -> {
            IntAttrName intAttrName = null;
            try {
                intAttrName = intAttrNameParser.parse(item.getIntAttrName());
            } catch (ParseException e) {
                LOG.error("Invalid intAttrName '{}', ignoring", item.getIntAttrName(), e);
            }
            if (intAttrName != null && intAttrName.getSchema() != null) {
                AttrSchemaType schemaType = intAttrName.getSchema() instanceof PlainSchema
                        ? intAttrName.getSchema().getType()
                        : AttrSchemaType.String;

                MappingManager.IntValues intValues = mappingManager.getIntValues(
                        resource,
                        item,
                        intAttrName,
                        schemaType,
                        realm);
                if (intValues.values().isEmpty()
                        && jexlTools.evaluateMandatoryCondition(item.getMandatoryCondition(), realm, derAttrHandler)) {

                    missingAttrNames.add(item.getIntAttrName());
                }
            }
        });

        return missingAttrNames;
    }

    protected SyncopeClientException checkMandatoryOnResources(final Realm realm) {
        SyncopeClientException reqValMissing = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        realm.getResources().stream().filter(ExternalResource::isEnforceMandatoryCondition).
                forEach(resource -> {
                    List<String> missingAttrNames = evaluateMandatoryCondition(resource, realm);
                    if (!missingAttrNames.isEmpty()) {
                        LOG.error("Mandatory schemas {} not provided with values", missingAttrNames);

                        reqValMissing.getElements().addAll(missingAttrNames);
                    }
                });

        return reqValMissing;
    }

    @Override
    public Realm create(final Realm parent, final RealmTO realmTO) {
        Realm realm = entityFactory.newEntity(Realm.class);
        realm.setParent(parent);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        bind(realm, realmTO, scce);

        realmTO.getResources().forEach(key -> resourceDAO.findById(key).ifPresentOrElse(
                realm::add,
                () -> LOG.debug("Invalid {} {}, ignoring...", ExternalResource.class.getSimpleName(), key)));

        SyncopeClientException requiredValuesMissing = checkMandatoryOnResources(realm);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        return realm;
    }

    @Override
    public PropagationByResource<String> update(final Realm realm, final RealmTO realmTO) {
        realm.setParent(Optional.ofNullable(realmTO.getParent()).flatMap(realmDAO::findById).orElse(null));

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        bind(realm, realmTO, scce);

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

        SyncopeClientException requiredValuesMissing = checkMandatoryOnResources(realm);
        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        return propByRes;
    }

    @Override
    public RealmTO getRealmTO(final Realm realm, final boolean admin) {
        RealmTO realmTO = new RealmTO();

        realmTO.setKey(realm.getKey());
        realmTO.setName(realm.getName());
        Optional.ofNullable(realm.getParent()).ifPresent(parent -> realmTO.setParent(parent.getKey()));
        realmTO.setFullPath(realm.getFullPath());
        Optional.ofNullable(realm.getAnyTypeClass()).map(AnyTypeClass::getKey).ifPresent(realmTO::setAnyTypeClass);

        realm.getPlainAttrs().forEach(plainAttr -> realmTO.getPlainAttrs().
                add(new Attr.Builder(plainAttr.getSchema()).values(plainAttr.getValuesAsStrings()).build()));

        derAttrHandler.getValues(realm).forEach((schema, value) -> realmTO.getDerAttrs().
                add(new Attr.Builder(schema.getKey()).value(value).build()));

        if (admin) {
            Optional.ofNullable(realm.getAccountPolicy()).map(AccountPolicy::getKey).
                    ifPresent(realmTO::setAccountPolicy);
            Optional.ofNullable(realm.getPasswordPolicy()).map(PasswordPolicy::getKey).
                    ifPresent(realmTO::setPasswordPolicy);
            Optional.ofNullable(realm.getAuthPolicy()).map(AuthPolicy::getKey).
                    ifPresent(realmTO::setAuthPolicy);
            Optional.ofNullable(realm.getAccessPolicy()).map(AccessPolicy::getKey).
                    ifPresent(realmTO::setAccessPolicy);
            Optional.ofNullable(realm.getAttrReleasePolicy()).map(AttrReleasePolicy::getKey).
                    ifPresent(realmTO::setAttrReleasePolicy);
            Optional.ofNullable(realm.getTicketExpirationPolicy()).map(TicketExpirationPolicy::getKey).
                    ifPresent(realmTO::setTicketExpirationPolicy);

            realmTO.getActions().addAll(realm.getActions().stream().map(Implementation::getKey).toList());

            realm.getTemplates().forEach(
                    template -> realmTO.getTemplates().put(template.getAnyType().getKey(), template.get()));

            realmTO.getResources().addAll(realm.getResources().stream().map(ExternalResource::getKey).toList());
        }

        return realmTO;
    }
}
