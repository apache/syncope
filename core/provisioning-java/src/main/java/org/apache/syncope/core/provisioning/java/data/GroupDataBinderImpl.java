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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyChecker;
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
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AnyDataBinder implements GroupDataBinder {

    protected final SearchCondVisitor searchCondVisitor;

    public GroupDataBinderImpl(
            final AnyTypeDAO anyTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final ExternalResourceDAO resourceDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final AnyChecker anyChecker,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final SearchCondVisitor searchCondVisitor,
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
                anyChecker,
                entityFactory,
                anyUtilsFactory,
                derAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator,
                jexlTools);

        this.searchCondVisitor = searchCondVisitor;
    }

    @Override
    public void create(final Group group, final GroupCR groupCR) {
        GroupTO anyTO = new GroupTO();
        EntityTOUtils.toAnyTO(groupCR, anyTO);

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (groupCR.getName() == null) {
            LOG.error("No name specified for this group");

            invalidGroups.getElements().add("No name specified for this group");
        } else {
            group.setName(groupCR.getName());
        }

        // realm
        Realm realm = realmSearchDAO.findByFullPath(groupCR.getRealm()).orElse(null);
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + groupCR.getRealm());
            scce.addException(noRealm);
        }
        group.setRealm(realm);

        // manager, attributes, resources and relationships
        fill(anyTO, group, groupCR, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        // type extensions
        groupCR.getTypeExtensions().forEach(typeExtTO -> anyTypeDAO.findById(typeExtTO.getAnyType()).ifPresentOrElse(
                anyType -> {
                    GroupTypeExtension typeExt = entityFactory.newEntity(GroupTypeExtension.class);
                    typeExt.setAnyType(anyType);
                    typeExt.setGroup(group);
                    group.add(typeExt);

                    typeExtTO.getAuxClasses().forEach(name -> anyTypeClassDAO.findById(name).ifPresentOrElse(
                    typeExt::add,
                    () -> LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), name)));

                    if (typeExt.getAuxClasses().isEmpty()) {
                        group.getTypeExtensions().remove(typeExt);
                        typeExt.setGroup(null);
                    }
                },
                () -> LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType())));

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource<String> update(final Group toBeUpdated, final GroupUR groupUR) {
        // Re-merge any pending change from workflow tasks
        Group group = groupDAO.save(toBeUpdated);

        GroupTO anyTO = AnyOperations.patch(getGroupTO(group, true), groupUR);

        PropagationByResource<String> propByRes = new PropagationByResource<>();

        // Save projection on Resources (before update)
        Map<String, ConnObject> beforeOnResources =
                onResources(group, groupDAO.findAllResourceKeys(group.getKey()), null, Set.of());

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // realm
        setRealm(group, groupUR);

        // name
        if (groupUR.getName() != null && StringUtils.isNotBlank(groupUR.getName().getValue())) {
            group.setName(groupUR.getName().getValue());
        }

        // manager, attributes, resources and relationships
        fill(anyTO, group, groupUR, propByRes, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        group = groupDAO.save(group);

        // type extensions
        for (TypeExtensionTO typeExtTO : groupUR.getTypeExtensions()) {
            AnyType anyType = anyTypeDAO.findById(typeExtTO.getAnyType()).orElse(null);
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType());
            } else {
                GroupTypeExtension typeExt = group.getTypeExtension(anyType).orElse(null);
                if (typeExt == null) {
                    typeExt = entityFactory.newEntity(GroupTypeExtension.class);
                    typeExt.setAnyType(anyType);
                    typeExt.setGroup(group);
                    group.add(typeExt);
                }

                // add all classes contained in the TO
                for (String key : typeExtTO.getAuxClasses()) {
                    AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(key).orElse(null);
                    if (anyTypeClass == null) {
                        LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), key);
                    } else {
                        typeExt.add(anyTypeClass);
                    }
                }
                // remove all classes not contained in the TO
                typeExt.getAuxClasses().
                        removeIf(anyTypeClass -> !typeExtTO.getAuxClasses().contains(anyTypeClass.getKey()));

                // only consider non-empty type extensions
                if (typeExt.getAuxClasses().isEmpty()) {
                    group.getTypeExtensions().remove(typeExt);
                    typeExt.setGroup(null);
                }
            }
        }
        // remove all type extensions not contained in the TO
        group.getTypeExtensions().
                removeIf(typeExt -> groupUR.getTypeExtension(typeExt.getAnyType().getKey()).isEmpty());

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        group = groupDAO.save(group);

        // Build final information for next stage (propagation)
        propByRes.merge(propByRes(
                beforeOnResources, onResources(group, groupDAO.findAllResourceKeys(group.getKey()), null, Set.of())));
        return propByRes;
    }

    @Override
    public TypeExtensionTO getTypeExtensionTO(final GroupTypeExtension typeExt) {
        TypeExtensionTO typeExtTO = new TypeExtensionTO();
        typeExtTO.setAnyType(typeExt.getAnyType().getKey());
        typeExtTO.getAuxClasses().addAll(typeExt.getAuxClasses().stream().map(AnyTypeClass::getKey).toList());
        return typeExtTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group, final boolean details) {
        GroupTO groupTO = new GroupTO();

        groupTO.setKey(group.getKey());
        groupTO.setName(group.getName());
        groupTO.setStatus(group.getStatus());

        groupTO.setCreator(group.getCreator());
        groupTO.setCreationDate(group.getCreationDate());
        groupTO.setCreationContext(group.getCreationContext());
        groupTO.setLastModifier(group.getLastModifier());
        groupTO.setLastChangeDate(group.getLastChangeDate());
        groupTO.setLastChangeContext(group.getLastChangeContext());

        fillTO(group, groupTO, derAttrHandler.getValues(group), group.getResources());

        // User and AnyType membership counts
        groupTO.setUserMembershipCount(groupDAO.countUMembers(group.getKey()));
        groupTO.setAnyObjectMembershipCount(groupDAO.countAMembers(group.getKey()));

        group.getTypeExtensions().forEach(typeExt -> groupTO.getTypeExtensions().add(getTypeExtensionTO(typeExt)));

        if (details) {
            // relationships
            groupTO.getRelationships().addAll(group.getRelationships().stream().
                    map(relationship -> getRelationshipTO(group.getPlainAttrs(relationship),
                    derAttrHandler.getValues(group, relationship),
                    relationship.getType().getKey(),
                    RelationshipTO.End.LEFT,
                    relationship.getRightEnd())).toList());
        }

        return groupTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final String key) {
        return getGroupTO(groupDAO.authFind(key), true);
    }

    protected static void populateTransitiveResources(
            final Group group,
            final Groupable<?, ?, ?> any,
            final Map<String, PropagationByResource<String>> result) {

        PropagationByResource<String> propByRes = new PropagationByResource<>();
        group.getResources().forEach(resource -> {
            // exclude from propagation those objects that have that resource assigned by some other membership(s)
            if (!any.getResources().contains(resource)
                    && any.getMemberships().stream().
                            filter(m -> !m.getRightEnd().equals(group)).
                            noneMatch(m -> m.getRightEnd().getResources().contains(resource))) {

                propByRes.add(ResourceOperation.DELETE, resource.getKey());
            }

            if (!propByRes.isEmpty()) {
                result.put(any.getKey(), propByRes);
            }
        });
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, PropagationByResource<String>> findAnyObjectsWithTransitiveResources(final String groupKey) {
        Group group = groupDAO.authFind(groupKey);

        Map<String, PropagationByResource<String>> result = new HashMap<>();

        groupDAO.findAMemberships(group).
                forEach((membership) -> populateTransitiveResources(group, membership.getLeftEnd(), result));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, PropagationByResource<String>> findUsersWithTransitiveResources(final String groupKey) {
        Group group = groupDAO.authFind(groupKey);

        Map<String, PropagationByResource<String>> result = new HashMap<>();

        groupDAO.findUMemberships(group, Pageable.unpaged()).
                forEach((membership) -> populateTransitiveResources(group, membership.getLeftEnd(), result));

        return result;
    }
}
