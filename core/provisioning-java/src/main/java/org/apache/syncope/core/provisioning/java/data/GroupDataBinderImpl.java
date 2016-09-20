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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.PropagationByResource;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.persistence.api.search.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(rollbackFor = { Throwable.class })
public class GroupDataBinderImpl extends AbstractAnyDataBinder implements GroupDataBinder {

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    private void setDynMembership(final Group group, final AnyType anyType, final String dynMembershipFIQL) {
        SearchCond dynMembershipCond = SearchCondConverter.convert(dynMembershipFIQL);
        if (!dynMembershipCond.isValid()) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(dynMembershipFIQL);
            throw sce;
        }

        DynGroupMembership<?> dynMembership;
        if (anyType.getKind() == AnyTypeKind.ANY_OBJECT && group.getADynMembership(anyType) == null) {
            dynMembership = entityFactory.newEntity(ADynGroupMembership.class);
            dynMembership.setGroup(group);
            group.add((ADynGroupMembership) dynMembership);
            ((ADynGroupMembership) dynMembership).setAnyType(anyType);
        } else if (anyType.getKind() == AnyTypeKind.USER && group.getUDynMembership() == null) {
            dynMembership = entityFactory.newEntity(UDynGroupMembership.class);
            dynMembership.setGroup(group);
            group.setUDynMembership((UDynGroupMembership) dynMembership);
        } else {
            dynMembership = anyType.getKind() == AnyTypeKind.ANY_OBJECT
                    ? group.getADynMembership(anyType)
                    : group.getUDynMembership();
        }
        dynMembership.setFIQLCond(dynMembershipFIQL);
    }

    @Override
    public void create(final Group group, final GroupTO groupTO) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // name
        SyncopeClientException invalidGroups = SyncopeClientException.build(ClientExceptionType.InvalidGroup);
        if (groupTO.getName() == null) {
            LOG.error("No name specified for this group");

            invalidGroups.getElements().add("No name specified for this group");
        } else {
            group.setName(groupTO.getName());
        }

        // realm
        Realm realm = realmDAO.findByFullPath(groupTO.getRealm());
        if (realm == null) {
            SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
            noRealm.getElements().add("Invalid or null realm specified: " + groupTO.getRealm());
            scce.addException(noRealm);
        }
        group.setRealm(realm);

        // attributes and resources
        fill(group, groupTO, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce);

        // owner
        if (groupTO.getUserOwner() != null) {
            User owner = userDAO.find(groupTO.getUserOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid user specified as owner: {}", groupTO.getUserOwner());
            } else {
                group.setUserOwner(owner);
            }
        }
        if (groupTO.getGroupOwner() != null) {
            Group owner = groupDAO.find(groupTO.getGroupOwner());
            if (owner == null) {
                LOG.warn("Ignoring invalid group specified as owner: {}", groupTO.getGroupOwner());
            } else {
                group.setGroupOwner(owner);
            }
        }

        // dynamic membership
        if (groupTO.getUDynMembershipCond() != null) {
            setDynMembership(group, anyTypeDAO.findUser(), groupTO.getUDynMembershipCond());
        }
        for (Map.Entry<String, String> entry : groupTO.getADynMembershipConds().entrySet()) {
            AnyType anyType = anyTypeDAO.find(entry.getKey());
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), entry.getKey());
            } else {
                setDynMembership(group, anyType, entry.getValue());
            }
        }

        // type extensions
        for (TypeExtensionTO typeExtTO : groupTO.getTypeExtensions()) {
            AnyType anyType = anyTypeDAO.find(typeExtTO.getAnyType());
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType());
            } else {
                TypeExtension typeExt = entityFactory.newEntity(TypeExtension.class);
                typeExt.setAnyType(anyType);
                typeExt.setGroup(group);
                group.add(typeExt);

                for (String name : typeExtTO.getAuxClasses()) {
                    AnyTypeClass anyTypeClass = anyTypeClassDAO.find(name);
                    if (anyTypeClass == null) {
                        LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), name);
                    } else {
                        typeExt.add(anyTypeClass);
                    }
                }

                if (typeExt.getAuxClasses().isEmpty()) {
                    group.getTypeExtensions().remove(typeExt);
                    typeExt.setGroup(null);
                }
            }
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public PropagationByResource update(final Group toBeUpdated, final GroupPatch groupPatch) {
        // Re-merge any pending change from workflow tasks
        Group group = groupDAO.save(toBeUpdated);

        PropagationByResource propByRes = new PropagationByResource();

        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();

        // fetch connObjectKeys before update
        Map<String, String> oldConnObjectKeys = getConnObjectKeys(group);

        // realm
        setRealm(group, groupPatch);

        // name
        if (groupPatch.getName() != null && StringUtils.isNotBlank(groupPatch.getName().getValue())) {
            propByRes.addAll(ResourceOperation.UPDATE, group.getResourceKeys());

            group.setName(groupPatch.getName().getValue());
        }

        // owner
        if (groupPatch.getUserOwner() != null) {
            group.setUserOwner(groupPatch.getUserOwner().getValue() == null
                    ? null
                    : userDAO.find(groupPatch.getUserOwner().getValue()));
        }
        if (groupPatch.getGroupOwner() != null) {
            group.setGroupOwner(groupPatch.getGroupOwner().getValue() == null
                    ? null
                    : groupDAO.find(groupPatch.getGroupOwner().getValue()));
        }

        // attributes and resources
        propByRes.merge(fill(group, groupPatch, anyUtilsFactory.getInstance(AnyTypeKind.GROUP), scce));

        // check if some connObjectKey was changed by the update above
        Map<String, String> newConnObjectKeys = getConnObjectKeys(group);
        for (Map.Entry<String, String> entry : oldConnObjectKeys.entrySet()) {
            if (newConnObjectKeys.containsKey(entry.getKey())
                    && !entry.getValue().equals(newConnObjectKeys.get(entry.getKey()))) {

                propByRes.addOldConnObjectKey(entry.getKey(), entry.getValue());
                propByRes.add(ResourceOperation.UPDATE, entry.getKey());
            }
        }

        // dynamic membership
        if (groupPatch.getUDynMembershipCond() == null) {
            if (group.getUDynMembership() != null) {
                group.getUDynMembership().setGroup(null);
                group.setUDynMembership(null);
            }
        } else {
            setDynMembership(group, anyTypeDAO.findUser(), groupPatch.getUDynMembershipCond());
        }
        for (Iterator<? extends ADynGroupMembership> itor = group.getADynMemberships().iterator(); itor.hasNext();) {
            ADynGroupMembership memb = itor.next();
            memb.setGroup(null);
            itor.remove();
        }
        for (Map.Entry<String, String> entry : groupPatch.getADynMembershipConds().entrySet()) {
            AnyType anyType = anyTypeDAO.find(entry.getKey());
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), entry.getKey());
            } else {
                setDynMembership(group, anyType, entry.getValue());
            }
        }

        // type extensions
        for (TypeExtensionTO typeExtTO : groupPatch.getTypeExtensions()) {
            AnyType anyType = anyTypeDAO.find(typeExtTO.getAnyType());
            if (anyType == null) {
                LOG.warn("Ignoring invalid {}: {}", AnyType.class.getSimpleName(), typeExtTO.getAnyType());
            } else {
                TypeExtension typeExt = group.getTypeExtension(anyType);
                if (typeExt == null) {
                    typeExt = entityFactory.newEntity(TypeExtension.class);
                    typeExt.setAnyType(anyType);
                    typeExt.setGroup(group);
                    group.add(typeExt);
                }

                // add all classes contained in the TO
                for (String name : typeExtTO.getAuxClasses()) {
                    AnyTypeClass anyTypeClass = anyTypeClassDAO.find(name);
                    if (anyTypeClass == null) {
                        LOG.warn("Ignoring invalid {}: {}", AnyTypeClass.class.getSimpleName(), name);
                    } else {
                        typeExt.add(anyTypeClass);
                    }
                }
                // remove all classes not contained in the TO
                for (Iterator<? extends AnyTypeClass> itor = typeExt.getAuxClasses().iterator(); itor.hasNext();) {
                    AnyTypeClass anyTypeClass = itor.next();
                    if (!typeExtTO.getAuxClasses().contains(anyTypeClass.getKey())) {
                        itor.remove();
                    }
                }

                // only consider non-empty type extensions
                if (typeExt.getAuxClasses().isEmpty()) {
                    group.getTypeExtensions().remove(typeExt);
                    typeExt.setGroup(null);
                }
            }
        }
        // remove all type extensions not contained in the TO
        for (Iterator<? extends TypeExtension> itor = group.getTypeExtensions().iterator(); itor.hasNext();) {
            TypeExtension typeExt = itor.next();
            if (groupPatch.getTypeExtension(typeExt.getAnyType().getKey()) == null) {
                itor.remove();
            }
        }

        // Throw composite exception if there is at least one element set in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return propByRes;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final Group group, final boolean details) {
        GroupTO groupTO = new GroupTO();

        // set sys info
        groupTO.setCreator(group.getCreator());
        groupTO.setCreationDate(group.getCreationDate());
        groupTO.setLastModifier(group.getLastModifier());
        groupTO.setLastChangeDate(group.getLastChangeDate());

        groupTO.setKey(group.getKey());
        groupTO.setName(group.getName());

        if (group.getUserOwner() != null) {
            groupTO.setUserOwner(group.getUserOwner().getKey());
        }
        if (group.getGroupOwner() != null) {
            groupTO.setGroupOwner(group.getGroupOwner().getKey());
        }

        Map<DerSchema, String> derAttrValues = derAttrHandler.getValues(group);
        Map<VirSchema, List<String>> virAttrValues = details
                ? virAttrHandler.getValues(group)
                : Collections.<VirSchema, List<String>>emptyMap();
        fillTO(groupTO,
                group.getRealm().getFullPath(),
                group.getAuxClasses(),
                group.getPlainAttrs(),
                derAttrValues,
                virAttrValues,
                group.getResources(),
                details);

        if (group.getUDynMembership() != null) {
            groupTO.setUDynMembershipCond(group.getUDynMembership().getFIQLCond());
        }
        for (ADynGroupMembership memb : group.getADynMemberships()) {
            groupTO.getADynMembershipConds().put(memb.getAnyType().getKey(), memb.getFIQLCond());
        }

        for (TypeExtension typeExt : group.getTypeExtensions()) {
            TypeExtensionTO typeExtTO = new TypeExtensionTO();
            typeExtTO.setAnyType(typeExt.getAnyType().getKey());
            typeExtTO.getAuxClasses().addAll(CollectionUtils.collect(typeExt.getAuxClasses(),
                    new Transformer<AnyTypeClass, String>() {

                @Override
                public String transform(final AnyTypeClass clazz) {
                    return clazz.getKey();
                }
            }));
            groupTO.getTypeExtensions().add(typeExtTO);
        }

        return groupTO;
    }

    @Transactional(readOnly = true)
    @Override
    public GroupTO getGroupTO(final String key) {
        return SyncopeConstants.UUID_PATTERN.matcher(key).matches()
                ? getGroupTO(groupDAO.authFind(key), true)
                : getGroupTO(groupDAO.authFindByName(key), true);
    }

    private void populateTransitiveResources(
            final Group group, final Any<?> any, final Map<String, PropagationByResource> result) {

        PropagationByResource propByRes = new PropagationByResource();
        for (ExternalResource resource : group.getResources()) {
            if (!any.getResources().contains(resource)) {
                propByRes.add(ResourceOperation.DELETE, resource.getKey());
            }

            if (!propByRes.isEmpty()) {
                result.put(any.getKey(), propByRes);
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, PropagationByResource> findAnyObjectsWithTransitiveResources(final String groupKey) {
        Group group = groupDAO.authFind(groupKey);

        Map<String, PropagationByResource> result = new HashMap<>();

        for (AMembership membership : groupDAO.findAMemberships(group)) {
            populateTransitiveResources(group, membership.getLeftEnd(), result);
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, PropagationByResource> findUsersWithTransitiveResources(final String groupKey) {
        Group group = groupDAO.authFind(groupKey);

        Map<String, PropagationByResource> result = new HashMap<>();

        for (UMembership membership : groupDAO.findUMemberships(group)) {
            populateTransitiveResources(group, membership.getLeftEnd(), result);
        }

        return result;
    }
}
