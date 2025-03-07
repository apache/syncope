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
package org.apache.syncope.core.provisioning.java.utils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.RealmMember;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.springframework.transaction.annotation.Transactional;

public class TemplateUtils {

    public static void check(final Map<String, AnyTO> templates, final ClientExceptionType clientExceptionType) {
        SyncopeClientException sce = SyncopeClientException.build(clientExceptionType);

        templates.values().forEach(value -> {
            value.getPlainAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().getFirst())).
                    forEachOrdered(attrTO -> sce.getElements().add("Invalid JEXL: " + attrTO.getValues().getFirst()));

            value.getVirAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().getFirst())).
                    forEachOrdered((attrTO) -> sce.getElements().add("Invalid JEXL: " + attrTO.getValues().getFirst()));

            switch (value) {
                case UserTO template -> {
                    if (StringUtils.isNotBlank(template.getUsername())
                            && !JexlUtils.isExpressionValid(template.getUsername())) {

                        sce.getElements().add("Invalid JEXL: " + template.getUsername());
                    }
                    if (StringUtils.isNotBlank(template.getPassword())
                            && !JexlUtils.isExpressionValid(template.getPassword())) {

                        sce.getElements().add("Invalid JEXL: " + template.getPassword());
                    }
                }
                case GroupTO template -> {
                    if (StringUtils.isNotBlank(template.getName())
                            && !JexlUtils.isExpressionValid(template.getName())) {

                        sce.getElements().add("Invalid JEXL: " + template.getName());
                    }
                }
                default -> {
                }
            }
        });

        if (!sce.isEmpty()) {
            throw sce;
        }
    }

    protected static Attr evaluateAttr(final Attr template, final MapContext jexlContext) {
        Attr result = new Attr();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            template.getValues().forEach(value -> {
                String evaluated = JexlUtils.evaluateExpr(value, jexlContext).toString();
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            });
        }

        return result;
    }

    protected static void fill(final RealmMember realmMember, final RealmMember template) {
        MapContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(realmMember, jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getPlainAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getDerAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getVirAttrs(), jexlContext);

        if (template.getRealm() != null) {
            String evaluated = JexlUtils.evaluateExpr(template.getRealm(), jexlContext).toString();
            if (StringUtils.isNotBlank(evaluated)) {
                realmMember.setRealm(evaluated);
            }
        }

        Map<String, Attr> currentAttrMap = EntityTOUtils.buildAttrMap(realmMember.getPlainAttrs());
        for (Attr templatePlainAttr : template.getPlainAttrs()) {
            if (!templatePlainAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templatePlainAttr.getSchema())
                    || currentAttrMap.get(templatePlainAttr.getSchema()).getValues().isEmpty())) {

                Attr evaluated = evaluateAttr(templatePlainAttr, jexlContext);
                if (!evaluated.getValues().isEmpty()) {
                    realmMember.getPlainAttrs().add(evaluated);
                    jexlContext.set(evaluated.getSchema(), evaluated.getValues().getFirst());
                }
            }
        }

        currentAttrMap = EntityTOUtils.buildAttrMap(realmMember.getDerAttrs());
        for (Attr templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                realmMember.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = EntityTOUtils.buildAttrMap(realmMember.getVirAttrs());
        for (Attr templateVirAttr : template.getVirAttrs()) {
            if (!templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                Attr evaluated = evaluateAttr(templateVirAttr, jexlContext);
                if (!evaluated.getValues().isEmpty()) {
                    realmMember.getVirAttrs().add(evaluated);
                    jexlContext.set(evaluated.getSchema(), evaluated.getValues().getFirst());
                }
            }
        }

        realmMember.getResources().addAll(template.getResources());

        realmMember.getAuxClasses().addAll(template.getAuxClasses());
    }

    protected static void fillRelationships(final GroupableRelatableTO any, final GroupableRelatableTO template) {
        template.getRelationships().stream().
                filter(relationship -> any.getRelationship(
                relationship.getOtherEndKey(), relationship.getOtherEndKey()).isEmpty()).
                forEachOrdered(relationship -> any.getRelationships().add(relationship));
    }

    protected static void fillMemberships(final GroupableRelatableTO any, final GroupableRelatableTO template) {
        template.getMemberships().stream().
                filter(membership -> any.getMembership(membership.getGroupKey()).isEmpty()).
                forEachOrdered(membership -> any.getMemberships().add(membership));
    }

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    public TemplateUtils(final UserDAO userDAO, final GroupDAO groupDAO) {
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
    }

    @Transactional(readOnly = true)
    public void apply(final RealmMember realmMember, final AnyTO template) {
        fill(realmMember, template);

        MapContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(realmMember, jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getPlainAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getDerAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getVirAttrs(), jexlContext);

        switch (template) {
            case AnyObjectTO anyObjectTO -> {
                fillRelationships((GroupableRelatableTO) realmMember, anyObjectTO);
                fillMemberships((GroupableRelatableTO) realmMember, anyObjectTO);
            }

            case UserTO userTO -> {
                if (StringUtils.isNotBlank(userTO.getUsername())) {
                    String evaluated = JexlUtils.evaluateExpr(userTO.getUsername(), jexlContext).toString();
                    if (StringUtils.isNotBlank(evaluated)) {
                        switch (realmMember) {
                            case UserTO urm ->
                                urm.setUsername(evaluated);
                            case UserCR urm ->
                                urm.setUsername(evaluated);
                            default -> {
                            }
                        }
                    }
                }

                if (StringUtils.isNotBlank(userTO.getPassword())) {
                    String evaluated = JexlUtils.evaluateExpr(userTO.getPassword(), jexlContext).toString();
                    if (StringUtils.isNotBlank(evaluated)) {
                        switch (realmMember) {
                            case UserTO urm ->
                                urm.setPassword(evaluated);
                            case UserCR urm ->
                                urm.setPassword(evaluated);
                            default -> {
                            }
                        }
                    }
                }

                if (userTO.isMustChangePassword()) {
                    switch (realmMember) {
                        case UserTO urm ->
                            urm.setMustChangePassword(true);
                        case UserCR urm ->
                            urm.setMustChangePassword(true);
                        default -> {
                        }
                    }
                }

                fillRelationships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));
                fillMemberships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));

                userTO.getRoles().
                    forEach(role -> {
                        if (realmMember instanceof UserTO urm) {
                            urm.getRoles().add(role);
                        } else if (realmMember instanceof UserCR urm) {
                            urm.getRoles().add(role);
                        }
                });

                userTO.getLinkedAccounts().forEach(account -> {
                    if (realmMember instanceof UserTO urm && urm.getLinkedAccounts().stream().
                            noneMatch(a -> Objects.equals(account.getConnObjectKeyValue(), a.getConnObjectKeyValue())
                            && Objects.equals(account.getResource(), a.getResource()))) {

                        urm.getLinkedAccounts().add(account);
                    } else if (realmMember instanceof UserCR urm && urm.getLinkedAccounts().stream().
                            noneMatch(a -> Objects.equals(account.getConnObjectKeyValue(), a.getConnObjectKeyValue())
                            && Objects.equals(account.getResource(), a.getResource()))) {

                        urm.getLinkedAccounts().add(account);
                    }
                });
            }

            case GroupTO groupTO -> {
                if (StringUtils.isNotBlank(groupTO.getName())) {
                    String evaluated = JexlUtils.evaluateExpr(groupTO.getName(), jexlContext).toString();
                    if (StringUtils.isNotBlank(evaluated)) {
                        switch (realmMember) {
                            case GroupTO grm ->
                                grm.setName(evaluated);
                            case GroupCR grm ->
                                grm.setName(evaluated);
                            default -> {
                            }
                        }
                    }
                }

                Optional.ofNullable(groupTO.getUserOwner()).flatMap(userDAO::findById).ifPresent(userOwner -> {
                    switch (realmMember) {
                        case GroupTO grm ->
                            grm.setUserOwner(userOwner.getKey());
                        case GroupCR grm ->
                            grm.setUserOwner(userOwner.getKey());
                        default -> {
                        }
                    }
                });
                Optional.ofNullable(groupTO.getGroupOwner()).flatMap(groupDAO::findById).ifPresent(groupOwner -> {
                    switch (realmMember) {
                        case GroupTO grm ->
                            grm.setGroupOwner(groupOwner.getKey());
                        case GroupCR grm ->
                            grm.setGroupOwner(groupOwner.getKey());
                        default -> {
                        }
                    }
                });

                Optional.ofNullable(groupTO.getUDynMembershipCond()).ifPresent(udynMembershipCond -> {
                    switch (realmMember) {
                        case GroupTO grm ->
                            grm.setUDynMembershipCond(udynMembershipCond);
                        case GroupCR grm ->
                            grm.setUDynMembershipCond(udynMembershipCond);
                        default -> {
                        }
                    }
                });

                groupTO.getADynMembershipConds().forEach((anyType, cond) -> {
                    if (realmMember instanceof GroupTO grm && grm.getADynMembershipConds().containsKey(anyType)) {
                        grm.getADynMembershipConds().put(anyType, cond);
                    } else if (realmMember instanceof GroupCR grm
                            && !grm.getADynMembershipConds().containsKey(anyType)) {

                        grm.getADynMembershipConds().put(anyType, cond);
                    }
                });

                groupTO.getTypeExtensions().forEach(typeExt -> {
                    if (realmMember instanceof GroupTO grm && !grm.getTypeExtensions().contains(typeExt)) {
                        grm.getTypeExtensions().add(typeExt);
                    } else if (realmMember instanceof GroupCR grm && !grm.getTypeExtensions().contains(typeExt)) {
                        grm.getTypeExtensions().add(typeExt);
                    }
                });
            }

            default -> {
            }
        }
    }
}
