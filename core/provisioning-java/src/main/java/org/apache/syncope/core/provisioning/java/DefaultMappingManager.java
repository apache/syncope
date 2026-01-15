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
package org.apache.syncope.core.provisioning.java;

import java.text.ParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Mapping;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Groupable;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Relatable;
import org.apache.syncope.core.persistence.api.entity.Relationship;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.Account;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.api.utils.FormatUtils;
import org.apache.syncope.core.provisioning.api.AccountGetter;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.PlainAttrGetter;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.jexl.JexlContextBuilder;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class DefaultMappingManager implements MappingManager {

    protected static final Logger LOG = LoggerFactory.getLogger(MappingManager.class);

    protected static Optional<String> processPreparedAttr(
            final PreparedAttr preparedAttr,
            final Set<Attribute> attributes) {

        if (preparedAttr == null) {
            return Optional.empty();
        }

        String connObjectKey = null;

        if (preparedAttr.connObjectLink() != null) {
            connObjectKey = preparedAttr.connObjectLink();
        }

        if (preparedAttr.attribute() != null) {
            Optional.ofNullable(AttributeUtil.find(preparedAttr.attribute().getName(), attributes)).ifPresentOrElse(
                    alreadyAdded -> {
                        attributes.remove(alreadyAdded);

                        Set<Object> values = new HashSet<>();
                        if (!CollectionUtils.isEmpty(alreadyAdded.getValue())) {
                            values.addAll(alreadyAdded.getValue());
                        }
                        if (preparedAttr.attribute().getValue() != null) {
                            values.addAll(preparedAttr.attribute().getValue());
                        }

                        attributes.add(AttributeBuilder.build(preparedAttr.attribute().getName(), values));
                    },
                    () -> attributes.add(preparedAttr.attribute()));
        }

        return Optional.ofNullable(connObjectKey);
    }

    protected static Name getName(final String evalConnObjectLink, final String connObjectKey) {
        // If connObjectLink evaluates to an empty string, just use the provided connObjectKey as Name(),
        // otherwise evaluated connObjectLink expression is taken as Name().
        Name name;
        if (StringUtils.isBlank(evalConnObjectLink)) {
            // add connObjectKey as __NAME__ attribute ...
            LOG.debug("Add connObjectKey [{}] as {}", connObjectKey, Name.NAME);
            name = new Name(connObjectKey);
        } else {
            LOG.debug("Add connObjectLink [{}] as {}", evalConnObjectLink, Name.NAME);
            name = new Name(evalConnObjectLink);

            // connObjectKey not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("connObjectKey [{}] will be used as {}", connObjectKey, Uid.NAME);
        }

        return name;
    }

    protected static PlainAttrValue clonePlainAttrValue(final PlainAttrValue src) {
        PlainAttrValue dst = new PlainAttrValue();

        dst.setBinaryValue(src.getBinaryValue());
        dst.setBooleanValue(src.getBooleanValue());
        dst.setDateValue(src.getDateValue());
        dst.setDoubleValue(src.getDoubleValue());
        dst.setLongValue(src.getLongValue());
        dst.setStringValue(src.getStringValue());

        return dst;
    }

    protected final AnyTypeDAO anyTypeDAO;

    protected final UserDAO userDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final GroupDAO groupDAO;

    protected final RelationshipTypeDAO relationshipTypeDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final ImplementationDAO implementationDAO;

    protected final DerAttrHandler derAttrHandler;

    protected final IntAttrNameParser intAttrNameParser;

    protected final EncryptorManager encryptorManager;

    protected final JexlTools jexlTools;

    public DefaultMappingManager(
            final AnyTypeDAO anyTypeDAO,
            final UserDAO userDAO,
            final AnyObjectDAO anyObjectDAO,
            final GroupDAO groupDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final RealmSearchDAO realmSearchDAO,
            final ImplementationDAO implementationDAO,
            final DerAttrHandler derAttrHandler,
            final IntAttrNameParser intAttrNameParser,
            final EncryptorManager encryptorManager,
            final JexlTools jexlTools) {

        this.anyTypeDAO = anyTypeDAO;
        this.userDAO = userDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.groupDAO = groupDAO;
        this.relationshipTypeDAO = relationshipTypeDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.implementationDAO = implementationDAO;
        this.derAttrHandler = derAttrHandler;
        this.intAttrNameParser = intAttrNameParser;
        this.encryptorManager = encryptorManager;
        this.jexlTools = jexlTools;
    }

    protected List<Implementation> getTransformers(final Item item) {
        return item.getTransformers().stream().
                map(implementationDAO::findById).
                flatMap(Optional::stream).
                collect(Collectors.toList());
    }

    /**
     * Build __NAME__ for propagation.
     * First look if there is a defined connObjectLink for the given resource (and in
     * this case evaluate as JEXL); otherwise, take given connObjectKey.
     *
     * @param any given any object
     * @param provision external resource
     * @param connObjectKey connector object key
     * @return the value to be propagated as __NAME__
     */
    protected Name evaluateNAME(final Any any, final Provision provision, final String connObjectKey) {
        if (StringUtils.isBlank(connObjectKey)) {
            // log but avoid to throw exception: leave it to the external resource
            LOG.debug("Missing connObjectKey for {}", any.getType().getKey());
        }

        // Evaluate connObjectKey expression
        String connObjectLink = Optional.ofNullable(provision.getMapping()).
                map(Mapping::getConnObjectLink).
                orElse(null);
        String evalConnObjectLink = null;
        if (StringUtils.isNotBlank(connObjectLink)) {
            JexlContext jexlContext = new JexlContextBuilder().
                    fields(any).
                    plainAttrs(any.getPlainAttrs()).
                    derAttrs(any, derAttrHandler).
                    build();

            evalConnObjectLink = jexlTools.evaluateExpression(connObjectLink, jexlContext).toString();
        }

        return getName(evalConnObjectLink, connObjectKey);
    }

    /**
     * Build __NAME__ for propagation.
     * First look if there is a defined connObjectLink for the given resource (and in
     * this case evaluate as JEXL); otherwise, take given connObjectKey.
     *
     * @param realm given any object
     * @param orgUnit external resource
     * @param connObjectKey connector object key
     * @return the value to be propagated as __NAME__
     */
    protected Name evaluateNAME(final Realm realm, final OrgUnit orgUnit, final String connObjectKey) {
        if (StringUtils.isBlank(connObjectKey)) {
            // log but avoid to throw exception: leave it to the external resource
            LOG.debug("Missing connObjectKey for Realms");
        }

        // Evaluate connObjectKey expression
        String connObjectLink = orgUnit.getConnObjectLink();
        String evalConnObjectLink = null;
        if (StringUtils.isNotBlank(connObjectLink)) {
            JexlContext jexlContext = new JexlContextBuilder().
                    fields(realm).
                    plainAttrs(realm.getPlainAttrs()).
                    derAttrs(realm, derAttrHandler).
                    build();

            evalConnObjectLink = jexlTools.evaluateExpression(connObjectLink, jexlContext).toString();
        }

        return getName(evalConnObjectLink, connObjectKey);
    }

    @Transactional(readOnly = true)
    @Override
    public PreparedAttrs prepareAttrsFromAny(
            final Any any,
            final String password,
            final boolean changePwd,
            final Boolean enable,
            final ExternalResource resource,
            final Provision provision) {

        LOG.debug("Preparing resource attributes for {} with provision {} for attributes {}",
                any, provision, any.getPlainAttrs());

        Set<Attribute> attributes = new HashSet<>();
        Mutable<String> connObjectKeyValue = new MutableObject<>();

        MappingUtils.getPropagationItems(provision.getMapping().getItems().stream()).forEach(item -> {
            LOG.debug("Processing expression '{}'", item.getIntAttrName());

            try {
                processPreparedAttr(
                        prepareAttr(
                                resource,
                                provision,
                                item,
                                any,
                                password,
                                AccountGetter.DEFAULT,
                                AccountGetter.DEFAULT,
                                PlainAttrGetter.DEFAULT),
                        attributes).ifPresent(connObjectKeyValue::setValue);
            } catch (Exception e) {
                LOG.error("Expression '{}' processing failed", item.getIntAttrName(), e);
            }
        });

        MappingUtils.getConnObjectKeyItem(provision).ifPresent(item -> {
            Attribute connObjectKeyAttr = AttributeUtil.find(item.getExtAttrName(), attributes);
            if (connObjectKeyAttr != null) {
                attributes.remove(connObjectKeyAttr);
                attributes.add(AttributeBuilder.build(item.getExtAttrName(), connObjectKeyValue.get()));
            }

            Name name = evaluateNAME(any, provision, connObjectKeyValue.get());
            attributes.add(name);

            Optional.ofNullable(connObjectKeyValue.get()).
                    filter(cokv -> connObjectKeyAttr == null && !cokv.equals(name.getNameValue())).
                    ifPresent(cokv -> attributes.add(AttributeBuilder.build(item.getExtAttrName(), cokv)));
        });

        Optional.ofNullable(enable).ifPresent(e -> attributes.add(AttributeBuilder.buildEnabled(e)));

        if (!changePwd) {
            Optional.ofNullable(AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes)).
                    ifPresent(attributes::remove);
        }

        return new PreparedAttrs(connObjectKeyValue.get(), attributes);
    }

    @Transactional(readOnly = true)
    @Override
    public Set<Attribute> prepareAttrsFromLinkedAccount(
            final User user,
            final LinkedAccount account,
            final String password,
            final boolean changePwd,
            final Provision provision) {

        LOG.debug("Preparing resource attributes for linked account {} of user {} with provision {} "
                + "for user attributes {} with override {}",
                account, user, provision, user.getPlainAttrs(), account.getPlainAttrs());

        Set<Attribute> attributes = new HashSet<>();

        MappingUtils.getPropagationItems(provision.getMapping().getItems().stream()).forEach(item -> {
            LOG.debug("Processing expression '{}'", item.getIntAttrName());

            try {
                processPreparedAttr(
                        prepareAttr(
                                account.getResource(),
                                provision,
                                item,
                                user,
                                password,
                                acct -> account.getUsername() == null ? AccountGetter.DEFAULT.apply(acct) : account,
                                acct -> account.getPassword() == null ? AccountGetter.DEFAULT.apply(acct) : account,
                                (attributable, schema) -> {
                                    PlainAttr result = null;
                                    if (attributable instanceof User) {
                                        result = account.getPlainAttr(schema).orElse(null);
                                    }
                                    if (result == null) {
                                        result = PlainAttrGetter.DEFAULT.apply(attributable, schema);
                                    }
                                    return result;
                                }),
                        attributes);
            } catch (Exception e) {
                LOG.error("Expression '{}' processing failed", item.getIntAttrName(), e);
            }
        });

        String connObjectKey = account.getConnObjectKeyValue();
        MappingUtils.getConnObjectKeyItem(provision).ifPresent(connObjectKeyItem -> {
            Attribute connObjectKeyExtAttr = AttributeUtil.find(connObjectKeyItem.getExtAttrName(), attributes);
            if (connObjectKeyExtAttr != null) {
                attributes.remove(connObjectKeyExtAttr);
                attributes.add(AttributeBuilder.build(connObjectKeyItem.getExtAttrName(), connObjectKey));
            }
            Name name = evaluateNAME(user, provision, connObjectKey);
            attributes.add(name);
            if (!connObjectKey.equals(name.getNameValue()) && connObjectKeyExtAttr == null) {
                attributes.add(AttributeBuilder.build(connObjectKeyItem.getExtAttrName(), connObjectKey));
            }
        });

        if (account.isSuspended() != null) {
            attributes.add(AttributeBuilder.buildEnabled(BooleanUtils.negate(account.isSuspended())));
        }
        if (!changePwd) {
            Attribute pwdAttr = AttributeUtil.find(OperationalAttributes.PASSWORD_NAME, attributes);
            if (pwdAttr != null) {
                attributes.remove(pwdAttr);
            }
        }

        return attributes;
    }

    @Override
    public PreparedAttrs prepareAttrsFromRealm(final Realm realm, final ExternalResource resource) {
        if (resource.getOrgUnit() == null) {
            LOG.error("No mapping configured for Realms");
            return new PreparedAttrs(null, Set.of());
        }

        LOG.debug("Preparing resource attributes for {} with orgUnit {}", realm, resource.getOrgUnit());

        Set<Attribute> attributes = new HashSet<>();
        Mutable<String> connObjectKeyValue = new MutableObject<>();

        MappingUtils.getPropagationItems(resource.getOrgUnit().getItems().stream()).forEach(item -> {
            LOG.debug("Processing expression '{}'", item.getIntAttrName());

            try {
                processPreparedAttr(
                        prepareAttr(
                                resource,
                                item,
                                realm),
                        attributes).ifPresent(connObjectKeyValue::setValue);
            } catch (Exception e) {
                LOG.error("Expression '{}' processing failed", item.getIntAttrName(), e);
            }
        });

        resource.getOrgUnit().getConnObjectKeyItem().ifPresent(item -> {
            Attribute connObjectKeyAttr = AttributeUtil.find(item.getExtAttrName(), attributes);
            if (connObjectKeyAttr != null) {
                attributes.remove(connObjectKeyAttr);
                attributes.add(AttributeBuilder.build(item.getExtAttrName(), connObjectKeyValue.get()));
            }

            Name name = evaluateNAME(realm, resource.getOrgUnit(), connObjectKeyValue.get());
            attributes.add(name);

            Optional.ofNullable(connObjectKeyValue.get()).
                    filter(cokv -> connObjectKeyAttr == null && !cokv.equals(name.getNameValue())).
                    ifPresent(cokv -> attributes.add(AttributeBuilder.build(item.getExtAttrName(), cokv)));
        });

        return new PreparedAttrs(connObjectKeyValue.get(), attributes);
    }

    protected Optional<String> decodePassword(final Account account) {
        try {
            return Optional.of(encryptorManager.getInstance().
                    decode(account.getPassword(), account.getCipherAlgorithm()));
        } catch (Exception e) {
            LOG.error("Could not decode password for {}", account, e);
            return Optional.empty();
        }
    }

    protected Optional<String> getPasswordAttrValue(final Account account, final String defaultValue) {
        Optional<String> passwordAttrValue;
        if (account instanceof LinkedAccount) {
            passwordAttrValue = account.getPassword() == null
                    ? Optional.of(defaultValue)
                    : decodePassword(account);
        } else {
            if (StringUtils.isNotBlank(defaultValue)) {
                passwordAttrValue = Optional.of(defaultValue);
            } else if (account.canDecodeSecrets()) {
                passwordAttrValue = decodePassword(account);
            } else {
                passwordAttrValue = Optional.empty();
            }
        }

        return passwordAttrValue;
    }

    @Override
    public PreparedAttr prepareAttr(
            final ExternalResource resource,
            final Provision provision,
            final Item item,
            final Any any,
            final String password,
            final AccountGetter usernameAccountGetter,
            final AccountGetter passwordAccountGetter,
            final PlainAttrGetter plainAttrGetter) {

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(item.getIntAttrName(), any.getType().getKind());
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            return null;
        }

        AttrSchemaType schemaType = intAttrName.getSchema() instanceof PlainSchema
                ? intAttrName.getSchema().getType()
                : AttrSchemaType.String;

        IntValues intValues = getIntValues(
                resource, provision, item, intAttrName, schemaType, any, usernameAccountGetter, plainAttrGetter);
        schemaType = intValues.attrSchemaType();
        List<PlainAttrValue> values = intValues.values();

        LOG.debug(
                """
                  Define mapping for: 
                  * Item {}
                  * Schema {}
                  * ClassType {}
                  * AttrSchemaType {}
                  * Values {}""",
                item, intAttrName.getSchema(), schemaType.getType().getName(), schemaType, values);

        List<Object> objValues = new ArrayList<>();

        for (PlainAttrValue value : values) {
            if (intAttrName.getSchema() instanceof PlainSchema schema && schemaType == AttrSchemaType.Encrypted) {
                String decoded = null;
                try {
                    decoded = encryptorManager.getInstance(schema.getSecretKey()).
                            decode(value.getStringValue(), schema.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.warn("Could not decode value for {} with algorithm {}",
                            intAttrName.getSchema(), schema.getCipherAlgorithm(), e);
                }
                objValues.add(Optional.ofNullable(decoded).orElse(value.getStringValue()));
            } else if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                objValues.add(value.getValue());
            } else {
                PlainSchema plainSchema = intAttrName.getSchema() instanceof final PlainSchema schema
                        ? schema
                        : null;
                if (plainSchema == null || plainSchema.getType() != schemaType) {
                    objValues.add(value.getValueAsString(schemaType));
                } else {
                    objValues.add(value.getValueAsString(plainSchema));
                }
            }
        }

        PreparedAttr result;
        if (item.isConnObjectKey()) {
            result = new PreparedAttr(objValues.isEmpty() ? null : objValues.getFirst().toString(), null);
        } else if (item.isPassword() && any instanceof User user) {
            result = getPasswordAttrValue(passwordAccountGetter.apply(user), password).
                    map(passwordAttrValue -> new PreparedAttr(
                    null, AttributeBuilder.buildPassword(passwordAttrValue.toCharArray()))).
                    orElse(null);
        } else if (objValues.isEmpty()) {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.build(item.getExtAttrName()));
        } else if (OperationalAttributes.PASSWORD_NAME.equals(item.getExtAttrName())) {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.buildPassword(objValues.getFirst().toString().toCharArray()));
        } else {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.build(item.getExtAttrName(), objValues));
        }

        return result;
    }

    @Override
    public PreparedAttr prepareAttr(
            final ExternalResource resource,
            final Item item,
            final Realm realm) {

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(item.getIntAttrName());
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            return null;
        }

        AttrSchemaType schemaType = intAttrName.getSchema() instanceof PlainSchema
                ? intAttrName.getSchema().getType()
                : AttrSchemaType.String;

        IntValues intValues = getIntValues(resource, item, intAttrName, schemaType, realm);
        schemaType = intValues.attrSchemaType();
        List<PlainAttrValue> values = intValues.values();

        LOG.debug(
                """
                  Define mapping for: 
                  * Item {}
                  * Schema {}
                  * ClassType {}
                  * AttrSchemaType {}
                  * Values {}""",
                item, intAttrName.getSchema(), schemaType.getType().getName(), schemaType, values);

        List<Object> objValues = new ArrayList<>();

        for (PlainAttrValue value : values) {
            if (intAttrName.getSchema() instanceof PlainSchema schema && schemaType == AttrSchemaType.Encrypted) {
                String decoded = null;
                try {
                    decoded = encryptorManager.getInstance(schema.getSecretKey()).
                            decode(value.getStringValue(), schema.getCipherAlgorithm());
                } catch (Exception e) {
                    LOG.warn("Could not decode value for {} with algorithm {}",
                            intAttrName.getSchema(), schema.getCipherAlgorithm(), e);
                }
                objValues.add(Optional.ofNullable(decoded).orElse(value.getStringValue()));
            } else if (FrameworkUtil.isSupportedAttributeType(schemaType.getType())) {
                objValues.add(value.getValue());
            } else {
                PlainSchema plainSchema = intAttrName.getSchema() instanceof final PlainSchema schema
                        ? schema
                        : null;
                if (plainSchema == null || plainSchema.getType() != schemaType) {
                    objValues.add(value.getValueAsString(schemaType));
                } else {
                    objValues.add(value.getValueAsString(plainSchema));
                }
            }
        }

        PreparedAttr result;
        if (item.isConnObjectKey()) {
            result = new PreparedAttr(objValues.isEmpty() ? null : objValues.getFirst().toString(), null);
        } else if (objValues.isEmpty()) {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.build(item.getExtAttrName()));
        } else if (OperationalAttributes.PASSWORD_NAME.equals(item.getExtAttrName())) {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.buildPassword(objValues.iterator().next().toString().toCharArray()));
        } else {
            result = new PreparedAttr(
                    null,
                    AttributeBuilder.build(item.getExtAttrName(), objValues));
        }

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public IntValues getIntValues(
            final ExternalResource resource,
            final Provision provision,
            final Item item,
            final IntAttrName intAttrName,
            final AttrSchemaType schemaType,
            final Any any,
            final AccountGetter usernameAccountGetter,
            final PlainAttrGetter plainAttrGetter) {

        LOG.debug("Get internal values for {} as '{}' on {}", any, item.getIntAttrName(), resource);

        List<Any> references = new ArrayList<>();
        if (intAttrName.getEnclosingGroup() == null
                && intAttrName.getRelatedAnyObject() == null
                && intAttrName.getRelationshipAnyType() == null
                && intAttrName.getRelationshipType() == null
                && intAttrName.getRelatedUser() == null) {

            references.add(any);
        }
        Membership<?> membership = null;

        if (intAttrName.getEnclosingGroup() != null) {
            Group group = groupDAO.findByName(intAttrName.getEnclosingGroup()).orElse(null);
            if (group == null
                    || any instanceof User
                            ? !userDAO.findAllGroupKeys((User) any).contains(group.getKey())
                            : any instanceof final AnyObject anyObject
                                    ? !anyObjectDAO.findAllGroupKeys(anyObject).contains(group.getKey())
                                    : false) {

                LOG.warn("No (dyn) membership for {} in {}, ignoring", intAttrName.getEnclosingGroup(), any);
            } else {
                references.add(group);
            }
        } else if (intAttrName.getRelatedUser() != null) {
            User user = userDAO.findByUsername(intAttrName.getRelatedUser()).orElse(null);
            if (user == null || user.getRelationships(any.getKey()).isEmpty()) {
                LOG.warn("No relationship for {} in {}, ignoring", intAttrName.getRelatedUser(), any);
            } else if (any.getType().getKind() == AnyTypeKind.USER) {
                LOG.warn("Users cannot have relationship with other users, ignoring");
            } else {
                references.add(user);
            }
        } else if (intAttrName.getRelatedAnyObject() != null && any instanceof Relatable<?, ?> relatable) {
            AnyObject anyObject = anyObjectDAO.findById(intAttrName.getRelatedAnyObject()).orElse(null);
            if (anyObject == null || relatable.getRelationships(anyObject.getKey()).isEmpty()) {
                LOG.warn("No relationship for {} in {}, ignoring", intAttrName.getRelatedAnyObject(), relatable);
            } else {
                references.add(anyObject);
            }
        } else if (intAttrName.getRelationshipAnyType() != null && intAttrName.getRelationshipType() != null
                && any instanceof Relatable<?, ?> relatable) {

            RelationshipType relationshipType = relationshipTypeDAO.findById(
                    intAttrName.getRelationshipType()).orElse(null);
            AnyType anyType = anyTypeDAO.findById(intAttrName.getRelationshipAnyType()).orElse(null);
            if (relationshipType == null || relatable.getRelationships(relationshipType).isEmpty()) {
                LOG.warn("No relationship for type {} in {}, ignoring", intAttrName.getRelationshipType(), relatable);
            } else if (anyType == null) {
                LOG.warn("No anyType {}, ignoring", intAttrName.getRelationshipAnyType());
            } else {
                references.addAll(relatable.getRelationships(relationshipType).stream().
                        filter(relationship -> anyType.equals(relationship.getRightEnd().getType())).
                        map(Relationship::getRightEnd).
                        toList());
            }
        } else if (intAttrName.getMembershipOfGroup() != null && any instanceof Groupable<?, ?, ?> groupable) {
            membership = groupDAO.findByName(intAttrName.getMembershipOfGroup()).
                    flatMap(group -> groupable.getMembership(group.getKey())).
                    orElse(null);
        }
        if (references.isEmpty()) {
            LOG.warn("Could not determine the reference instance for {}", item.getIntAttrName());
            return new IntValues(schemaType, List.of());
        }

        List<PlainAttrValue> values = new ArrayList<>();
        boolean transform = true;

        for (Any ref : references) {
            if (intAttrName.getField() != null) {
                PlainAttrValue attrValue = new PlainAttrValue();

                switch (intAttrName.getField()) {
                    case "key" -> {
                        attrValue.setStringValue(ref.getKey());
                        values.add(attrValue);
                    }

                    case "username" -> {
                        if (ref instanceof Account account) {
                            attrValue.setStringValue(usernameAccountGetter.apply(account).getUsername());
                            values.add(attrValue);
                        }
                    }

                    case "realm" -> {
                        attrValue.setStringValue(ref.getRealm().getFullPath());
                        values.add(attrValue);
                    }

                    case "password" -> {
                    }

                    case "userOwner", "groupOwner" -> {
                        Mapping uMappingTO = provision.getAnyType().equals(AnyTypeKind.USER.name())
                                ? provision.getMapping()
                                : null;
                        Mapping gMappingTO = provision.getAnyType().equals(AnyTypeKind.GROUP.name())
                                ? provision.getMapping()
                                : null;

                        if (ref instanceof Group group) {
                            String groupOwnerValue = null;
                            if (group.getUserOwner() != null && uMappingTO != null) {
                                groupOwnerValue = getGroupOwnerValue(resource, provision, group.getUserOwner());
                            }
                            if (group.getGroupOwner() != null && gMappingTO != null) {
                                groupOwnerValue = getGroupOwnerValue(resource, provision, group.getGroupOwner());
                            }

                            if (StringUtils.isNotBlank(groupOwnerValue)) {
                                attrValue.setStringValue(groupOwnerValue);
                                values.add(attrValue);
                            }
                        }
                    }
                    case "suspended" -> {
                        if (ref instanceof User user) {
                            attrValue.setBooleanValue(user.isSuspended());
                            values.add(attrValue);
                        }
                    }

                    case "mustChangePassword" -> {
                        if (ref instanceof User user) {
                            attrValue.setBooleanValue(user.isMustChangePassword());
                            values.add(attrValue);
                        }
                    }

                    default -> {
                        try {
                            Object fieldValue = FieldUtils.readField(ref, intAttrName.getField(), true);
                            if (fieldValue instanceof TemporalAccessor temporalAccessor) {
                                // needed because ConnId does not natively supports the Date type
                                attrValue.setStringValue(FormatUtils.format(temporalAccessor));
                            } else if (Boolean.TYPE.isInstance(fieldValue)) {
                                attrValue.setBooleanValue((Boolean) fieldValue);
                            } else if (Double.TYPE.isInstance(fieldValue) || Float.TYPE.isInstance(fieldValue)) {
                                attrValue.setDoubleValue((Double) fieldValue);
                            } else if (Long.TYPE.isInstance(fieldValue) || Integer.TYPE.isInstance(fieldValue)) {
                                attrValue.setLongValue((Long) fieldValue);
                            } else {
                                attrValue.setStringValue(fieldValue.toString());
                            }
                            values.add(attrValue);
                        } catch (Exception e) {
                            LOG.error("Could not read value of '{}' from {}", intAttrName.getField(), ref, e);
                        }
                    }
                }
                // ignore
            } else if (intAttrName.getSchemaType() != null) {
                switch (intAttrName.getSchemaType()) {
                    case PLAIN -> {
                        PlainAttr attr = membership == null
                                ? plainAttrGetter.apply(ref, intAttrName.getSchema().getKey())
                                : ((Groupable<?, ?, ?>) ref).getPlainAttr(
                                        intAttrName.getSchema().getKey(), membership).orElse(null);
                        if (attr != null) {
                            if (attr.getUniqueValue() != null) {
                                values.add(clonePlainAttrValue(attr.getUniqueValue()));
                            } else if (attr.getValues() != null) {
                                attr.getValues().forEach(value -> values.add(clonePlainAttrValue(value)));
                            }
                        }
                    }

                    case DERIVED -> {
                        DerSchema derSchema = (DerSchema) intAttrName.getSchema();
                        String derValue = membership == null
                                ? derAttrHandler.getValue(ref, derSchema)
                                : derAttrHandler.getValue((Groupable<?, ?, ?>) ref, membership, derSchema);
                        if (derValue != null) {
                            PlainAttrValue attrValue = new PlainAttrValue();
                            attrValue.setStringValue(derValue);
                            values.add(attrValue);
                        }
                    }

                    default -> {
                    }
                }
            }
        }

        LOG.debug("Internal values: {}", values);

        IntValues transformed = new IntValues(schemaType, values);
        if (transform) {
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(item, getTransformers(item))) {
                transformed = transformer.beforePropagation(
                        item, any, transformed.attrSchemaType(), transformed.values());
            }
            LOG.debug("Transformed values: {}", values);
        } else {
            LOG.debug("No transformation occurred");
        }

        return transformed;
    }

    @Transactional(readOnly = true)
    @Override
    public IntValues getIntValues(
            final ExternalResource resource,
            final Item item,
            final IntAttrName intAttrName,
            final AttrSchemaType schemaType,
            final Realm realm) {

        LOG.debug("Get internal values for {} as '{}' on {}", realm, item.getIntAttrName(), resource);

        List<PlainAttrValue> values = new ArrayList<>();
        boolean transform = true;

        if (intAttrName.getField() != null) {
            PlainAttrValue attrValue = new PlainAttrValue();

            switch (intAttrName.getField()) {
                case "key" -> {
                    attrValue.setStringValue(realm.getKey());
                    values.add(attrValue);
                }

                case "name" -> {
                    attrValue.setStringValue(realm.getName());
                    values.add(attrValue);
                }

                case "fullPath" -> {
                    attrValue.setStringValue(realm.getFullPath());
                    values.add(attrValue);
                }

                default -> {
                }
            }
        } else if (intAttrName.getSchemaType() != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN -> {
                    realm.getPlainAttr(intAttrName.getSchema().getKey()).ifPresent(attr -> {
                        if (attr.getUniqueValue() != null) {
                            values.add(clonePlainAttrValue(attr.getUniqueValue()));
                        } else if (attr.getValues() != null) {
                            attr.getValues().forEach(value -> values.add(clonePlainAttrValue(value)));
                        }
                    });
                }

                case DERIVED -> {
                    Optional.ofNullable(derAttrHandler.getValue(realm, (DerSchema) intAttrName.getSchema())).
                            ifPresent(derValue -> {
                                PlainAttrValue attrValue = new PlainAttrValue();
                                attrValue.setStringValue(derValue);
                                values.add(attrValue);
                            });
                }

                default -> {
                }
            }
        }

        LOG.debug("Internal values: {}", values);

        IntValues transformed = new IntValues(schemaType, values);
        if (transform) {
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(item, getTransformers(item))) {
                transformed = transformer.beforePropagation(
                        item, realm, transformed.attrSchemaType(), transformed.values());
            }
            LOG.debug("Transformed values: {}", values);
        } else {
            LOG.debug("No transformation occurred");
        }

        return transformed;
    }

    protected String getGroupOwnerValue(
            final ExternalResource resource,
            final Provision provision,
            final Any any) {

        Optional<Item> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision);

        PreparedAttr preparedAttr = null;
        if (connObjectKeyItem.isPresent()) {
            preparedAttr = prepareAttr(
                    resource,
                    provision,
                    connObjectKeyItem.get(),
                    any,
                    null,
                    AccountGetter.DEFAULT,
                    AccountGetter.DEFAULT,
                    PlainAttrGetter.DEFAULT);
        }

        return Optional.ofNullable(preparedAttr).
                map(attr -> evaluateNAME(any, provision, attr.connObjectLink()).getNameValue()).orElse(null);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> getConnObjectKeyValue(
            final Any any,
            final ExternalResource resource,
            final Provision provision) {

        Optional<Item> connObjectKeyItem = provision.getMapping().getConnObjectKeyItem();
        if (connObjectKeyItem.isEmpty()) {
            LOG.error("Unable to locate conn object key item for {}", any.getType().getKey());
            return Optional.empty();
        }

        Item item = connObjectKeyItem.get();
        IntValues intValues;
        try {
            intValues = getIntValues(
                    resource,
                    provision,
                    item,
                    intAttrNameParser.parse(item.getIntAttrName(), any.getType().getKind()),
                    AttrSchemaType.String,
                    any,
                    AccountGetter.DEFAULT,
                    PlainAttrGetter.DEFAULT);
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            intValues = new IntValues(AttrSchemaType.String, List.of());
        }
        return intValues.values().isEmpty()
                ? Optional.empty()
                : Optional.of(intValues.values().getFirst().getValueAsString());
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<String> getConnObjectKeyValue(final Realm realm, final ExternalResource resource) {
        if (resource.getOrgUnit() == null) {
            LOG.error("No mapping configured for Realms");
            return Optional.empty();
        }

        Optional<Item> connObjectKeyItem = resource.getOrgUnit().getConnObjectKeyItem();
        if (connObjectKeyItem.isEmpty()) {
            LOG.error("Unable to locate conn object key item for Realms");
            return Optional.empty();
        }

        Item item = connObjectKeyItem.get();
        IntValues intValues;
        try {
            intValues = getIntValues(
                    resource,
                    item,
                    intAttrNameParser.parse(item.getIntAttrName()),
                    AttrSchemaType.String,
                    realm);
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            intValues = new IntValues(AttrSchemaType.String, List.of());
        }
        return intValues.values().isEmpty()
                ? Optional.empty()
                : Optional.of(intValues.values().getFirst().getValueAsString());
    }

    @Transactional(readOnly = true)
    @Override
    public void setIntValues(final Item item, final Attribute attr, final AnyTO anyTO) {
        List<Object> values = null;
        if (attr != null) {
            values = attr.getValue();
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(item, getTransformers(item))) {
                values = transformer.beforePull(item, anyTO, values);
            }
        }
        values = Optional.ofNullable(values).orElseGet(List::of);

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(item.getIntAttrName(), AnyTypeKind.fromTOClass(anyTO.getClass()));
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            return;
        }

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "password" -> {
                    if (anyTO instanceof UserTO && !values.isEmpty()) {
                        ((UserTO) anyTO).setPassword(ConnObjectUtils.getPassword(values.getFirst()));
                    }
                }

                case "username" -> {
                    if (anyTO instanceof UserTO userTO) {
                        userTO.setUsername(values.isEmpty() || values.getFirst() == null
                                ? null
                                : values.getFirst().toString());
                    }
                }

                case "name" -> {
                    switch (anyTO) {
                        case GroupTO groupTO ->
                            groupTO.setName(values.isEmpty() || values.getFirst() == null
                                    ? null
                                    : values.getFirst().toString());
                        case AnyObjectTO anyObjectTO ->
                            anyObjectTO.setName(values.isEmpty() || values.getFirst() == null
                                    ? null
                                    : values.getFirst().toString());
                        default -> {
                        }
                    }
                }

                case "mustChangePassword" -> {
                    if (anyTO instanceof UserTO && !values.isEmpty() && values.getFirst() != null) {
                        ((UserTO) anyTO).setMustChangePassword(BooleanUtils.toBoolean(values.getFirst().toString()));
                    }
                }

                case "userOwner", "groupOwner" -> {
                    if (anyTO instanceof final GroupTO groupTO && attr != null) {
                        // using a special attribute (with schema "", that will be ignored) for carrying the
                        // GroupOwnerSchema value
                        Attr attrTO = new Attr();
                        attrTO.setSchema(StringUtils.EMPTY);
                        if (values.isEmpty() || values.getFirst() == null) {
                            attrTO.getValues().add(StringUtils.EMPTY);
                        } else {
                            attrTO.getValues().add(values.getFirst().toString());
                        }

                        groupTO.getPlainAttrs().add(attrTO);
                    }
                }
                default -> {
                }
            }
        } else if (intAttrName.getSchemaType() != null && attr != null) {
            GroupableRelatableTO groupableTO;
            Group group;
            if (anyTO instanceof final GroupableRelatableTO groupableRelatableTO
                    && intAttrName.getMembershipOfGroup() != null) {
                groupableTO = groupableRelatableTO;
                group = groupDAO.findByName(intAttrName.getMembershipOfGroup()).orElse(null);
            } else {
                groupableTO = null;
                group = null;
            }

            switch (intAttrName.getSchemaType()) {
                case PLAIN -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(intAttrName.getSchema().getKey());

                    PlainSchema schema = (PlainSchema) intAttrName.getSchema();

                    for (Object value : values) {
                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            if (schemaType == AttrSchemaType.Binary) {
                                attrTO.getValues().add(Base64.getEncoder().encodeToString((byte[]) value));
                            } else {
                                attrTO.getValues().add(value.toString());
                            }
                        }
                    }

                    if (groupableTO == null || group == null) {
                        anyTO.getPlainAttrs().add(attrTO);
                    } else {
                        MembershipTO membership = groupableTO.getMembership(group.getKey()).orElseGet(() -> {
                            MembershipTO newMemb = new MembershipTO.Builder(group.getKey()).build();
                            groupableTO.getMemberships().add(newMemb);
                            return newMemb;
                        });
                        membership.getPlainAttrs().add(attrTO);
                    }
                }

                case DERIVED -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(intAttrName.getSchema().getKey());
                    if (groupableTO == null || group == null) {
                        anyTO.getDerAttrs().add(attrTO);
                    } else {
                        MembershipTO membership = groupableTO.getMembership(group.getKey()).orElseGet(() -> {
                            MembershipTO newMemb = new MembershipTO.Builder(group.getKey()).build();
                            groupableTO.getMemberships().add(newMemb);
                            return newMemb;
                        });
                        membership.getDerAttrs().add(attrTO);
                    }
                }

                default -> {
                }
            }
        }
    }

    @Override
    public void setIntValues(final Item item, final Attribute attr, final RealmTO realmTO) {
        List<Object> values = null;
        if (attr != null) {
            values = attr.getValue();
            for (ItemTransformer transformer : MappingUtils.getItemTransformers(item, getTransformers(item))) {
                values = transformer.beforePull(item, realmTO, values);
            }
        }
        values = Optional.ofNullable(values).orElseGet(List::of);

        IntAttrName intAttrName;
        try {
            intAttrName = intAttrNameParser.parse(item.getIntAttrName());
        } catch (ParseException e) {
            LOG.error("Invalid intAttrName '{}' specified, ignoring", item.getIntAttrName(), e);
            return;
        }

        if (intAttrName.getField() != null) {
            switch (intAttrName.getField()) {
                case "name" -> {
                    realmTO.setName(values.isEmpty() || values.getFirst() == null
                            ? null
                            : values.getFirst().toString());
                }

                case "fullpath" -> {
                    String parentFullPath = StringUtils.substringBeforeLast(values.getFirst().toString(), "/");
                    realmSearchDAO.findByFullPath(parentFullPath).ifPresentOrElse(
                            parent -> realmTO.setParent(parent.getFullPath()),
                            () -> LOG.warn("Could not find Realm with path {}, ignoring", parentFullPath));
                }

                default -> {
                }
            }
        } else if (intAttrName.getSchemaType() != null && attr != null) {
            switch (intAttrName.getSchemaType()) {
                case PLAIN -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(intAttrName.getSchema().getKey());

                    PlainSchema schema = (PlainSchema) intAttrName.getSchema();

                    for (Object value : values) {
                        AttrSchemaType schemaType = schema == null ? AttrSchemaType.String : schema.getType();
                        if (value != null) {
                            if (schemaType == AttrSchemaType.Binary) {
                                attrTO.getValues().add(Base64.getEncoder().encodeToString((byte[]) value));
                            } else {
                                attrTO.getValues().add(value.toString());
                            }
                        }
                    }

                    realmTO.getPlainAttrs().add(attrTO);
                }

                case DERIVED -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(intAttrName.getSchema().getKey());
                    realmTO.getDerAttrs().add(attrTO);
                }

                default -> {
                }
            }
        }
    }

    @Override
    public boolean hasMustChangePassword(final Provision provision) {
        return Optional.ofNullable(provision.getMapping()).
                map(mapping -> mapping.getItems().stream().
                anyMatch(item -> "mustChangePassword".equals(item.getIntAttrName()))).
                orElse(false);
    }
}
