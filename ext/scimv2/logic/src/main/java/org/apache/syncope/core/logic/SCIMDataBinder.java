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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
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
import org.apache.syncope.ext.scimv2.api.data.Value;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Function;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMDataBinder.class);

    protected static final List<String> USER_SCHEMAS = List.of(Resource.User.schema());

    protected static final List<String> ENTERPRISE_USER_SCHEMAS =
            List.of(Resource.User.schema(), Resource.EnterpriseUser.schema());

    protected static final List<String> GROUP_SCHEMAS = List.of(Resource.Group.schema());

    protected final SCIMConfManager confManager;

    protected final UserLogic userLogic;

    protected final AuthDataAccessor authDataAccessor;

    public SCIMDataBinder(
            final SCIMConfManager confManager,
            final UserLogic userLogic,
            final AuthDataAccessor authDataAccessor) {

        this.confManager = confManager;
        this.userLogic = userLogic;
        this.authDataAccessor = authDataAccessor;
    }

    protected <E extends Enum<?>> void fill(
            final Map<String, Attr> attrs,
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

    protected boolean output(
            final List<String> attributes,
            final List<String> excludedAttributes,
            final String schema) {

        return (attributes.isEmpty() || attributes.contains(schema))
                && (excludedAttributes.isEmpty() || !excludedAttributes.contains(schema));
    }

    protected <T> T output(
            final List<String> attributes,
            final List<String> excludedAttributes,
            final String schema,
            final T value) {

        return output(attributes, excludedAttributes, schema)
                ? value
                : null;
    }

    public SCIMUser toSCIMUser(
            final UserTO userTO,
            final String location,
            final List<String> attributes,
            final List<String> excludedAttributes) {

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
                output(attributes, excludedAttributes, "userName", userTO.getUsername()),
                !userTO.isSuspended());

        Map<String, Attr> attrs = new HashMap<>();
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getPlainAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getDerAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(userTO.getVirAttrs()));
        attrs.put("username", new Attr.Builder("username").value(userTO.getUsername()).build());

        if (conf.getUserConf() != null) {
            if (output(attributes, excludedAttributes, "name") && conf.getUserConf().getName() != null) {
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

            if (output(attributes, excludedAttributes, "displayName")
                    && conf.getUserConf().getDisplayName() != null
                    && attrs.containsKey(conf.getUserConf().getDisplayName())) {

                user.setDisplayName(attrs.get(conf.getUserConf().getDisplayName()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "nickName")
                    && conf.getUserConf().getNickName() != null
                    && attrs.containsKey(conf.getUserConf().getNickName())) {

                user.setNickName(attrs.get(conf.getUserConf().getNickName()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "profileUrl")
                    && conf.getUserConf().getProfileUrl() != null
                    && attrs.containsKey(conf.getUserConf().getProfileUrl())) {

                user.setProfileUrl(attrs.get(conf.getUserConf().getProfileUrl()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "title")
                    && conf.getUserConf().getTitle() != null
                    && attrs.containsKey(conf.getUserConf().getTitle())) {

                user.setTitle(attrs.get(conf.getUserConf().getTitle()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "userType")
                    && conf.getUserConf().getUserType() != null
                    && attrs.containsKey(conf.getUserConf().getUserType())) {

                user.setUserType(attrs.get(conf.getUserConf().getUserType()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "preferredLanguage")
                    && conf.getUserConf().getPreferredLanguage() != null
                    && attrs.containsKey(conf.getUserConf().getPreferredLanguage())) {

                user.setPreferredLanguage(attrs.get(conf.getUserConf().getPreferredLanguage()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "locale")
                    && conf.getUserConf().getLocale() != null
                    && attrs.containsKey(conf.getUserConf().getLocale())) {

                user.setLocale(attrs.get(conf.getUserConf().getLocale()).getValues().get(0));
            }
            if (output(attributes, excludedAttributes, "timezone")
                    && conf.getUserConf().getTimezone() != null
                    && attrs.containsKey(conf.getUserConf().getTimezone())) {

                user.setTimezone(attrs.get(conf.getUserConf().getTimezone()).getValues().get(0));
            }

            if (output(attributes, excludedAttributes, "emails")) {
                fill(attrs, conf.getUserConf().getEmails(), user.getEmails());
            }
            if (output(attributes, excludedAttributes, "phoneNumbers")) {
                fill(attrs, conf.getUserConf().getPhoneNumbers(), user.getPhoneNumbers());
            }
            if (output(attributes, excludedAttributes, "ims")) {
                fill(attrs, conf.getUserConf().getIms(), user.getIms());
            }
            if (output(attributes, excludedAttributes, "photos")) {
                fill(attrs, conf.getUserConf().getPhotos(), user.getPhotos());
            }
            if (output(attributes, excludedAttributes, "addresses")) {
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
            }
            if (output(attributes, excludedAttributes, "x509Certificates")) {
                conf.getUserConf().getX509Certificates().stream()
                        .filter(attrs::containsKey)
                        .forEachOrdered(certificate -> user.getX509Certificates().add(
                        new Value(attrs.get(certificate).getValues().get(0))));
            }

            if (conf.getEnterpriseUserConf() != null) {
                SCIMEnterpriseInfo enterpriseInfo = new SCIMEnterpriseInfo();

                if (output(attributes, excludedAttributes, "employeeNumber")
                        && conf.getEnterpriseUserConf().getEmployeeNumber() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getEmployeeNumber())) {

                    enterpriseInfo.setEmployeeNumber(
                            attrs.get(conf.getEnterpriseUserConf().getEmployeeNumber()).getValues().get(0));
                }
                if (output(attributes, excludedAttributes, "costCenter")
                        && conf.getEnterpriseUserConf().getCostCenter() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getCostCenter())) {

                    enterpriseInfo.setCostCenter(
                            attrs.get(conf.getEnterpriseUserConf().getCostCenter()).getValues().get(0));
                }
                if (output(attributes, excludedAttributes, "organization")
                        && conf.getEnterpriseUserConf().getOrganization() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getOrganization())) {

                    enterpriseInfo.setOrganization(
                            attrs.get(conf.getEnterpriseUserConf().getOrganization()).getValues().get(0));
                }
                if (output(attributes, excludedAttributes, "division")
                        && conf.getEnterpriseUserConf().getDivision() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getDivision())) {

                    enterpriseInfo.setDivision(
                            attrs.get(conf.getEnterpriseUserConf().getDivision()).getValues().get(0));
                }
                if (output(attributes, excludedAttributes, "department")
                        && conf.getEnterpriseUserConf().getDepartment() != null
                        && attrs.containsKey(conf.getEnterpriseUserConf().getDepartment())) {

                    enterpriseInfo.setDepartment(
                            attrs.get(conf.getEnterpriseUserConf().getDepartment()).getValues().get(0));
                }
                if (output(attributes, excludedAttributes, "manager")
                        && conf.getEnterpriseUserConf().getManager() != null) {

                    SCIMUserManager manager = new SCIMUserManager();

                    if (conf.getEnterpriseUserConf().getManager().getKey() != null
                            && attrs.containsKey(conf.getEnterpriseUserConf().getManager().getKey())) {

                        try {
                            UserTO userManager = userLogic.read(attrs.get(
                                    conf.getEnterpriseUserConf().getManager().getKey()).getValues().get(0));
                            manager.setValue(userManager.getKey());
                            manager.setRef(
                                    StringUtils.substringBefore(location, "/Users") + "/Users/" + userManager.getKey());

                            if (conf.getEnterpriseUserConf().getManager().getDisplayName() != null) {
                                Attr displayName = userManager.getPlainAttr(
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
                            LOG.error("Could not read user {}", conf.getEnterpriseUserConf().getManager().getKey(), e);
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

            if (output(attributes, excludedAttributes, "groups")) {
                userTO.getMemberships().forEach(membership -> user.getGroups().add(new Group(
                        membership.getGroupKey(),
                        StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                        membership.getGroupName(),
                        Function.direct)));
                userTO.getDynMemberships().forEach(membership -> user.getGroups().add(new Group(
                        membership.getGroupKey(),
                        StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                        membership.getGroupName(),
                        Function.indirect)));
            }

            if (output(attributes, excludedAttributes, "entitlements")) {
                authDataAccessor.getAuthorities(userTO.getUsername(), null).forEach(authority -> user.getEntitlements().
                        add(new Value(authority.getAuthority() + " on Realm(s) " + authority.getRealms())));
            }

            if (output(attributes, excludedAttributes, "roles")) {
                userTO.getRoles().forEach(role -> user.getRoles().add(new Value(role)));
            }
        }

        return user;
    }

    protected <E extends Enum<?>> void fill(
            final Set<Attr> attrs,
            final List<SCIMComplexConf<E>> confs,
            final List<SCIMComplexValue> values) {

        values.stream().filter(value -> value.getType() != null).forEach(value -> confs.stream().
                filter(object -> value.getType().equals(object.getType().name())).findFirst().
                ifPresent(conf -> attrs.add(
                new Attr.Builder(conf.getValue()).value(value.getValue()).build())));
    }

    public UserTO toUserTO(final SCIMUser user) {
        if (!USER_SCHEMAS.equals(user.getSchemas()) && !ENTERPRISE_USER_SCHEMAS.equals(user.getSchemas())) {
            throw new BadRequestException(ErrorType.invalidValue);
        }

        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setKey(user.getId());
        userTO.setPassword(user.getPassword());
        userTO.setUsername(user.getUserName());

        SCIMConf conf = confManager.get();

        if (conf.getUserConf() != null) {
            if (conf.getUserConf().getName() != null && user.getName() != null) {
                if (conf.getUserConf().getName().getFamilyName() != null
                        && user.getName().getFamilyName() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getFamilyName(),
                            user.getName().getFamilyName());
                }
                if (conf.getUserConf().getName().getFormatted() != null
                        && user.getName().getFormatted() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getFormatted(),
                            user.getName().getFormatted());
                }
                if (conf.getUserConf().getName().getGivenName() != null
                        && user.getName().getGivenName() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getGivenName(),
                            user.getName().getGivenName());
                }
                if (conf.getUserConf().getName().getHonorificPrefix() != null
                        && user.getName().getHonorificPrefix() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getHonorificPrefix(),
                            user.getName().getHonorificPrefix());
                }
                if (conf.getUserConf().getName().getHonorificSuffix() != null
                        && user.getName().getHonorificSuffix() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getHonorificSuffix(),
                            user.getName().getHonorificSuffix());
                }
                if (conf.getUserConf().getName().getMiddleName() != null
                        && user.getName().getMiddleName() != null) {

                    setAttribute(userTO, conf.getUserConf().getName().getMiddleName(),
                            user.getName().getMiddleName());
                }
            }

            if (conf.getUserConf().getDisplayName() != null && user.getDisplayName() != null) {
                setAttribute(userTO, conf.getUserConf().getDisplayName(), user.getDisplayName());
            }
            if (conf.getUserConf().getNickName() != null && user.getNickName() != null) {
                setAttribute(userTO, conf.getUserConf().getNickName(), user.getNickName());
            }
            if (conf.getUserConf().getProfileUrl() != null && user.getProfileUrl() != null) {
                setAttribute(userTO, conf.getUserConf().getProfileUrl(), user.getProfileUrl());
            }
            if (conf.getUserConf().getTitle() != null && user.getTitle() != null) {
                setAttribute(userTO, conf.getUserConf().getTitle(),
                        user.getTitle());
            }
            if (conf.getUserConf().getUserType() != null && user.getUserType() != null) {
                setAttribute(userTO, conf.getUserConf().getUserType(), user.getUserType());
            }
            if (conf.getUserConf().getPreferredLanguage() != null && user.getPreferredLanguage() != null) {
                setAttribute(userTO, conf.getUserConf().getPreferredLanguage(), user.getPreferredLanguage());
            }
            if (conf.getUserConf().getLocale() != null && user.getLocale() != null) {
                setAttribute(userTO, conf.getUserConf().getLocale(), user.getLocale());
            }
            if (conf.getUserConf().getTimezone() != null && user.getTimezone() != null) {
                setAttribute(userTO, conf.getUserConf().getTimezone(), user.getTimezone());
            }

            fill(userTO.getPlainAttrs(), conf.getUserConf().getEmails(), user.getEmails());
            fill(userTO.getPlainAttrs(), conf.getUserConf().getPhoneNumbers(), user.getPhoneNumbers());
            fill(userTO.getPlainAttrs(), conf.getUserConf().getIms(), user.getIms());
            fill(userTO.getPlainAttrs(), conf.getUserConf().getPhotos(), user.getPhotos());

            user.getAddresses().stream().filter(address -> address.getType() != null).
                    forEach(address -> conf.getUserConf().getAddresses().stream().
                    filter(object -> address.getType().equals(object.getType().name())).findFirst().
                    ifPresent(addressConf -> {
                        if (addressConf.getFormatted() != null && address.getFormatted() != null) {
                            setAttribute(userTO, addressConf.getFormatted(), address.getFormatted());
                        }
                        if (addressConf.getStreetAddress() != null && address.getStreetAddress() != null) {
                            setAttribute(userTO, addressConf.getStreetAddress(), address.getStreetAddress());
                        }
                        if (addressConf.getLocality() != null && address.getLocality() != null) {
                            setAttribute(userTO, addressConf.getLocality(), address.getLocality());
                        }
                        if (addressConf.getRegion() != null && address.getRegion() != null) {
                            setAttribute(userTO, addressConf.getRegion(), address.getRegion());
                        }
                        if (addressConf.getPostalCode() != null && address.getPostalCode() != null) {
                            setAttribute(userTO, addressConf.getPostalCode(), address.getPostalCode());
                        }
                        if (addressConf.getCountry() != null && address.getCountry() != null) {
                            setAttribute(userTO, addressConf.getCountry(), address.getCountry());
                        }
                    }));

            for (int i = 0; i < user.getX509Certificates().size(); i++) {
                Value certificate = user.getX509Certificates().get(i);
                if (conf.getUserConf().getX509Certificates().size() > i) {
                    setAttribute(userTO, conf.getUserConf().getX509Certificates().get(i), certificate.getValue());
                }
            }
        }

        if (conf.getEnterpriseUserConf() != null) {
            if (conf.getEnterpriseUserConf().getEmployeeNumber() != null
                    && user.getEnterpriseInfo().getEmployeeNumber() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getEmployeeNumber(),
                        user.getEnterpriseInfo().getEmployeeNumber());
            }
            if (conf.getEnterpriseUserConf().getCostCenter() != null
                    && user.getEnterpriseInfo().getCostCenter() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getCostCenter(),
                        user.getEnterpriseInfo().getCostCenter());
            }
            if (conf.getEnterpriseUserConf().getOrganization() != null
                    && user.getEnterpriseInfo().getOrganization() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getOrganization(),
                        user.getEnterpriseInfo().getOrganization());
            }
            if (conf.getEnterpriseUserConf().getDivision() != null
                    && user.getEnterpriseInfo().getDivision() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getDivision(),
                        user.getEnterpriseInfo().getDivision());
            }
            if (conf.getEnterpriseUserConf().getDepartment() != null
                    && user.getEnterpriseInfo().getDepartment() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getDepartment(),
                        user.getEnterpriseInfo().getDepartment());
            }
            if (conf.getEnterpriseUserConf().getManager() != null
                    && conf.getEnterpriseUserConf().getManager().getKey() != null
                    && user.getEnterpriseInfo().getManager() != null
                    && user.getEnterpriseInfo().getManager().getValue() != null) {

                setAttribute(userTO, conf.getEnterpriseUserConf().getManager().getKey(),
                        user.getEnterpriseInfo().getManager().getValue());
            }
        }

        userTO.getMemberships().addAll(user.getGroups().stream().
                map(group -> new MembershipTO.Builder(group.getValue()).build()).
                collect(Collectors.toList()));

        userTO.getRoles().addAll(user.getRoles().stream().
                map(Value::getValue).
                collect(Collectors.toList()));

        return userTO;
    }

    public UserCR toUserCR(final SCIMUser user) {
        UserTO userTO = toUserTO(user);
        UserCR userCR = new UserCR();
        EntityTOUtils.toAnyCR(userTO, userCR);
        return userCR;
    }

    protected void setAttribute(final UserTO userTO, final String schema, final String value) {
        switch (schema) {
            case "username":
                userTO.setUsername(value);
                break;

            default:
                userTO.getPlainAttrs().add(new Attr.Builder(schema).value(value).build());
        }
    }

    public SCIMGroup toSCIMGroup(
            final GroupTO groupTO,
            final String location,
            final List<String> attributes,
            final List<String> excludedAttributes) {

        SCIMGroup group = new SCIMGroup(
                groupTO.getKey(),
                new Meta(
                        Resource.Group,
                        groupTO.getCreationDate(),
                        groupTO.getLastChangeDate() == null
                        ? groupTO.getCreationDate() : groupTO.getLastChangeDate(),
                        groupTO.getETagValue(),
                        location),
                output(attributes, excludedAttributes, "displayName", groupTO.getName()));

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(groupTO.getKey());
        SearchCond searchCond = SearchCond.getLeaf(membCond);

        if (output(attributes, excludedAttributes, "members")) {
            int count = userLogic.search(searchCond,
                    1, 1, List.of(),
                    SyncopeConstants.ROOT_REALM, false).getLeft();

            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                List<UserTO> users = userLogic.search(
                        searchCond,
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE,
                        List.of(),
                        SyncopeConstants.ROOT_REALM,
                        false).
                        getRight();
                users.forEach(userTO -> group.getMembers().add(new Member(
                        userTO.getKey(),
                        StringUtils.substringBefore(location, "/Groups") + "/Users/" + userTO.getKey(),
                        userTO.getUsername())));
            }
        }

        return group;
    }

    public static GroupTO toGroupTO(final SCIMGroup group) {
        if (!GROUP_SCHEMAS.equals(group.getSchemas())) {
            throw new BadRequestException(ErrorType.invalidValue);
        }

        GroupTO groupTO = new GroupTO();
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        groupTO.setKey(group.getId());
        groupTO.setName(group.getDisplayName());
        return groupTO;
    }

    public static GroupCR toGroupCR(final SCIMGroup group) {
        if (!GROUP_SCHEMAS.equals(group.getSchemas())) {
            throw new BadRequestException(ErrorType.invalidValue);
        }

        return new GroupCR.Builder(SyncopeConstants.ROOT_REALM, group.getDisplayName()).build();
    }
}
