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
package org.apache.syncope.core.provisioning.api.jexl;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.jexl3.JexlContext;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class TemplateUtils {

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

    protected final JexlTools jexlTools;

    public TemplateUtils(final UserDAO userDAO, final GroupDAO groupDAO, final JexlTools jexlTools) {
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.jexlTools = jexlTools;
    }

    public void check(final Map<String, AnyTO> templates, final ClientExceptionType clientExceptionType) {
        SyncopeClientException sce = SyncopeClientException.build(clientExceptionType);

        templates.values().forEach(value -> {
            value.getPlainAttrs().stream().
                    filter(attr -> !attr.getValues().isEmpty()
                    && !jexlTools.isExpressionValid(attr.getValues().getFirst())).
                    forEach(attr -> sce.getElements().add("Invalid JEXL: " + attr.getValues().getFirst()));

            switch (value) {
                case UserTO template -> {
                    if (template.getUsername() != null && !jexlTools.isExpressionValid(template.getUsername())) {
                        sce.getElements().add("Invalid JEXL: " + template.getUsername());
                    }
                    if (template.getPassword() != null && !jexlTools.isExpressionValid(template.getPassword())) {
                        sce.getElements().add("Invalid JEXL: " + template.getPassword());
                    }
                }
                case GroupTO template -> {
                    if (template.getName() != null && !jexlTools.isExpressionValid(template.getName())) {
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

    protected Attr evaluateAttr(final Attr template, final JexlContext jexlContext) {
        Attr result = new Attr();
        result.setSchema(template.getSchema());

        if (!CollectionUtils.isEmpty(template.getValues())) {
            template.getValues().forEach(value -> {
                String evaluated = jexlTools.evaluateExpression(value, jexlContext).toString();
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            });
        }

        return result;
    }

    protected void fill(final RealmMember realmMember, final RealmMember template) {
        JexlContext jexlContext = new JexlContextBuilder().
                fields(realmMember).
                attrs(realmMember.getPlainAttrs()).
                attrs(realmMember.getDerAttrs()).
                build();

        if (template.getRealm() != null) {
            String evaluated = jexlTools.evaluateExpression(template.getRealm(), jexlContext).toString();
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

        realmMember.getResources().addAll(template.getResources());

        realmMember.getAuxClasses().addAll(template.getAuxClasses());
    }

    @Transactional(readOnly = true)
    public void apply(final RealmMember realmMember, final AnyTO template) {
        fill(realmMember, template);

        JexlContext jexlContext = new JexlContextBuilder().
                fields(realmMember).
                attrs(realmMember.getPlainAttrs()).
                attrs(realmMember.getDerAttrs()).
                build();

        switch (template) {
            case AnyObjectTO anyObjectTO -> {
                fillRelationships((GroupableRelatableTO) realmMember, anyObjectTO);
                fillMemberships((GroupableRelatableTO) realmMember, anyObjectTO);
            }

            case UserTO userTO -> {
                if (StringUtils.isNotBlank(userTO.getUsername())) {
                    String evaluated = jexlTools.evaluateExpression(userTO.getUsername(), jexlContext).toString();
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
                    String evaluated = jexlTools.evaluateExpression(userTO.getPassword(), jexlContext).toString();
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
                    String evaluated = jexlTools.evaluateExpression(groupTO.getName(), jexlContext).toString();
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
