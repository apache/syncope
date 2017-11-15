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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.data.Value;
import org.apache.syncope.ext.scimv2.api.data.Group;
import org.apache.syncope.ext.scimv2.api.data.Member;
import org.apache.syncope.ext.scimv2.api.data.Meta;
import org.apache.syncope.ext.scimv2.api.data.SCIMComplexValue;
import org.apache.syncope.ext.scimv2.api.data.SCIMEnterpriseInfo;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserAddress;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserManager;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserName;
import org.apache.syncope.ext.scimv2.api.type.Function;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SCIMDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMDataBinder.class);

    @Autowired
    private SCIMConfManager confManager;

    @Autowired
    private UserLogic userLogic;

    @Autowired
    private AuthDataAccessor authDataAccessor;

    private <E extends Enum<?>> void fill(
            final Map<String, AttrTO> attrs,
            final List<SCIMComplexConf<E>> confs,
            final List<SCIMComplexValue> values) {

        confs.forEach(conf -> {
            SCIMComplexValue value = new SCIMComplexValue();

            if (conf.getValue() != null && attrs.containsKey(conf.getValue())) {
                value.setValue(attrs.get(conf.getValue()).getValues().get(0));
            }
            if (conf.getDisplay() != null && attrs.containsKey(conf.getDisplay())) {
                value.setDisplay(attrs.get(conf.getDisplay()).getValues().get(0));
            }
            if (conf.getType() != null) {
                value.setType(conf.getType().name());
            }
            if (conf.isPrimary()) {
                value.setPrimary(true);
            }

            if (!value.isEmpty()) {
                values.add(value);
            }
        });
    }

    public SCIMUser toSCIMUser(final UserTO userTO, final String location) {
        SCIMConf conf = confManager.get();

        List<String> schemas = new ArrayList<>();
        schemas.add(Resource.User.schema());
        if (conf.getEnterpriseUserConf() != null) {
            schemas.add(Resource.EnterpriseUser.schema());
        }

        SCIMUser user = new SCIMUser(
                userTO.getKey(),
                schemas,
                new Meta(
                        Resource.User,
                        userTO.getCreationDate(),
                        userTO.getLastChangeDate() == null
                        ? userTO.getCreationDate() : userTO.getLastChangeDate(),
                        userTO.getETagValue(),
                        location),
                userTO.getUsername(),
                !userTO.isSuspended());

        Map<String, AttrTO> attrs = new HashMap<>();
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getPlainAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getDerAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getVirAttrs()));

        if (conf.getUserConf() != null) {
            if (conf.getUserConf().getName() != null) {
                SCIMUserName name = new SCIMUserName();

                if (conf.getUserConf().getName().getFamilyName() != null
                        && attrs.containsKey(conf.getUserConf().getName().getFamilyName())) {

                    name.setFamilyName(attrs.get(conf.getUserConf().getName().getFamilyName()).getValues().get(0));
                }
                if (conf.getUserConf().getName().getFormatted() != null
                        && attrs.containsKey(conf.getUserConf().getName().getFormatted())) {

                    name.setFormatted(attrs.get(conf.getUserConf().getName().getFormatted()).getValues().get(0));
                }
                if (conf.getUserConf().getName().getGivenName() != null
                        && attrs.containsKey(conf.getUserConf().getName().getGivenName())) {

                    name.setGivenName(attrs.get(conf.getUserConf().getName().getGivenName()).getValues().get(0));
                }
                if (conf.getUserConf().getName().getHonorificPrefix() != null
                        && attrs.containsKey(conf.getUserConf().getName().getHonorificPrefix())) {

                    name.setHonorificPrefix(
                            attrs.get(conf.getUserConf().getName().getHonorificPrefix()).getValues().get(0));
                }
                if (conf.getUserConf().getName().getHonorificSuffix() != null
                        && attrs.containsKey(conf.getUserConf().getName().getHonorificSuffix())) {

                    name.setHonorificSuffix(
                            attrs.get(conf.getUserConf().getName().getHonorificSuffix()).getValues().get(0));
                }
                if (conf.getUserConf().getName().getMiddleName() != null
                        && attrs.containsKey(conf.getUserConf().getName().getMiddleName())) {

                    name.setMiddleName(attrs.get(conf.getUserConf().getName().getMiddleName()).getValues().get(0));
                }

                if (!name.isEmpty()) {
                    user.setName(name);
                }
            }

            if (conf.getUserConf().getDisplayName() != null
                    && attrs.containsKey(conf.getUserConf().getDisplayName())) {

                user.setDisplayName(attrs.get(conf.getUserConf().getDisplayName()).getValues().get(0));
            }

            if (conf.getUserConf().getNickName() != null
                    && attrs.containsKey(conf.getUserConf().getNickName())) {

                user.setNickName(attrs.get(conf.getUserConf().getNickName()).getValues().get(0));
            }

            if (conf.getUserConf().getProfileUrl() != null
                    && attrs.containsKey(conf.getUserConf().getProfileUrl())) {

                user.setProfileUrl(attrs.get(conf.getUserConf().getProfileUrl()).getValues().get(0));
            }

            if (conf.getUserConf().getTitle() != null
                    && attrs.containsKey(conf.getUserConf().getTitle())) {

                user.setTitle(attrs.get(conf.getUserConf().getTitle()).getValues().get(0));
            }

            if (conf.getUserConf().getUserType() != null
                    && attrs.containsKey(conf.getUserConf().getUserType())) {

                user.setUserType(attrs.get(conf.getUserConf().getUserType()).getValues().get(0));
            }

            if (conf.getUserConf().getPreferredLanguage() != null
                    && attrs.containsKey(conf.getUserConf().getPreferredLanguage())) {

                user.setPreferredLanguage(attrs.get(conf.getUserConf().getPreferredLanguage()).getValues().get(0));
            }

            if (conf.getUserConf().getLocale() != null
                    && attrs.containsKey(conf.getUserConf().getLocale())) {

                user.setLocale(attrs.get(conf.getUserConf().getLocale()).getValues().get(0));
            }

            if (conf.getUserConf().getTimezone() != null
                    && attrs.containsKey(conf.getUserConf().getTimezone())) {

                user.setTimezone(attrs.get(conf.getUserConf().getTimezone()).getValues().get(0));
            }

            fill(attrs, conf.getUserConf().getEmails(), user.getEmails());
            fill(attrs, conf.getUserConf().getPhoneNumbers(), user.getPhoneNumbers());
            fill(attrs, conf.getUserConf().getIms(), user.getIms());
            fill(attrs, conf.getUserConf().getPhotos(), user.getPhotos());
            conf.getUserConf().getAddresses().forEach(addressConf -> {
                SCIMUserAddress address = new SCIMUserAddress();

                if (addressConf.getFormatted() != null && attrs.containsKey(addressConf.getFormatted())) {
                    address.setFormatted(attrs.get(addressConf.getFormatted()).getValues().get(0));
                }
                if (addressConf.getStreetAddress() != null && attrs.containsKey(addressConf.getStreetAddress())) {
                    address.setStreetAddress(attrs.get(addressConf.getStreetAddress()).getValues().get(0));
                }
                if (addressConf.getLocality() != null && attrs.containsKey(addressConf.getLocality())) {
                    address.setLocality(attrs.get(addressConf.getLocality()).getValues().get(0));
                }
                if (addressConf.getRegion() != null && attrs.containsKey(addressConf.getRegion())) {
                    address.setRegion(attrs.get(addressConf.getRegion()).getValues().get(0));
                }
                if (addressConf.getCountry() != null && attrs.containsKey(addressConf.getCountry())) {
                    address.setCountry(attrs.get(addressConf.getCountry()).getValues().get(0));
                }
                if (addressConf.getType() != null) {
                    address.setType(addressConf.getType().name());
                }
                if (addressConf.isPrimary()) {
                    address.setPrimary(true);
                }

                if (!address.isEmpty()) {
                    user.getAddresses().add(address);
                }
            });

            conf.getUserConf().getX509Certificates().stream().
                    filter(certificate -> attrs.containsKey(certificate)).
                    forEachOrdered(certificate -> {
                        user.getX509Certificates().add(new Value(attrs.get(certificate).getValues().get(0)));
                    });
        }

        if (conf.getEnterpriseUserConf() != null) {
            SCIMEnterpriseInfo enterpriseInfo = new SCIMEnterpriseInfo();

            if (conf.getEnterpriseUserConf().getEmployeeNumber() != null
                    && attrs.containsKey(conf.getEnterpriseUserConf().getEmployeeNumber())) {

                enterpriseInfo.setEmployeeNumber(
                        attrs.get(conf.getEnterpriseUserConf().getEmployeeNumber()).getValues().get(0));
            }
            if (conf.getEnterpriseUserConf().getCostCenter() != null
                    && attrs.containsKey(conf.getEnterpriseUserConf().getCostCenter())) {

                enterpriseInfo.setCostCenter(
                        attrs.get(conf.getEnterpriseUserConf().getCostCenter()).getValues().get(0));
            }
            if (conf.getEnterpriseUserConf().getOrganization() != null
                    && attrs.containsKey(conf.getEnterpriseUserConf().getOrganization())) {

                enterpriseInfo.setOrganization(
                        attrs.get(conf.getEnterpriseUserConf().getOrganization()).getValues().get(0));
            }
            if (conf.getEnterpriseUserConf().getDivision() != null
                    && attrs.containsKey(conf.getEnterpriseUserConf().getDivision())) {

                enterpriseInfo.setDivision(
                        attrs.get(conf.getEnterpriseUserConf().getDivision()).getValues().get(0));
            }
            if (conf.getEnterpriseUserConf().getDepartment() != null
                    && attrs.containsKey(conf.getEnterpriseUserConf().getDepartment())) {

                enterpriseInfo.setDepartment(
                        attrs.get(conf.getEnterpriseUserConf().getDepartment()).getValues().get(0));
            }
            if (conf.getEnterpriseUserConf().getManager() != null) {
                SCIMUserManager manager = new SCIMUserManager();

                if (conf.getEnterpriseUserConf().getManager().getManager() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getManager().getManager())) {

                    try {
                        UserTO userManager = userLogic.read(
                                attrs.get(conf.getEnterpriseUserConf().getManager().getManager()).getValues().get(0));
                        manager.setValue(userManager.getKey());
                        manager.setRef(
                                StringUtils.substringBefore(location, "/Users") + "/Users/" + userManager.getKey());

                        if (conf.getEnterpriseUserConf().getManager().getDisplayName() != null) {
                            AttrTO displayName = userManager.getPlainAttr(
                                    conf.getEnterpriseUserConf().getManager().getDisplayName()).orElse(null);
                            if (displayName == null) {
                                displayName = userManager.getDerAttr(
                                        conf.getEnterpriseUserConf().getManager().getDisplayName()).orElse(null);
                            }
                            if (displayName == null) {
                                displayName = userManager.getVirAttr(
                                        conf.getEnterpriseUserConf().getManager().getDisplayName()).orElse(null);
                            }
                            if (displayName != null) {
                                manager.setDisplayName(displayName.getValues().get(0));
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Could not read attribute {}",
                                conf.getEnterpriseUserConf().getManager().getManager(), e);
                    }
                }

                if (!manager.isEmpty()) {
                    enterpriseInfo.setManager(manager);
                }
            }

            if (!enterpriseInfo.isEmpty()) {
                user.setEnterpriseInfo(enterpriseInfo);
            }
        }

        userTO.getMemberships().forEach(membership -> {
            user.getGroups().add(new Group(
                    membership.getGroupKey(),
                    StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                    membership.getGroupName(),
                    Function.direct));
        });
        userTO.getDynMemberships().forEach(membership -> {
            user.getGroups().add(new Group(
                    membership.getGroupKey(),
                    StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                    membership.getGroupName(),
                    Function.indirect));
        });

        authDataAccessor.getAuthorities(userTO.getUsername()).forEach(authority -> {
            user.getEntitlements().add(new Value(authority.getAuthority() + " on Realm(s) " + authority.getRealms()));
        });

        userTO.getRoles().forEach(role -> {
            user.getRoles().add(new Value(role));
        });

        return user;
    }

    public SCIMGroup toSCIMGroup(final GroupTO groupTO, final String location) {
        SCIMGroup group = new SCIMGroup(
                groupTO.getKey(),
                new Meta(
                        Resource.Group,
                        groupTO.getCreationDate(),
                        groupTO.getLastChangeDate() == null
                        ? groupTO.getCreationDate() : groupTO.getLastChangeDate(),
                        groupTO.getETagValue(),
                        location),
                groupTO.getName());

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(groupTO.getKey());
        SearchCond searchCond = SearchCond.getLeafCond(membCond);

        int count = userLogic.
                search(searchCond, 1, 1, Collections.<OrderByClause>emptyList(), SyncopeConstants.ROOT_REALM, false).
                getLeft();

        for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
            List<UserTO> users = userLogic.search(
                    searchCond,
                    page,
                    AnyDAO.DEFAULT_PAGE_SIZE,
                    Collections.<OrderByClause>emptyList(),
                    SyncopeConstants.ROOT_REALM,
                    false).
                    getRight();
            users.forEach(userTO -> {
                group.getMembers().add(new Member(
                        userTO.getKey(),
                        StringUtils.substringBefore(location, "/Groups") + "/Users/" + userTO.getKey(),
                        userTO.getUsername(),
                        Resource.User));
            });
        }

        return group;
    }
}
