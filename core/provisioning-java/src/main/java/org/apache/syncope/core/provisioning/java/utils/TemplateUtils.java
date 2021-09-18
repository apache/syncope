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
import java.util.Optional;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.RealmMember;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.springframework.transaction.annotation.Transactional;

public class TemplateUtils {

    protected static Attr evaluateAttr(final Attr template, final MapContext jexlContext) {
        Attr result = new Attr();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            template.getValues().forEach(value -> {
                String evaluated = JexlUtils.evaluate(value, jexlContext);
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
            String evaluated = JexlUtils.evaluate(template.getRealm(), jexlContext);
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
                    jexlContext.set(evaluated.getSchema(), evaluated.getValues().get(0));
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
                    jexlContext.set(evaluated.getSchema(), evaluated.getValues().get(0));
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
    public void apply(final RealmMember realmMember, final Optional<? extends AnyTemplate> template) {
        template.ifPresent(anyTemplate -> apply(realmMember, anyTemplate.get()));
    }

    @Transactional(readOnly = true)
    public void apply(final RealmMember realmMember, final AnyTO template) {
        fill(realmMember, template);

        MapContext jexlContext = new MapContext();
        JexlUtils.addFieldsToContext(realmMember, jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getPlainAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getDerAttrs(), jexlContext);
        JexlUtils.addAttrsToContext(realmMember.getVirAttrs(), jexlContext);

        if (template instanceof AnyObjectTO) {
            fillRelationships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));
            fillMemberships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));
        } else if (template instanceof UserTO) {
            if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                String evaluated = JexlUtils.evaluate(((UserTO) template).getUsername(), jexlContext);
                if (StringUtils.isNotBlank(evaluated)) {
                    if (realmMember instanceof UserTO) {
                        ((UserTO) realmMember).setUsername(evaluated);
                    } else if (realmMember instanceof UserCR) {
                        ((UserCR) realmMember).setUsername(evaluated);
                    }
                }
            }

            if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                String evaluated = JexlUtils.evaluate(((UserTO) template).getPassword(), jexlContext);
                if (StringUtils.isNotBlank(evaluated)) {
                    if (realmMember instanceof UserTO) {
                        ((UserTO) realmMember).setPassword(evaluated);
                    } else if (realmMember instanceof UserCR) {
                        ((UserCR) realmMember).setPassword(evaluated);
                    }
                }
            }

            fillRelationships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));
            fillMemberships((GroupableRelatableTO) realmMember, ((GroupableRelatableTO) template));
            if (realmMember instanceof UserTO) {
                ((UserTO) realmMember).getRoles().addAll(((UserTO) template).getRoles());
            } else if (realmMember instanceof UserCR) {
                ((UserCR) realmMember).getRoles().addAll(((UserTO) template).getRoles());
            }
        } else if (template instanceof GroupTO) {
            if (StringUtils.isNotBlank(((GroupTO) template).getName())) {
                String evaluated = JexlUtils.evaluate(((GroupTO) template).getName(), jexlContext);
                if (StringUtils.isNotBlank(evaluated)) {
                    if (realmMember instanceof GroupTO) {
                        ((GroupTO) realmMember).setName(evaluated);
                    } else if (realmMember instanceof GroupCR) {
                        ((GroupCR) realmMember).setName(evaluated);
                    }
                }
            }

            if (((GroupTO) template).getUserOwner() != null) {
                final User userOwner = userDAO.find(((GroupTO) template).getUserOwner());
                if (userOwner != null) {
                    if (realmMember instanceof GroupTO) {
                        ((GroupTO) realmMember).setUserOwner(userOwner.getKey());
                    } else if (realmMember instanceof GroupCR) {
                        ((GroupCR) realmMember).setUserOwner(userOwner.getKey());
                    }
                }
            }
            if (((GroupTO) template).getGroupOwner() != null) {
                final Group groupOwner = groupDAO.find(((GroupTO) template).getGroupOwner());
                if (groupOwner != null) {
                    if (realmMember instanceof GroupTO) {
                        ((GroupTO) realmMember).setGroupOwner(groupOwner.getKey());
                    } else if (realmMember instanceof GroupCR) {
                        ((GroupCR) realmMember).setGroupOwner(groupOwner.getKey());
                    }
                }
            }
        }
    }

    public static void check(final Map<String, AnyTO> templates, final ClientExceptionType clientExceptionType) {
        SyncopeClientException sce = SyncopeClientException.build(clientExceptionType);

        templates.values().forEach(value -> {
            value.getPlainAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().get(0))).
                    forEachOrdered(attrTO -> sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0)));

            value.getVirAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().get(0))).
                    forEachOrdered((attrTO) -> sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0)));

            if (value instanceof UserTO) {
                UserTO template = (UserTO) value;
                if (StringUtils.isNotBlank(template.getUsername())
                        && !JexlUtils.isExpressionValid(template.getUsername())) {

                    sce.getElements().add("Invalid JEXL: " + template.getUsername());
                }
                if (StringUtils.isNotBlank(template.getPassword())
                        && !JexlUtils.isExpressionValid(template.getPassword())) {

                    sce.getElements().add("Invalid JEXL: " + template.getPassword());
                }
            } else if (value instanceof GroupTO) {
                GroupTO template = (GroupTO) value;
                if (StringUtils.isNotBlank(template.getName())
                        && !JexlUtils.isExpressionValid(template.getName())) {

                    sce.getElements().add("Invalid JEXL: " + template.getName());
                }
            }
        });

        if (!sce.isEmpty()) {
            throw sce;
        }
    }
}
