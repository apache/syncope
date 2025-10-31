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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.StringPatchItem;
import org.apache.syncope.common.lib.request.StringReplacePatchItem;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMEnterpriseUserConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyObjectConf;
import org.apache.syncope.common.lib.scim.SCIMManagerConf;
import org.apache.syncope.common.lib.scim.SCIMUserAddressConf;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.Group;
import org.apache.syncope.ext.scimv2.api.data.Member;
import org.apache.syncope.ext.scimv2.api.data.Meta;
import org.apache.syncope.ext.scimv2.api.data.SCIMAnyObject;
import org.apache.syncope.ext.scimv2.api.data.SCIMComplexValue;
import org.apache.syncope.ext.scimv2.api.data.SCIMEnterpriseInfo;
import org.apache.syncope.ext.scimv2.api.data.SCIMExtensionInfo;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOp;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOperation;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserAddress;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserManager;
import org.apache.syncope.ext.scimv2.api.data.SCIMUserName;
import org.apache.syncope.ext.scimv2.api.data.Value;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Function;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class SCIMDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMDataBinder.class);

    /**
     * Translates the given SCIM filter into the equivalent JEXL expression.
     *
     * @param filter SCIM filter according to https://www.rfc-editor.org/rfc/rfc7644#section-3.4.2.2
     * @return translated JEXL expression; see https://commons.apache.org/proper/commons-jexl/reference/syntax.html
     * */
    public static String filter2JexlExpression(final String filter) {
        String jexlExpression = filter.
                replace(" co ", " =~ ").
                replace(" sw ", " =^ ").
                replace(" ew ", " =$ ");

        boolean endsWithPR = jexlExpression.endsWith(" pr");
        int pr = endsWithPR ? jexlExpression.indexOf(" pr") : jexlExpression.indexOf(" pr ");
        while (pr != -1) {
            String before = jexlExpression.substring(0, pr);
            int start = before.indexOf(' ') == -1 ? 0 : jexlExpression.substring(0, pr).lastIndexOf(' ', pr) + 1;
            String literal = jexlExpression.substring(start, pr);

            endsWithPR = jexlExpression.endsWith(" pr");
            jexlExpression = jexlExpression.replace(
                    literal + " pr" + (endsWithPR ? "" : " "),
                    "not(empty(" + literal + "))" + (endsWithPR ? "" : " "));

            pr = endsWithPR ? jexlExpression.indexOf(" pr") : jexlExpression.indexOf(" pr ");
        }

        return jexlExpression;
    }

    protected final SCIMConfManager confManager;

    protected final UserLogic userLogic;

    protected final AuthDataAccessor authDataAccessor;

    protected final GroupDAO groupDAO;

    public SCIMDataBinder(
            final SCIMConfManager confManager,
            final UserLogic userLogic,
            final AuthDataAccessor authDataAccessor,
            final GroupDAO groupDAO) {

        this.confManager = confManager;
        this.userLogic = userLogic;
        this.authDataAccessor = authDataAccessor;
        this.groupDAO = groupDAO;
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

            value.setPrimary(conf.isPrimary());

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
        if (conf.getExtensionUserConf() != null) {
            schemas.add(Resource.ExtensionUser.schema());
        }

        SCIMUser user = new SCIMUser(
                userTO.getKey(),
                schemas,
                new Meta(
                        Resource.User.name(),
                        userTO.getCreationDate(),
                        Optional.ofNullable(userTO.getLastChangeDate()).orElse(userTO.getCreationDate()),
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
            if (output(attributes, excludedAttributes, "externalId")
                    && conf.getUserConf().getExternalId() != null
                    && attrs.containsKey(conf.getUserConf().getExternalId())) {

                user.setExternalId(attrs.get(conf.getUserConf().getExternalId()).getValues().get(0));
            }

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
                conf.getUserConf().getX509Certificates().stream().filter(attrs::containsKey).
                        forEach(cert -> user.getX509Certificates().add(new Value(attrs.get(cert).getValues().get(0))));
            }
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

        if (conf.getExtensionUserConf() != null) {
            SCIMExtensionInfo extensionInfo = new SCIMExtensionInfo();
            conf.getExtensionUserConf().asMap().forEach((scimAttr, syncopeAttr) -> {
                if (output(attributes, excludedAttributes, scimAttr) && attrs.containsKey(syncopeAttr)) {
                    extensionInfo.getAttributes().put(scimAttr, attrs.get(syncopeAttr).getValues().get(0));
                }
            });

            if (!extensionInfo.isEmpty()) {
                user.setExtensionInfo(extensionInfo);
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

        return user;
    }

    protected void setAttribute(
            final UserTO userTO,
            final String schema,
            final String value) {

        if (schema == null || value == null) {
            return;
        }

        switch (schema) {
            case "username":
                userTO.setUsername(value);
                break;

            default:
                userTO.getPlainAttrs().add(new Attr.Builder(schema).value(value).build());
        }
    }

    protected void setAttribute(
            final GroupTO groupTO,
            final String schema,
            final String value) {

        if (schema == null || value == null) {
            return;
        }

        switch (schema) {
            case "name":
                groupTO.setName(value);
                break;

            default:
                groupTO.getPlainAttrs().add(new Attr.Builder(schema).value(value).build());
        }
    }

    protected void setAttribute(
            final AnyObjectTO anyObjectTO,
            final String schema,
            final String value) {

        if (schema == null || value == null) {
            return;
        }

        if ("name".equals(schema)) {
            anyObjectTO.setName(value);
        } else {
            anyObjectTO.getPlainAttrs().add(new Attr.Builder(schema).value(value).build());
        }
    }

    protected <E extends Enum<?>> void setAttribute(
            final Set<Attr> attrs,
            final List<SCIMComplexConf<E>> confs,
            final List<SCIMComplexValue> values) {

        values.stream().filter(value -> value.getType() != null).forEach(value -> confs.stream().
                filter(object -> value.getType().equals(object.getType().name())).findFirst().
                ifPresent(conf -> attrs.add(
                new Attr.Builder(conf.getValue()).value(value.getValue()).build())));
    }

    protected <E extends Enum<?>> void setAttribute(
            final Set<Attr> attrs,
            final Set<AttrPatch> attrPatches,
            final List<SCIMComplexConf<E>> confs,
            final List<SCIMComplexValue> values) {

        values.stream().filter(value -> value.getType() != null).forEach(value -> confs.stream().
                filter(object -> value.getType().equals(object.getType().name())
                && attrPatches.stream().noneMatch(attrPatch ->
                attrPatch.getAttr().getSchema().equals(object.getValue()))).findFirst().
                ifPresent(conf -> attrs.add(
                new Attr.Builder(conf.getValue()).value(value.getValue()).build())));
    }
    
    public void addValues(
            final UserUR userUR,
            final UserTO before,
            final SCIMUser user,
            final Collection<String> resources,
            final SCIMPatchOperation op) {
        SCIMConf conf = confManager.get();

        if (!SyncopeConstants.ROOT_REALM.equals(before.getRealm())) {
            userUR.setRealm(new StringReplacePatchItem.Builder()
                    .value(SyncopeConstants.ROOT_REALM).operation(PatchOperation.ADD_REPLACE).build());
        }

        if (StringUtils.isNotBlank(user.getPassword())) {
            userUR.setPassword(new PasswordPatch.Builder()
                    .value(user.getPassword()).resources(resources).operation(PatchOperation.ADD_REPLACE).build());
        }

        if (StringUtils.isNotBlank(user.getUserName()) && !user.getUserName().equals(before.getUsername())) {
            userUR.setUsername(new StringReplacePatchItem.Builder()
                    .value(user.getUserName()).operation(PatchOperation.ADD_REPLACE).build());
        }

        if (conf.getUserConf() != null) {
            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getExternalId(),
                    user.getExternalId(),
                    op);

            if (conf.getUserConf().getName() != null && user.getName() != null) {
                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getFamilyName(),
                        user.getName().getFamilyName(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getFormatted(),
                        user.getName().getFormatted(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getGivenName(),
                        user.getName().getGivenName(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getHonorificPrefix(),
                        user.getName().getHonorificPrefix(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getHonorificSuffix(),
                        user.getName().getHonorificSuffix(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        conf.getUserConf().getName().getMiddleName(),
                        user.getName().getMiddleName(),
                        op);
            }

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getDisplayName(),
                    user.getDisplayName(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getNickName(),
                    user.getNickName(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getProfileUrl(),
                    user.getProfileUrl(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getTitle(),
                    user.getTitle(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getUserType(),
                    user.getUserType(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getPreferredLanguage(),
                    user.getPreferredLanguage(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getLocale(),
                    user.getLocale(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getUserConf().getTimezone(),
                    user.getTimezone(),
                    op);

            setAttribute(
                    before.getPlainAttrs(), userUR.getPlainAttrs(), conf.getUserConf().getEmails(), user.getEmails());
            setAttribute(
                    before.getPlainAttrs(),
                    userUR.getPlainAttrs(),
                    conf.getUserConf().getPhoneNumbers(),
                    user.getPhoneNumbers());
            setAttribute(before.getPlainAttrs(), userUR.getPlainAttrs(), conf.getUserConf().getIms(), user.getIms());
            setAttribute(
                    before.getPlainAttrs(), userUR.getPlainAttrs(), conf.getUserConf().getPhotos(), user.getPhotos());

            user.getAddresses().stream().filter(address -> address.getType() != null).
                    forEach(address -> conf.getUserConf().getAddresses().stream().
                    filter(object -> address.getType().equals(object.getType().name())).findFirst().
                    ifPresent(addressConf -> {
                setAttribute(
                        before,
                        userUR,
                        addressConf.getFormatted(),
                        address.getFormatted(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        addressConf.getStreetAddress(),
                        address.getStreetAddress(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        addressConf.getLocality(),
                        address.getLocality(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        addressConf.getRegion(),
                        address.getRegion(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        addressConf.getPostalCode(),
                        address.getPostalCode(),
                        op);

                setAttribute(
                        before,
                        userUR,
                        addressConf.getCountry(),
                        address.getCountry(),
                        op);
            }));

            for (int i = 0; i < user.getX509Certificates().size(); i++) {
                Value certificate = user.getX509Certificates().get(i);
                if (conf.getUserConf().getX509Certificates().size() > i) {
                    setAttribute(
                            before,
                            userUR,
                            conf.getUserConf().getX509Certificates().get(i),
                            certificate.getValue(),
                            op);
                }
            }
        }

        if (conf.getEnterpriseUserConf() != null && user.getEnterpriseInfo() != null) {
            setAttribute(
                    before,
                    userUR,
                    conf.getEnterpriseUserConf().getEmployeeNumber(),
                    user.getEnterpriseInfo().getEmployeeNumber(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getEnterpriseUserConf().getCostCenter(),
                    user.getEnterpriseInfo().getCostCenter(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getEnterpriseUserConf().getOrganization(),
                    user.getEnterpriseInfo().getOrganization(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getEnterpriseUserConf().getDivision(),
                    user.getEnterpriseInfo().getDivision(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    conf.getEnterpriseUserConf().getDepartment(),
                    user.getEnterpriseInfo().getDepartment(),
                    op);

            setAttribute(
                    before,
                    userUR,
                    Optional.ofNullable(conf.getEnterpriseUserConf().getManager()).
                            map(SCIMManagerConf::getKey).orElse(null),
                    Optional.ofNullable(user.getEnterpriseInfo().getManager()).
                            map(SCIMUserManager::getValue).orElse(null),
                    op);
        }

        if (conf.getExtensionUserConf() != null && user.getExtensionInfo() != null) {
            conf.getExtensionUserConf().asMap().forEach((scimAttr, syncopeAttr) -> setAttribute(
                    before, userUR, syncopeAttr, user.getExtensionInfo().getAttributes().get(scimAttr), op));
        }

        user.getGroups().forEach(group -> {
            if (before.getMembership(group.getValue()).isEmpty()
                    && userUR.getMemberships().stream().noneMatch(membershipUR ->
                    membershipUR.getGroup().equals(group.getValue()))) {
                userUR.getMemberships().add(new MembershipUR.Builder(group.getValue())
                        .operation(PatchOperation.ADD_REPLACE).build());
            }
        });

        user.getRoles().forEach(role -> {
            if (before.getRoles().contains(role.getValue())
                    && userUR.getRoles().stream().noneMatch(roleUR ->
                    roleUR.getValue().equals(role.getValue()))) {
                userUR.getRoles().add(new StringPatchItem.Builder()
                        .value(role.getValue()).operation(PatchOperation.ADD_REPLACE).build());
            }
        });
    }

    public UserTO toUserTO(final SCIMUser user, final boolean checkSchemas) {
        SCIMConf conf = confManager.get();

        Set<String> expectedSchemas = new HashSet<>();
        expectedSchemas.add(Resource.User.schema());
        if (conf.getEnterpriseUserConf() != null) {
            expectedSchemas.add(Resource.EnterpriseUser.schema());
        }
        if (conf.getExtensionUserConf() != null) {
            expectedSchemas.add(Resource.ExtensionUser.schema());
        }
        if (checkSchemas
                && (!user.getSchemas().containsAll(expectedSchemas)
                || !expectedSchemas.containsAll(user.getSchemas()))) {

            throw new BadRequestException(ErrorType.invalidValue);
        }

        UserTO userTO = new UserTO();
        userTO.setRealm(SyncopeConstants.ROOT_REALM);
        userTO.setKey(user.getId());
        userTO.setPassword(user.getPassword());
        userTO.setUsername(user.getUserName());

        if (conf.getUserConf() != null) {
            setAttribute(
                    userTO,
                    conf.getUserConf().getExternalId(),
                    user.getExternalId());

            if (conf.getUserConf().getName() != null && user.getName() != null) {
                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getFamilyName(),
                        user.getName().getFamilyName());

                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getFormatted(),
                        user.getName().getFormatted());

                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getGivenName(),
                        user.getName().getGivenName());

                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getHonorificPrefix(),
                        user.getName().getHonorificPrefix());

                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getHonorificSuffix(),
                        user.getName().getHonorificSuffix());

                setAttribute(
                        userTO,
                        conf.getUserConf().getName().getMiddleName(),
                        user.getName().getMiddleName());
            }

            setAttribute(
                    userTO,
                    conf.getUserConf().getDisplayName(),
                    user.getDisplayName());

            setAttribute(
                    userTO,
                    conf.getUserConf().getNickName(),
                    user.getNickName());

            setAttribute(
                    userTO,
                    conf.getUserConf().getProfileUrl(),
                    user.getProfileUrl());

            setAttribute(
                    userTO,
                    conf.getUserConf().getTitle(),
                    user.getTitle());

            setAttribute(
                    userTO,
                    conf.getUserConf().getUserType(),
                    user.getUserType());

            setAttribute(
                    userTO,
                    conf.getUserConf().getPreferredLanguage(),
                    user.getPreferredLanguage());

            setAttribute(
                    userTO,
                    conf.getUserConf().getLocale(),
                    user.getLocale());

            setAttribute(
                    userTO,
                    conf.getUserConf().getTimezone(),
                    user.getTimezone());

            setAttribute(userTO.getPlainAttrs(), conf.getUserConf().getEmails(), user.getEmails());
            setAttribute(userTO.getPlainAttrs(), conf.getUserConf().getPhoneNumbers(), user.getPhoneNumbers());
            setAttribute(userTO.getPlainAttrs(), conf.getUserConf().getIms(), user.getIms());
            setAttribute(userTO.getPlainAttrs(), conf.getUserConf().getPhotos(), user.getPhotos());

            user.getAddresses().stream().filter(address -> address.getType() != null).
                    forEach(address -> conf.getUserConf().getAddresses().stream().
                    filter(object -> address.getType().equals(object.getType().name())).findFirst().
                    ifPresent(addressConf -> {
                        setAttribute(
                                userTO,
                                addressConf.getFormatted(),
                                address.getFormatted());

                        setAttribute(
                                userTO,
                                addressConf.getStreetAddress(),
                                address.getStreetAddress());

                        setAttribute(
                                userTO,
                                addressConf.getLocality(),
                                address.getLocality());

                        setAttribute(
                                userTO,
                                addressConf.getRegion(),
                                address.getRegion());

                        setAttribute(
                                userTO,
                                addressConf.getPostalCode(),
                                address.getPostalCode());

                        setAttribute(
                                userTO,
                                addressConf.getCountry(),
                                address.getCountry());
                    }));

            for (int i = 0; i < user.getX509Certificates().size(); i++) {
                Value certificate = user.getX509Certificates().get(i);
                if (conf.getUserConf().getX509Certificates().size() > i) {
                    setAttribute(
                            userTO,
                            conf.getUserConf().getX509Certificates().get(i),
                            certificate.getValue());
                }
            }
        }

        if (conf.getEnterpriseUserConf() != null && user.getEnterpriseInfo() != null) {
            setAttribute(
                    userTO,
                    conf.getEnterpriseUserConf().getEmployeeNumber(),
                    user.getEnterpriseInfo().getEmployeeNumber());

            setAttribute(
                    userTO,
                    conf.getEnterpriseUserConf().getCostCenter(),
                    user.getEnterpriseInfo().getCostCenter());

            setAttribute(
                    userTO,
                    conf.getEnterpriseUserConf().getOrganization(),
                    user.getEnterpriseInfo().getOrganization());

            setAttribute(
                    userTO,
                    conf.getEnterpriseUserConf().getDivision(),
                    user.getEnterpriseInfo().getDivision());

            setAttribute(
                    userTO,
                    conf.getEnterpriseUserConf().getDepartment(),
                    user.getEnterpriseInfo().getDepartment());

            setAttribute(
                    userTO,
                    Optional.ofNullable(conf.getEnterpriseUserConf().getManager()).
                            map(SCIMManagerConf::getKey).orElse(null),
                    Optional.ofNullable(user.getEnterpriseInfo().getManager()).
                            map(SCIMUserManager::getValue).orElse(null));
        }

        if (conf.getExtensionUserConf() != null && user.getExtensionInfo() != null) {
            conf.getExtensionUserConf().asMap().forEach((scimAttr, syncopeAttr) -> setAttribute(
                    userTO, syncopeAttr, user.getExtensionInfo().getAttributes().get(scimAttr)));
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
        UserTO userTO = toUserTO(user, true);
        UserCR userCR = new UserCR();
        EntityTOUtils.toAnyCR(userTO, userCR);
        return userCR;
    }

    protected void setAttribute(
            final UserTO before,
            final UserUR userUR,
            final String schema,
            final String value,
            final SCIMPatchOperation op) {
        if (schema == null || value == null) {
            return;
        }
        switch (schema) {
            case "username":
                if (!value.equals(before.getUsername()) && userUR.getUsername() == null) {
                    userUR.setUsername(new StringReplacePatchItem.Builder()
                            .value(value).operation(PatchOperation.ADD_REPLACE).build());
                }
                break;

            default:
                if ((before.getPlainAttr(schema).isEmpty()
                        || !value.equals(before.getPlainAttr(schema).get().getValues().get(0)))
                        && userUR.getPlainAttrs().stream().noneMatch(attrPatch ->
                        attrPatch.getAttr().getSchema().equals(schema))
                        && op.getOp() != PatchOp.remove) {
                    userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder(schema).value(value).build())
                            .operation(PatchOperation.ADD_REPLACE)
                            .build());
                }
                if (before.getPlainAttr(schema).isPresent()
                        && userUR.getPlainAttrs().stream().noneMatch(attrPatch ->
                        attrPatch.getAttr().getSchema().equals(schema))
                        && op.getOp() == PatchOp.remove) {
                    userUR.getPlainAttrs().add(new AttrPatch.Builder(new Attr.Builder(schema).build())
                            .operation(PatchOperation.DELETE)
                            .build());
                }
        }
    }

    protected void setAttribute(final Set<AttrPatch> attrs, final String schema, final SCIMPatchOperation op) {
        Optional.ofNullable(schema).ifPresent(a -> {
            Attr.Builder attr = new Attr.Builder(a);
            if (!CollectionUtils.isEmpty(op.getValue())) {
                attr.value(op.getValue().get(0).toString());
            }

            attrs.add(new AttrPatch.Builder(attr.build()).
                    operation(op.getOp() == PatchOp.remove ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                    build());
        });
    }

    protected <E extends Enum<?>> void setAttribute(
            final Set<AttrPatch> attrs,
            final List<SCIMComplexConf<E>> confs,
            final SCIMPatchOperation op) {

        confs.stream().
                filter(conf -> BooleanUtils.toBoolean(JexlUtils.evaluateExpr(
                filter2JexlExpression(op.getPath().getFilter()),
                new MapContext(Map.of("type", conf.getType().name()))).toString())).findFirst().
                ifPresent(conf -> {
                    if (op.getPath().getSub() == null || "display".equals(op.getPath().getSub())) {
                        setAttribute(attrs, conf.getDisplay(), op);
                    }
                    if (op.getPath().getSub() == null || "value".equals(op.getPath().getSub())) {
                        setAttribute(attrs, conf.getValue(), op);
                    }
                });
    }

    protected <E extends Enum<?>> void setAttribute(
            final Set<AttrPatch> attrs,
            final List<SCIMComplexConf<E>> confs,
            final List<SCIMComplexValue> values,
            final PatchOp patchOp) {

        values.stream().
                filter(value -> value.getType() != null).forEach(value -> confs.stream().
                filter(conf -> value.getType().equals(conf.getType().name())).findFirst().
                ifPresent(conf -> attrs.add(new AttrPatch.Builder(
                new Attr.Builder(conf.getValue()).value(value.getValue()).build()).
                operation(patchOp == PatchOp.remove ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).
                build())));
    }

    protected void setAttribute(
            final Set<AttrPatch> attrs,
            final SCIMUserAddressConf conf,
            final SCIMPatchOperation op) {

        if (op.getPath().getSub() == null || "formatted".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getFormatted(), op);
        }
        if (op.getPath().getSub() == null || "streetAddress".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getStreetAddress(), op);
        }
        if (op.getPath().getSub() == null || "locality".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getLocality(), op);
        }
        if (op.getPath().getSub() == null || "region".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getRegion(), op);
        }
        if (op.getPath().getSub() == null || "postalCode".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getPostalCode(), op);
        }
        if (op.getPath().getSub() == null || "country".equals(op.getPath().getSub())) {
            setAttribute(attrs, conf.getCountry(), op);
        }
    }

    public Pair<UserUR, StatusR> toUserUpdate(
            final UserTO before,
            final Collection<String> resources,
            final SCIMPatchOp patch) {
        AtomicReference<StatusR> statusR = new AtomicReference<>();
        UserUR userUR = new UserUR.Builder(before.getKey()).build();

        patch.getOperations().forEach(op -> {
            if (op.getPath() == null && op.getOp() != PatchOp.remove
                    && !CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                SCIMUser after = (SCIMUser) op.getValue().get(0);
                if (after.getActive() != null && before.isSuspended() == after.isActive()) {
                    statusR.set(new StatusR.Builder(
                            before.getKey(),
                            after.isActive() ? StatusRType.REACTIVATE : StatusRType.SUSPEND)
                            .resources(resources)
                            .build());
                }
                addValues(userUR, before, after, resources, op);
            } else {
                SCIMConf conf = confManager.get();
                if (conf != null) {
                    switch (op.getPath().getAttribute()) {
                        case "externalId":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getExternalId(), op);
                            break;

                        case "userName":
                            if (op.getOp() != PatchOp.remove && !CollectionUtils.isEmpty(op.getValue())) {
                                userUR.setUsername(new StringReplacePatchItem.Builder().
                                        value(op.getValue().get(0).toString()).build());
                            }
                            break;

                        case "password":
                            if (op.getOp() != PatchOp.remove && !CollectionUtils.isEmpty(op.getValue())) {
                                userUR.setPassword(new PasswordPatch.Builder().
                                        value(op.getValue().get(0).toString()).resources(resources).build());
                            }
                            break;

                        case "active":
                            if (!CollectionUtils.isEmpty(op.getValue())) {

                                // Workaround for Microsoft Entra being not SCIM compliant on PATCH requests
                                if (op.getValue().get(0) instanceof String) {
                                    String a = (String) op.getValue().get(0);
                                    op.setValue(List.of(BooleanUtils.toBoolean(a)));
                                }

                                statusR.set(new StatusR.Builder(
                                        before.getKey(),
                                        (boolean) op.getValue().get(0) ? StatusRType.REACTIVATE : StatusRType.SUSPEND).
                                        resources(resources).
                                        build());
                            }
                            break;

                        case "name":
                            if (conf.getUserConf().getName() != null) {
                                if (op.getPath().getSub() == null || "familyName".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(), conf.getUserConf().getName().getFamilyName(), op);
                                }
                                if (op.getPath().getSub() == null || "formatted".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(), conf.getUserConf().getName().getFormatted(), op);
                                }
                                if (op.getPath().getSub() == null || "givenName".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(), conf.getUserConf().getName().getGivenName(), op);
                                }
                                if (op.getPath().getSub() == null || "honorificPrefix".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(),
                                            conf.getUserConf().getName().getHonorificPrefix(),
                                            op);
                                }
                                if (op.getPath().getSub() == null || "honorificSuffix".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(),
                                            conf.getUserConf().getName().getHonorificSuffix(),
                                            op);
                                }
                                if (op.getPath().getSub() == null || "middleName".equals(op.getPath().getSub())) {
                                    setAttribute(
                                            userUR.getPlainAttrs(), conf.getUserConf().getName().getMiddleName(), op);
                                }
                            }
                            break;

                        case "displayName":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getDisplayName(), op);
                            break;

                        case "nickName":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getNickName(), op);
                            break;

                        case "profileUrl":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getProfileUrl(), op);
                            break;

                        case "title":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getTitle(), op);
                            break;

                        case "userType":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getUserType(), op);
                            break;

                        case "preferredLanguage":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getPreferredLanguage(), op);
                            break;

                        case "locale":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getLocale(), op);
                            break;

                        case "timezone":
                            setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getTimezone(), op);
                            break;

                        case "emails":
                            if (!CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                                setAttribute(
                                        userUR.getPlainAttrs(),
                                        conf.getUserConf().getEmails(),
                                        ((SCIMUser) op.getValue().get(0)).getEmails(),
                                        op.getOp());
                            } else if (op.getPath().getFilter() != null) {
                                setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getEmails(), op);
                            }
                            break;

                        case "phoneNumbers":
                            if (!CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                                setAttribute(
                                        userUR.getPlainAttrs(),
                                        conf.getUserConf().getPhoneNumbers(),
                                        ((SCIMUser) op.getValue().get(0)).getPhoneNumbers(),
                                        op.getOp());
                            } else if (op.getPath().getFilter() != null) {
                                setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getPhoneNumbers(), op);
                            }
                            break;

                        case "ims":
                            if (!CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                                setAttribute(
                                        userUR.getPlainAttrs(),
                                        conf.getUserConf().getIms(),
                                        ((SCIMUser) op.getValue().get(0)).getIms(),
                                        op.getOp());
                            } else if (op.getPath().getFilter() != null) {
                                setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getIms(), op);
                            }
                            break;

                        case "photos":
                            if (!CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                                setAttribute(
                                        userUR.getPlainAttrs(),
                                        conf.getUserConf().getPhotos(),
                                        ((SCIMUser) op.getValue().get(0)).getPhotos(),
                                        op.getOp());
                            } else if (op.getPath().getFilter() != null) {
                                setAttribute(userUR.getPlainAttrs(), conf.getUserConf().getPhotos(), op);
                            }
                            break;

                        case "addresses":
                            if (!CollectionUtils.isEmpty(op.getValue()) && op.getValue().get(0) instanceof SCIMUser) {
                                SCIMUser after = (SCIMUser) op.getValue().get(0);
                                after.getAddresses().stream().filter(address -> address.getType() != null).
                                        forEach(address -> conf.getUserConf().getAddresses().stream().
                                        filter(object -> address.getType().equals(object.getType().name())).findFirst().
                                        ifPresent(addressConf ->
                                        setAttribute(userUR.getPlainAttrs(), addressConf, op)));
                            } else if (op.getPath().getFilter() != null) {
                                conf.getUserConf().getAddresses().stream().
                                        filter(addressConf -> BooleanUtils.toBoolean(JexlUtils.evaluateExpr(
                                        filter2JexlExpression(op.getPath().getFilter()),
                                        new MapContext(Map.of("type", addressConf.getType().name()))).toString())).
                                        findFirst().
                                        ifPresent(addressConf -> setAttribute(userUR.getPlainAttrs(), addressConf, op));
                            }
                            break;

                        case "employeeNumber":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getEmployeeNumber).orElse(null), op);
                            break;

                        case "costCenter":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getCostCenter).orElse(null), op);
                            break;

                        case "organization":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getOrganization).orElse(null), op);
                            break;

                        case "division":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getDivision).orElse(null), op);
                            break;

                        case "department":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getDepartment).orElse(null), op);
                            break;

                        case "manager":
                            setAttribute(userUR.getPlainAttrs(), Optional.ofNullable(conf.getEnterpriseUserConf()).
                                    map(SCIMEnterpriseUserConf::getManager).map(SCIMManagerConf::getKey).
                                    orElse(null), op);
                            break;

                        default:
                            Optional.ofNullable(conf.getExtensionUserConf()).
                                    flatMap(schema ->
                                    Optional.ofNullable(schema.asMap().get(op.getPath().getAttribute()))).
                                    ifPresent(schema -> setAttribute(userUR.getPlainAttrs(), schema, op));
                    }
                }
            }
        });
        return Pair.of(userUR, statusR.get());
    }

    @Transactional(readOnly = true)
    public SCIMGroup toSCIMGroup(
            final GroupTO groupTO,
            final String location,
            final List<String> attributes,
            final List<String> excludedAttributes) {

        SCIMConf conf = confManager.get();
        List<String> schemas = new ArrayList<>();
        schemas.add(Resource.Group.schema());
        if (conf.getExtensionGroupConf() != null) {
            schemas.add(Resource.ExtensionGroup.schema());
        }

        SCIMGroup group = new SCIMGroup(
                groupTO.getKey(),
                schemas,
                new Meta(
                        Resource.Group.name(),
                        groupTO.getCreationDate(),
                        Optional.ofNullable(groupTO.getLastChangeDate()).orElse(groupTO.getCreationDate()),
                        groupTO.getETagValue(),
                        location),
                output(attributes, excludedAttributes, "displayName", groupTO.getName()));

        Map<String, Attr> attrs = new HashMap<>();
        attrs.putAll(EntityTOUtils.buildAttrMap(groupTO.getPlainAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(groupTO.getDerAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(groupTO.getVirAttrs()));

        if (output(attributes, excludedAttributes, "externalId")
                && conf.getGroupConf() != null
                && conf.getGroupConf().getExternalId() != null
                && attrs.containsKey(conf.getGroupConf().getExternalId())) {

            group.setExternalId(attrs.get(conf.getGroupConf().getExternalId()).getValues().get(0));
        }

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(groupTO.getKey());
        SearchCond searchCond = SearchCond.getLeaf(membCond);

        if (conf.getExtensionGroupConf() != null) {
            SCIMExtensionInfo extensionInfo = new SCIMExtensionInfo();
            conf.getExtensionGroupConf().asMap().forEach((scimAttr, syncopeAttr) -> {
                if (output(attributes, excludedAttributes, scimAttr) && attrs.containsKey(syncopeAttr)) {
                    extensionInfo.getAttributes().put(scimAttr, attrs.get(syncopeAttr).getValues().get(0));
                }
            });

            if (!extensionInfo.isEmpty()) {
                group.setExtensionInfo(extensionInfo);
            }
        }

        if (output(attributes, excludedAttributes, "members")) {
            int count = userLogic.search(
                    searchCond, 1, 1, List.of(), SyncopeConstants.ROOT_REALM, true, false).getLeft();

            for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                List<UMembership> users = groupDAO.findUMemberships(
                        Optional.ofNullable(groupDAO.find(groupTO.getKey()))
                                .orElseThrow(() -> new NotFoundException("Group " + groupTO.getKey())),
                        page,
                        AnyDAO.DEFAULT_PAGE_SIZE);
                users.forEach(uMembership -> group.getMembers().add(new Member(
                        uMembership.getLeftEnd().getKey(),
                        StringUtils.substringBefore(location, "/Groups") + "/Users/" + uMembership.getKey(),
                        uMembership.getLeftEnd().getUsername())));
            }
        }

        return group;
    }

    public GroupTO toGroupTO(final SCIMGroup group, final boolean checkSchemas) {
        SCIMConf conf = confManager.get();
        Set<String> expectedSchemas = new HashSet<>();
        expectedSchemas.add(Resource.Group.schema());
        if (conf.getExtensionGroupConf() != null) {
            expectedSchemas.add(Resource.ExtensionGroup.schema());
        }
        if (checkSchemas
                && (!group.getSchemas().containsAll(expectedSchemas)
                || !expectedSchemas.containsAll(group.getSchemas()))) {

            throw new BadRequestException(ErrorType.invalidValue);
        }

        GroupTO groupTO = new GroupTO();
        groupTO.setRealm(SyncopeConstants.ROOT_REALM);
        groupTO.setKey(group.getId());
        groupTO.setName(group.getDisplayName());

        if (conf.getGroupConf() != null
                && conf.getGroupConf().getExternalId() != null && group.getExternalId() != null) {

            groupTO.getPlainAttrs().add(
                    new Attr.Builder(conf.getGroupConf().getExternalId()).
                            value(group.getExternalId()).build());
        }

        if (conf.getExtensionGroupConf() != null && group.getExtensionInfo() != null) {
            conf.getExtensionGroupConf().asMap().forEach((scimAttr, syncopeAttr) -> setAttribute(
                    groupTO, syncopeAttr, group.getExtensionInfo().getAttributes().get(scimAttr)));
        }

        return groupTO;
    }

    public GroupCR toGroupCR(final SCIMGroup group) {
        GroupTO groupTO = toGroupTO(group, true);
        GroupCR groupCR = new GroupCR();
        EntityTOUtils.toAnyCR(groupTO, groupCR);
        return groupCR;
    }

    public GroupUR toGroupUR(final GroupTO before, final SCIMPatchOperation op) {
        if (op.getPath() == null) {
            throw new UnsupportedOperationException("Empty path not supported for Groups");
        }

        GroupUR groupUR = new GroupUR.Builder(before.getKey()).build();

        if ("displayName".equals(op.getPath().getAttribute())) {
            StringReplacePatchItem.Builder name = new StringReplacePatchItem.Builder().
                    operation(op.getOp() == PatchOp.remove ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE);
            if (!CollectionUtils.isEmpty(op.getValue())) {
                name.value(op.getValue().get(0).toString());
            }
            groupUR.setName(name.build());
        } else {
            SCIMConf conf = confManager.get();
            if (conf.getGroupConf() != null && "externalId".equals(op.getPath().getAttribute())) {
                setAttribute(groupUR.getPlainAttrs(), conf.getGroupConf().getExternalId(), op);
            }
            if (conf.getExtensionGroupConf() != null) {
                Optional.ofNullable(conf.getExtensionGroupConf()).
                        flatMap(schema -> Optional.ofNullable(schema.asMap().get(op.getPath().getAttribute()))).
                        ifPresent(schema -> setAttribute(groupUR.getPlainAttrs(), schema, op));
            }
        }

        return groupUR;
    }

    public SCIMAnyObject toSCIMAnyObject(
            final AnyObjectTO anyObjectTO,
            final String location,
            final List<String> attributes,
            final List<String> excludedAttributes) {
        SCIMConf conf = confManager.get();
        List<String> schemas = new ArrayList<>();
        SCIMExtensionAnyObjectConf scimExtensionAnyObjectConf =
                conf.getExtensionAnyObjectsConf().stream()
                        .filter(scimExtAnyObjectConf -> scimExtAnyObjectConf.getType().equals(anyObjectTO.getType()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("SCIMExtensionAnyObjectConf not found"));
        schemas.add("urn:ietf:params:scim:schemas:extension:syncope:2.0:" + scimExtensionAnyObjectConf.getType());

        SCIMAnyObject anyObject = new SCIMAnyObject(
                anyObjectTO.getKey(),
                schemas,
                new Meta(
                        "urn:ietf:params:scim:schemas:extension:syncope:2.0:"
                                + scimExtensionAnyObjectConf.getType(),
                        anyObjectTO.getCreationDate(),
                        Optional.ofNullable(anyObjectTO.getLastChangeDate()).orElse(anyObjectTO.getCreationDate()),
                        anyObjectTO.getETagValue(),
                        location),
                output(attributes, excludedAttributes, "displayName", anyObjectTO.getName()));

        Map<String, Attr> attrs = new HashMap<>();
        attrs.putAll(EntityTOUtils.buildAttrMap(anyObjectTO.getPlainAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(anyObjectTO.getDerAttrs()));
        attrs.putAll(EntityTOUtils.buildAttrMap(anyObjectTO.getVirAttrs()));

        if (output(attributes, excludedAttributes, "externalId")
                && scimExtensionAnyObjectConf.getExternalId() != null
                && attrs.containsKey(scimExtensionAnyObjectConf.getExternalId())) {

            anyObject.setExternalId(attrs.get(scimExtensionAnyObjectConf.getExternalId()).getValues().get(0));
        }

        SCIMExtensionInfo extensionInfo = new SCIMExtensionInfo();
        scimExtensionAnyObjectConf.asMap().forEach((scimAttr, syncopeAttr) -> {
            if (output(attributes, excludedAttributes, scimAttr) && attrs.containsKey(syncopeAttr)) {
                extensionInfo.getAttributes().put(scimAttr, attrs.get(syncopeAttr).getValues().get(0));
            }
        });

        if (!extensionInfo.isEmpty()) {
            anyObject.setExtensionInfo(extensionInfo);
        }

        return anyObject;
    }

    public AnyObjectTO toAnyObjectTO(final SCIMAnyObject anyObject, final boolean checkSchemas) {
        SCIMConf conf = confManager.get();
        Set<String> expectedSchemas = new HashSet<>();
        Optional<SCIMExtensionAnyObjectConf> scimExtensionAnyObjectConf =
                conf.getExtensionAnyObjectsConf().stream()
                        .filter(scimExtAnyObjectConf ->
                                scimExtAnyObjectConf.getType().equals(anyObject.getExtensionUrn()
                                        .substring(anyObject.getExtensionUrn().lastIndexOf(':') + 1)))
                        .findFirst();
        scimExtensionAnyObjectConf.ifPresent(scimExtAnyObjectConf ->
                expectedSchemas.add("urn:ietf:params:scim:schemas:extension:syncope:2.0:"
                        + scimExtAnyObjectConf.getType()));
        if (checkSchemas
                && (!anyObject.getSchemas().containsAll(expectedSchemas)
                || !expectedSchemas.containsAll(anyObject.getSchemas()))) {

            throw new BadRequestException(ErrorType.invalidValue);
        }

        AnyObjectTO anyObjectTO = new AnyObjectTO();
        anyObjectTO.setRealm(SyncopeConstants.ROOT_REALM);
        anyObjectTO.setKey(anyObject.getId());
        anyObjectTO.setName(anyObject.getDisplayName());
        anyObjectTO.setType(anyObject.getExtensionUrn().substring(anyObject.getExtensionUrn().lastIndexOf(':') + 1));

        if (scimExtensionAnyObjectConf.isPresent()
                && scimExtensionAnyObjectConf.get().getExternalId() != null && anyObject.getExternalId() != null) {

            anyObjectTO.getPlainAttrs().add(
                    new Attr.Builder(scimExtensionAnyObjectConf.get().getExternalId()).
                            value(anyObject.getExternalId()).build());
        }

        if (scimExtensionAnyObjectConf.isPresent() && anyObject.getExtensionInfo() != null) {
            scimExtensionAnyObjectConf.get().asMap().forEach((scimAttr, syncopeAttr) -> setAttribute(
                    anyObjectTO, syncopeAttr, anyObject.getExtensionInfo().getAttributes().get(scimAttr)));
        }

        return anyObjectTO;
    }

    public AnyObjectCR toAnyObjectCR(final SCIMAnyObject anyObject) {
        AnyObjectTO anyObjectTO = toAnyObjectTO(anyObject, true);
        AnyObjectCR anyObjectCR = new AnyObjectCR();
        EntityTOUtils.toAnyCR(anyObjectTO, anyObjectCR);
        return anyObjectCR;
    }

    public AnyObjectUR toAnyObjectUR(final AnyObjectTO before, final SCIMPatchOperation op) {
        if (op.getPath() == null) {
            throw new UnsupportedOperationException("Empty path not supported for AnyObjects");
        }

        AnyObjectUR anyObjectUR = new AnyObjectUR.Builder(before.getKey()).build();

        if ("displayName".equals(op.getPath().getAttribute())) {
            StringReplacePatchItem.Builder name = new StringReplacePatchItem.Builder().
                    operation(op.getOp() == PatchOp.remove ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE);
            if (!CollectionUtils.isEmpty(op.getValue())) {
                name.value(op.getValue().get(0).toString());
            }
            anyObjectUR.setName(name.build());
        } else {
            SCIMConf conf = confManager.get();
            Optional<SCIMExtensionAnyObjectConf> scimExtensionAnyObjectConf =
                    conf.getExtensionAnyObjectsConf().stream()
                            .filter(scimExtAnyObjectConf -> scimExtAnyObjectConf.getType().equals(before.getType()))
                            .findFirst();
            if (scimExtensionAnyObjectConf.isPresent() && "externalId".equals(op.getPath().getAttribute())) {
                setAttribute(anyObjectUR.getPlainAttrs(), scimExtensionAnyObjectConf.get().getExternalId(), op);
            }
            scimExtensionAnyObjectConf.flatMap(extensionAnyObjectConf -> Optional.of(extensionAnyObjectConf)
                            .flatMap(schema -> Optional.ofNullable(schema.asMap().get(op.getPath().getAttribute()))))
                    .ifPresent(schema -> setAttribute(anyObjectUR.getPlainAttrs(), schema, op));
        }

        return anyObjectUR;
    }
}
