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

import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class AnyObjectDataBinderImpl extends AnyDataBinder implements AnyObjectDataBinder {

    public AnyObjectDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final PlainAttrValidationManager validator,
            final JexlTools jexlTools) {

        super(anyTypeDAO,
                realmSearchDAO,
                anyTypeClassDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                plainSchemaDAO,
                resourceDAO,
                relationshipTypeDAO,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator,
                jexlTools);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final String key) {
        return getAnyObjectTO(anyObjectDAO.authFind(key), true);
    }

    @Transactional(readOnly = true)
    @Override
    public AnyObjectTO getAnyObjectTO(final AnyObject anyObject, final boolean details) {
        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setType(anyObject.getType().getKey());

        anyObjectTO.setKey(anyObject.getKey());
        anyObjectTO.setName(anyObject.getName());
        anyObjectTO.setStatus(anyObject.getStatus());

        anyObjectTO.setCreator(anyObject.getCreator());
        anyObjectTO.setCreationDate(anyObject.getCreationDate());
        anyObjectTO.setCreationContext(anyObject.getCreationContext());
        anyObjectTO.setLastModifier(anyObject.getLastModifier());
        anyObjectTO.setLastChangeDate(anyObject.getLastChangeDate());
        anyObjectTO.setLastChangeContext(anyObject.getLastChangeContext());

        fillTO(anyObject, anyObjectTO, derAttrHandler.getValues(anyObject), anyObjectDAO.findAllResources(anyObject));

        if (details) {
            // relationships
            anyObjectTO.getRelationships().addAll(
                    anyObjectDAO.findAllRelationships(anyObject).stream().
                            map(relationship -> getRelationshipTO(anyObject.getPlainAttrs(relationship),
                            derAttrHandler.getValues(anyObject, relationship),
                            relationship.getType().getKey(),
                            relationship.getLeftEnd().getKey().equals(anyObject.getKey())
                            ? RelationshipTO.End.LEFT
                            : RelationshipTO.End.RIGHT,
                            relationship.getLeftEnd().getKey().equals(anyObject.getKey())
                            ? relationship.getRightEnd()
                            : relationship.getLeftEnd())).
                            toList());

            // memberships
            anyObjectTO.getMemberships().addAll(
                    anyObject.getMemberships().stream().map(membership -> getMembershipTO(
                    anyObject.getPlainAttrs(membership),
                    derAttrHandler.getValues((Groupable<?, ?, ?>) anyObject, membership),
                    membership)).toList());
        }

        return anyObjectTO;
    }

    @Override
    public void create(final AnyObject anyObject, final AnyObjectCR anyObjectCR) {
        AnyType type = anyTypeDAO.findById(anyObjectCR.getType()).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
                    sce.getElements().add(anyObjectCR.getType());
                    return sce;
                });
        anyObject.setType(type);

        AnyObjectTO anyTO = new AnyObjectTO();
        EntityTOUtils.toAnyTO(anyObjectCR, anyTO);

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (anyObjectCR.getName() == null) {
            LOG.error("No name specified for this anyObject");

            invalidGroups.getElements().add("No name specified for this anyObject");
        } else {
            anyObject.setName(anyObjectCR.getName());
        }

        // realm
        Realm realm = realmSearchDAO.findByFullPath(anyObjectCR.getRealm()).orElse(null);
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + anyObjectCR.getRealm());
            scce.addException(noRealm);
        }
        anyObject.setRealm(realm);

        // attributes, resources and relationships
        fill(anyTO, anyObject, anyObjectCR, anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT), scce);

        // memberships
        memberships(anyObjectCR.getMemberships(), anyTO, anyObject, anyUtils, scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource<String> update(final AnyObject toBeUpdated, final AnyObjectUR anyObjectUR) {
        // Re-merge any pending change from workflow tasks
        AnyObject anyObject = anyObjectDAO.save(toBeUpdated);

        AnyObjectTO anyTO = AnyOperations.patch(getAnyObjectTO(anyObject, true), anyObjectUR);

        PropagationByResource<String> propByRes = new PropagationByResource<>();

        // Save projection on Resources (before update)
        Map<String, ConnObject> beforeOnResources =
                onResources(anyObject, anyObjectDAO.findAllResourceKeys(anyObject.getKey()), null, Set.of());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        AnyUtils anyUtils = anyUtilsFactory.getInstance(AnyTypeKind.ANY_OBJECT);

        // realm
        setRealm(anyObject, anyObjectUR);

        // name
        if (anyObjectUR.getName() != null && StringUtils.isNotBlank(anyObjectUR.getName().getValue())) {
            anyObject.setName(anyObjectUR.getName().getValue());
        }

        // attributes, resources and relationships
        fill(anyTO, anyObject, anyObjectUR, propByRes, anyUtils, scce);

        // memberships
        memberships(
                anyObjectUR.getMemberships(),
                anyTO,
                anyObject,
                group -> {
                },
                propByRes,
                anyUtils,
                scce);

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        AnyObject saved = anyObjectDAO.save(anyObject);

        // Build final information for next stage (propagation)
        propByRes.merge(propByRes(
                beforeOnResources,
                onResources(saved, anyObjectDAO.findAllResourceKeys(anyObject.getKey()), null, Set.of())));
        return propByRes;
    }
}
