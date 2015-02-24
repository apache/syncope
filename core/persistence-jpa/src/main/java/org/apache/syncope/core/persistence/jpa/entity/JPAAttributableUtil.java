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
package org.apache.syncope.core.persistence.jpa.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AbstractSubjectTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.AttrTemplate;
import org.apache.syncope.core.persistence.api.entity.Attributable;
import org.apache.syncope.core.persistence.api.entity.AttributableUtil;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.MappingItem;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.role.JPARole;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.misc.spring.BeanUtils;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JPAAttributableUtil implements AttributableUtil {

    /**
     * Logger.
     */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AttributableUtil.class);

    private final AttributableType type;

    protected JPAAttributableUtil(final AttributableType type) {
        this.type = type;
    }

    @Override
    public AttributableType getType() {
        return type;
    }

    @Override
    public <T extends Attributable<?, ?, ?>> Class<T> attributableClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = JPARole.class;
                break;

            case MEMBERSHIP:
                result = JPAMembership.class;
                break;

            case CONFIGURATION:
                result = JPAConf.class;
                break;

            case USER:
            default:
                result = JPAUser.class;
        }

        return result;
    }

    @Override
    public <T extends PlainSchema> Class<T> plainSchemaClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = JPARPlainSchema.class;
                break;

            case MEMBERSHIP:
                result = JPAMPlainSchema.class;
                break;

            case CONFIGURATION:
                result = JPACPlainSchema.class;
                break;

            case USER:
            default:
                result = JPAUPlainSchema.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainSchema> T newPlainSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUPlainSchema();
                break;

            case ROLE:
                result = (T) new JPARPlainSchema();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMPlainSchema();
                break;

            case CONFIGURATION:
                result = (T) new JPACPlainSchema();
            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttr> Class<T> plainAttrClass() {
        Class result = null;

        switch (type) {
            case ROLE:
                result = JPARPlainAttr.class;
                break;

            case MEMBERSHIP:
                result = JPAMPlainAttr.class;
                break;

            case CONFIGURATION:
                result = JPACPlainAttr.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttr.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttr> T newPlainAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUPlainAttr();
                break;

            case ROLE:
                result = (T) new JPARPlainAttr();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMPlainAttr();
                break;

            case CONFIGURATION:
                result = (T) new JPACPlainAttr();

            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> Class<T> plainAttrValueClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = JPARPlainAttrValue.class;
                break;

            case MEMBERSHIP:
                result = JPAMPlainAttrValue.class;
                break;

            case CONFIGURATION:
                result = JPACPlainAttrValue.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttrValue.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUPlainAttrValue();
                break;

            case ROLE:
                result = (T) new JPARPlainAttrValue();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMPlainAttrValue();
                break;

            case CONFIGURATION:
                result = (T) new JPACPlainAttrValue();
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> Class<T> plainAttrUniqueValueClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = JPARPlainAttrUniqueValue.class;
                break;

            case MEMBERSHIP:
                result = JPAMPlainAttrUniqueValue.class;
                break;

            case CONFIGURATION:
                result = JPACPlainAttrUniqueValue.class;
                break;

            case USER:
            default:
                result = JPAUPlainAttrUniqueValue.class;
                break;
        }

        return result;
    }

    @Override
    public <T extends PlainAttrValue> T newPlainAttrUniqueValue() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUPlainAttrUniqueValue();
                break;

            case ROLE:
                result = (T) new JPARPlainAttrUniqueValue();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMPlainAttrUniqueValue();
                break;

            case CONFIGURATION:
                result = (T) new JPACPlainAttrUniqueValue();
                break;

            default:
        }

        return result;
    }

    @Override
    public <T extends AttrTemplate<PlainSchema>> Class<T> plainAttrTemplateClass() {
        Class result;

        switch (type) {
            case ROLE:
                result = JPARPlainAttrTemplate.class;
                break;

            case MEMBERSHIP:
                result = JPAMPlainAttrTemplate.class;
                break;

            case USER:
            case CONFIGURATION:
            default:
                result = null;
        }

        return result;
    }

    @Override
    public <T extends DerSchema> Class<T> derSchemaClass() {
        Class result;

        switch (type) {
            case USER:
                result = JPAUDerSchema.class;
                break;

            case ROLE:
                result = JPARDerSchema.class;
                break;

            case MEMBERSHIP:
                result = JPAMDerSchema.class;
                break;

            case CONFIGURATION:
            default:
                result = null;
        }

        return result;
    }

    @Override
    public <T extends DerSchema> T newDerSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUDerSchema();
                break;

            case ROLE:
                result = (T) new JPARDerSchema();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMDerSchema();
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends DerAttr> Class<T> derAttrClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = JPAUDerAttr.class;
                break;

            case ROLE:
                result = JPARDerAttr.class;
                break;

            case MEMBERSHIP:
                result = JPAMDerAttr.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends DerAttr> T newDerAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUDerAttr();
                break;

            case ROLE:
                result = (T) new JPARDerAttr();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMDerAttr();
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends AttrTemplate<DerSchema>> Class<T> derAttrTemplateClass() {
        Class result = null;

        switch (type) {
            case USER:
                break;

            case ROLE:
                result = JPARDerAttrTemplate.class;
                break;

            case MEMBERSHIP:
                result = JPAMDerAttrTemplate.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends VirSchema> Class<T> virSchemaClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = JPAUVirSchema.class;
                break;

            case ROLE:
                result = JPARVirSchema.class;
                break;

            case MEMBERSHIP:
                result = JPAMVirSchema.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends VirSchema> T newVirSchema() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUVirSchema();
                break;

            case ROLE:
                result = (T) new JPARVirSchema();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMVirSchema();
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends VirAttr> Class<T> virAttrClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = JPAUVirAttr.class;
                break;

            case ROLE:
                result = JPARVirAttr.class;
                break;

            case MEMBERSHIP:
                result = JPAMVirAttr.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends VirAttr> T newVirAttr() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new JPAUVirAttr();
                break;

            case ROLE:
                result = (T) new JPARVirAttr();
                break;

            case MEMBERSHIP:
                result = (T) new JPAMVirAttr();
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends AttrTemplate<VirSchema>> Class<T> virAttrTemplateClass() {
        Class result = null;

        switch (type) {
            case USER:
                break;

            case ROLE:
                result = JPARVirAttrTemplate.class;
                break;

            case MEMBERSHIP:
                result = JPAMVirAttrTemplate.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends MappingItem> T getAccountIdItem(final ExternalResource resource) {
        T result = null;

        if (resource != null) {
            switch (type) {
                case ROLE:
                    if (resource.getRmapping() != null) {
                        result = (T) resource.getRmapping().getAccountIdItem();
                    }
                    break;

                case MEMBERSHIP:
                case USER:
                    if (resource.getUmapping() != null) {
                        result = (T) resource.getUmapping().getAccountIdItem();
                    }
                    break;

                default:
            }
        }

        return result;
    }

    @Override
    public String getAccountLink(final ExternalResource resource) {
        String result = null;

        if (resource != null) {
            switch (type) {
                case USER:
                    if (resource.getUmapping() != null) {
                        result = resource.getUmapping().getAccountLink();
                    }
                    break;

                case ROLE:
                    if (resource.getRmapping() != null) {
                        result = resource.getRmapping().getAccountLink();
                    }
                    break;

                case MEMBERSHIP:
                case CONFIGURATION:
                default:
            }
        }

        return result;
    }

    @Override
    public <T extends MappingItem> List<T> getMappingItems(
            final ExternalResource resource, final MappingPurpose purpose) {

        List<T> items = Collections.<T>emptyList();

        if (resource != null) {
            switch (type) {
                case ROLE:
                    if (resource.getRmapping() != null) {
                        items = (List<T>) resource.getRmapping().getItems();
                    }
                    break;

                case MEMBERSHIP:
                case USER:
                    if (resource.getUmapping() != null) {
                        items = (List<T>) resource.getUmapping().getItems();
                    }
                    break;

                default:
            }
        }

        final List<T> result = new ArrayList<>();

        switch (purpose) {
            case SYNCHRONIZATION:
                for (T item : items) {
                    if (MappingPurpose.PROPAGATION != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case PROPAGATION:
                for (T item : items) {
                    if (MappingPurpose.SYNCHRONIZATION != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case BOTH:
                for (T item : items) {
                    if (MappingPurpose.NONE != item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;

            case NONE:
                for (T item : items) {
                    if (MappingPurpose.NONE == item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;
            default:
                LOG.error("You requested not existing purpose {}", purpose);
        }

        return result;
    }

    @Override
    public <T extends MappingItem> List<T> getUidToMappingItems(
            final ExternalResource resource, final MappingPurpose purpose) {

        List<T> items = getMappingItems(resource, purpose);

        MappingItem uidItem = type == AttributableType.USER ? new JPAUMappingItem() : new JPARMappingItem();
        BeanUtils.copyProperties(getAccountIdItem(resource), uidItem);
        uidItem.setExtAttrName(Uid.NAME);
        uidItem.setAccountid(false);
        items.add((T) uidItem);

        return items;
    }

    @Override
    public IntMappingType plainIntMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RolePlainSchema;
                break;

            case MEMBERSHIP:
                result = IntMappingType.MembershipPlainSchema;
                break;

            case USER:
                result = IntMappingType.UserPlainSchema;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public IntMappingType derIntMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleDerivedSchema;
                break;

            case MEMBERSHIP:
                result = IntMappingType.MembershipDerivedSchema;
                break;

            case USER:
                result = IntMappingType.UserDerivedSchema;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public IntMappingType virIntMappingType() {
        IntMappingType result = null;

        switch (type) {
            case ROLE:
                result = IntMappingType.RoleVirtualSchema;
                break;

            case MEMBERSHIP:
                result = IntMappingType.MembershipVirtualSchema;
                break;

            case USER:
                result = IntMappingType.UserVirtualSchema;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends MappingItem> Class<T> mappingItemClass() {
        Class result = null;

        switch (type) {
            case USER:
                result = JPAUMappingItem.class;
                break;

            case ROLE:
                result = JPARMappingItem.class;
                break;

            case MEMBERSHIP:
                result = AbstractMappingItem.class;
                break;

            case CONFIGURATION:
            default:
        }

        return result;
    }

    @Override
    public <T extends AbstractAttributableTO> T newAttributableTO() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UserTO();
                break;
            case ROLE:
                result = (T) new RoleTO();
                break;
            case MEMBERSHIP:
                result = (T) new MembershipTO();
                break;
            case CONFIGURATION:
                result = (T) new ConfTO();
                break;
            default:
        }

        return result;
    }

    @Override
    public <T extends AbstractSubjectTO> T newSubjectTO() {
        T result = null;

        switch (type) {
            case USER:
                result = (T) new UserTO();
                break;
            case ROLE:
                result = (T) new RoleTO();
                break;
            case MEMBERSHIP:
            case CONFIGURATION:
            default:
                break;
        }

        return result;
    }
}
