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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.RelationshipUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.ResourceOperation;
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
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAnyDataBinder implements GroupDataBinder {

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
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final DerAttrHandler derAttrHandler,
            final VirAttrHandler virAttrHandler,
            final MappingManager mappingManager,
            final IntAttrNameParser intAttrNameParser,
            final OutboundMatcher outboundMatcher,
            final SearchCondVisitor searchCondVisitor,
            final PlainAttrValidationManager validator) {

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
                virAttrHandler,
                mappingManager,
                intAttrNameParser,
                outboundMatcher,
                validator);

        this.searchCondVisitor = searchCondVisitor;
    }

    protected void setDynMembership(final Group group, final AnyType anyType, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(searchCondVisitor, dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }
        if (anyType.getKind() == AnyTypeKind.GROUP) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidAnyType);
            sce.getElements().add(anyType.getKind().name());
            throw sce;
        }

        DynGroupMembership<?> dynMembership;
        if (anyType.getKind() == AnyTypeKind.ANY_OBJECT && group.getADynMembership(anyType).isEmpty()) {
            dynMembership = entityFactory.newEntity(ADynGroupMembership.class);
            dynMembership.setGroup(group);
            ((ADynGroupMembership) dynMembership).setAnyType(anyType);
            group.add((ADynGroupMembership) dynMembership);
        } else if (anyType.getKind() == AnyTypeKind.USER && group.getUDynMembership() == null) {
            dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
            dynMembership.setGroup(group);
            group.setUDynMembership((UDynGroupMembership) dynMembership);
        } else {
            dynMembership = anyType.getKind() == AnyTypeKind.ANY_OBJECT
                    ? group.getADynMembership(anyType).get()
                    : group.getUDynMembership();
        }
        dynMembership.setFIQLCond(dynMembershipFIQL);
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

        // attributes and resources
        fill(anyTO, group, groupCR, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        // owner
        if (groupCR.getUserOwner() != null) {
            userDAO.findById(groupCR.getUserOwner()).ifPresentOrElse(
                    group::setUserOwner,
                    () -> LOG.warn("Ignoring invalid user specified as owner: {}", groupCR.getUserOwner()));
        }
        if (groupCR.getGroupOwner() != null) {
            groupDAO.findById(groupCR.getGroupOwner()).ifPresentOrElse(
                    group::setGroupOwner,
                    () -> LOG.warn("Ignoring invalid group specified as owner: {}", groupCR.getGroupOwner()));
        }

        // dynamic membership
        if (groupCR.getUDynMembershipCond() != null) {
            setDynMembership(group, anyTypeDAO.getUser(), groupCR.getUDynMembershipCond());
        }
        groupCR.getADynMembershipConds().forEach((type, fiql) -> anyTypeDAO.findById(type).ifPresentOrElse(
                anyType -> setDynMembership(group, anyType, fiql),
                () -> LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), type)));

        // type extensions
        groupCR.getTypeExtensions().forEach(typeExtTO -> anyTypeDAO.findById(typeExtTO.getAnyType()).ifPresentOrElse(
                anyType -> {
                    TypeExtension typeExt = entityFactory.newEntity(TypeExtension.class);
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

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        groupCR.getRelationships().forEach(relationshipTO -> {
            AnyObject otherEnd = anyObjectDAO.findById(relationshipTO.getOtherEndKey()).orElse(null);
            if (otherEnd == null) {
                LOG.debug("Ignoring invalid anyObject {}", relationshipTO.getOtherEndKey());
            } else if (relationshipTO.getEnd() == RelationshipTO.End.RIGHT) {
                SyncopeClientException noRight =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                noRight.getElements().add(
                        "Relationships shall be created or updated only from their left end");
                scce.addException(noRight);
            } else if (relationships.contains(Pair.of(otherEnd.getKey(), relationshipTO.getType()))) {
                SyncopeClientException assigned =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                assigned.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " in relationship " + relationshipTO.getType());
                scce.addException(assigned);
            } else if (group.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                relationships.add(Pair.of(otherEnd.getKey(), relationshipTO.getType()));

                relationshipTypeDAO.findById(relationshipTO.getType()).ifPresentOrElse(
                        relationshipType -> {
                            GRelationship relationship = entityFactory.newEntity(GRelationship.class);
                            relationship.setType(relationshipType);
                            relationship.setRightEnd(otherEnd);
                            relationship.setLeftEnd(group);

                            group.add(relationship);
                        },
                        () -> LOG.debug("Ignoring invalid relationship type {}", relationshipTO.getType()));
            } else {
                SyncopeClientException unrelatable =
                        SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                        + " cannot be related");
                scce.addException(unrelatable);
            }
        });

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

        // owner
        PropagationByResource<String> ownerPropByRes = new PropagationByResource<>();
        if (groupUR.getUserOwner() != null) {
            if (groupUR.getUserOwner().getValue() == null) {
                if (group.getUserOwner() != null) {
                    group.setUserOwner(null);
                    ownerPropByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));
                }
            } else {
                User userOwner = userDAO.findById(groupUR.getUserOwner().getValue()).orElse(null);
                if (userOwner == null) {
                    LOG.debug("Unable to find user owner for group {} by key {}",
                            group.getKey(), groupUR.getUserOwner().getValue());
                    group.setUserOwner(null);
                } else {
                    group.setUserOwner(userOwner);
                    ownerPropByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));
                }
            }
        }
        if (groupUR.getGroupOwner() != null) {
            if (groupUR.getGroupOwner().getValue() == null) {
                if (group.getGroupOwner() != null) {
                    group.setGroupOwner(null);
                    ownerPropByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));
                }
            } else {
                Group groupOwner = groupDAO.findById(groupUR.getGroupOwner().getValue()).orElse(null);
                if (groupOwner == null) {
                    LOG.debug("Unable to find group owner for group {} by key {}",
                            group.getKey(), groupUR.getGroupOwner().getValue());
                    group.setGroupOwner(null);
                } else {
                    group.setGroupOwner(groupOwner);
                    ownerPropByRes.addAll(ResourceOperation.UPDATE, groupDAO.findAllResourceKeys(group.getKey()));
                }
            }
        }

        // attributes and resources
        fill(anyTO, group, groupUR, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        group = groupDAO.save(group);

        // dynamic membership
        if (groupUR.getUDynMembershipCond() == null) {
            if (group.getUDynMembership() != null) {
                group.getUDynMembership().setGroup(null);
                group.setUDynMembership(null);
                groupDAO.clearUDynMembers(group);
            }
        } else {
            setDynMembership(group, anyTypeDAO.getUser(), groupUR.getUDynMembershipCond());
        }
        for (Iterator<? extends ADynGroupMembership> itor = group.getADynMemberships().iterator(); itor.hasNext();) {
            ADynGroupMembership memb = itor.next();
            memb.setGroup(null);
            itor.remove();
        }
        groupDAO.clearADynMembers(group);
        for (Map.Entry<String, String> entry : groupUR.getADynMembershipConds().entrySet()) {
            AnyType anyType = anyTypeDAO.findById(entry.getKey()).orElse(null);
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), entry.getKey());
            } else {
                setDynMembership(group, anyType, entry.getValue());
            }
        }

        group = groupDAO.saveAndRefreshDynMemberships(group);

        // type extensions
        for (TypeExtensionTO typeExtTO : groupUR.getTypeExtensions()) {
            AnyType anyType = anyTypeDAO.findById(typeExtTO.getAnyType()).orElse(null);
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType());
            } else {
                TypeExtension typeExt = group.getTypeExtension(anyType).orElse(null);
                if (typeExt == null) {
                    typeExt = entityFactory.newEntity(TypeExtension.class);
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

        // relationships
        Set<Pair<String, String>> relationships = new HashSet<>();
        for (RelationshipUR patch : groupUR.getRelationships().stream().
                filter(patch -> patch.getRelationshipTO() != null).toList()) {

            RelationshipType relationshipType = relationshipTypeDAO.findById(patch.getRelationshipTO().getType()).
                    orElse(null);
            if (relationshipType == null) {
                LOG.debug("Ignoring invalid relationship type {}", patch.getRelationshipTO().getType());
            } else {
                GRelationship relationship = group.getRelationship(
                        relationshipType, patch.getRelationshipTO().getOtherEndKey()).orElse(null);
                if (relationship != null) {
                    group.getRelationships().remove(relationship);
                    relationship.setLeftEnd(null);
                }

                if (patch.getOperation() == PatchOperation.ADD_REPLACE) {
                    AnyObject otherEnd = anyObjectDAO.findById(patch.getRelationshipTO().getOtherEndKey()).orElse(null);
                    if (otherEnd == null) {
                        LOG.debug("Ignoring invalid any object {}", patch.getRelationshipTO().getOtherEndKey());
                    } else if (relationships.contains(
                            Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()))) {

                        SyncopeClientException assigned =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        assigned.getElements().add("Group was already in relationship "
                                + patch.getRelationshipTO().getType() + " with "
                                + otherEnd.getType().getKey() + " " + otherEnd.getName());
                        scce.addException(assigned);
                    } else if (patch.getRelationshipTO().getEnd() == RelationshipTO.End.RIGHT) {
                        SyncopeClientException noRight =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        noRight.getElements().add(
                                "Relationships shall be created or updated only from their left end");
                        scce.addException(noRight);
                    } else if (group.getRealm().getFullPath().startsWith(otherEnd.getRealm().getFullPath())) {
                        relationships.add(Pair.of(otherEnd.getKey(), patch.getRelationshipTO().getType()));

                        GRelationship newRelationship = entityFactory.newEntity(GRelationship.class);
                        newRelationship.setType(relationshipType);
                        newRelationship.setRightEnd(otherEnd);
                        newRelationship.setLeftEnd(group);

                        group.add(newRelationship);
                    } else {
                        LOG.error("{} cannot be related to {}", otherEnd, group);

                        SyncopeClientException unrelatable =
                                SyncopeClientException.build(ClientExceptionType.InvalidRelationship);
                        unrelatable.getElements().add(otherEnd.getType().getKey() + " " + otherEnd.getName()
                                + " cannot be related");
                        scce.addException(unrelatable);
                    }
                }
            }
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        // Re-merge any pending change from above
        group = groupDAO.save(group);

        // Build final information for next stage (propagation)
        PropagationByResource<String> propByRes = propByRes(
                beforeOnResources, onResources(group, groupDAO.findAllResourceKeys(group.getKey()), null, Set.of()));
        propByRes.merge(ownerPropByRes);
        return propByRes;
    }

    @Override
    public TypeExtensionTO getTypeExtensionTO(final TypeExtension typeExt) {
        TypeExtensionTO typeExtTO = new TypeExtensionTO();
        typeExtTO.setAnyType(typeExt.getAnyType().getKey());
        typeExtTO.getAuxClasses().addAll(
                typeExt.getAuxClasses().stream().map(AnyTypeClass::getKey).toList());
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

        if (group.getUserOwner() != null) {
            groupTO.setUserOwner(group.getUserOwner().getKey());
        }
        if (group.getGroupOwner() != null) {
            groupTO.setGroupOwner(group.getGroupOwner().getKey());
        }

        Map<DerSchema, String> derAttrValues = derAttrHandler.getValues(group);
        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHandler.getValues(group)
                : Collections.emptyMap();
        fillTO(groupTO,
                group.getRealm().getFullPath(),
                group.getAuxClasses(),
                group.getPlainAttrs(),
                derAttrValues,
                virAttrValues,
                group.getResources());

        // dynamic realms
        groupTO.getDynRealms().addAll(groupDAO.findDynRealms(group.getKey()));

        // Static user and AnyType membership counts
        groupTO.setStaticUserMembershipCount(groupDAO.countUMembers(group.getKey()));
        groupTO.setStaticAnyObjectMembershipCount(groupDAO.countAMembers(group.getKey()));

        // Dynamic user and AnyType membership counts
        groupTO.setDynamicUserMembershipCount(groupDAO.countUDynMembers(group));
        groupTO.setDynamicAnyObjectMembershipCount(groupDAO.countADynMembers(group));

        if (group.getUDynMembership() != null) {
            groupTO.setUDynMembershipCond(group.getUDynMembership().getFIQLCond());
        }
        group.getADynMemberships().
                forEach(memb -> groupTO.getADynMembershipConds().put(memb.getAnyType().getKey(), memb.getFIQLCond()));

        group.getTypeExtensions().
                forEach(typeExt -> groupTO.getTypeExtensions().add(getTypeExtensionTO(typeExt)));

        if (details) {
            // relationships
            groupTO.getRelationships().addAll(group.getRelationships().stream().map(relationship -> getRelationshipTO(
                    relationship.getType().getKey(), RelationshipTO.End.LEFT, relationship.getRightEnd())).
                    toList());
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
            final Groupable<?, ?, ?, ?> any,
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

        groupDAO.findUMemberships(group).
                forEach((membership) -> populateTransitiveResources(group, membership.getLeftEnd(), result));

        return result;
    }
}
