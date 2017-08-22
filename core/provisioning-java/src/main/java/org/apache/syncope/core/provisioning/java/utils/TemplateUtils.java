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
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyTemplate;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TemplateUtils {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    private AttrTO evaluateAttr(final AnyTO anyTO, final AttrTO template) {
        AttrTO result = new AttrTO();
        result.setSchema(template.getSchema());

        if (template.getValues() != null && !template.getValues().isEmpty()) {
            template.getValues().forEach(value -> {
                String evaluated = JexlUtils.evaluate(value, anyTO, new MapContext());
                if (StringUtils.isNotBlank(evaluated)) {
                    result.getValues().add(evaluated);
                }
            });
        }

        return result;
    }

    private void fill(final AnyTO anyTO, final AnyTO template) {
        if (template.getRealm() != null) {
            String evaluated = JexlUtils.evaluate(template.getRealm(), anyTO, new MapContext());
            if (StringUtils.isNotBlank(evaluated)) {
                anyTO.setRealm(evaluated);
            }
        }

        Map<String, AttrTO> currentAttrMap = EntityTOUtils.buildAttrMap(anyTO.getPlainAttrs());
        for (AttrTO templatePlainAttr : template.getPlainAttrs()) {
            if (!templatePlainAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templatePlainAttr.getSchema())
                    || currentAttrMap.get(templatePlainAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getPlainAttrs().add(evaluateAttr(anyTO, templatePlainAttr));
            }
        }

        currentAttrMap = EntityTOUtils.buildAttrMap(anyTO.getDerAttrs());
        for (AttrTO templateDerAttr : template.getDerAttrs()) {
            if (!currentAttrMap.containsKey(templateDerAttr.getSchema())) {
                anyTO.getDerAttrs().add(templateDerAttr);
            }
        }

        currentAttrMap = EntityTOUtils.buildAttrMap(anyTO.getVirAttrs());
        for (AttrTO templateVirAttr : template.getVirAttrs()) {
            if (!templateVirAttr.getValues().isEmpty()
                    && (!currentAttrMap.containsKey(templateVirAttr.getSchema())
                    || currentAttrMap.get(templateVirAttr.getSchema()).getValues().isEmpty())) {

                anyTO.getVirAttrs().add(evaluateAttr(anyTO, templateVirAttr));
            }
        }

        anyTO.getResources().addAll(template.getResources());

        anyTO.getAuxClasses().addAll(template.getAuxClasses());
    }

    private void fillRelationships(final GroupableRelatableTO any, final GroupableRelatableTO template) {
        template.getRelationships().stream().
                filter(relationship
                        -> !any.getRelationship(relationship.getRightKey(), relationship.getRightKey()).isPresent()).
                forEachOrdered(relationship -> {
                    any.getRelationships().add(relationship);
                });
    }

    private void fillMemberships(final GroupableRelatableTO any, final GroupableRelatableTO template) {
        template.getMemberships().stream().
                filter(membership -> !any.getMembership(membership.getGroupKey()).isPresent()).
                forEachOrdered(membership -> {
                    any.getMemberships().add(membership);
                });
    }

    @Transactional(readOnly = true)
    public <T extends AnyTO> void apply(final T anyTO, final Optional<? extends AnyTemplate> anyTemplate) {
        if (anyTemplate.isPresent()) {
            apply(anyTO, anyTemplate.get().get());
        }
    }

    @Transactional(readOnly = true)
    public <T extends AnyTO> void apply(final T anyTO, final AnyTO template) {
        fill(anyTO, template);

        if (template instanceof AnyObjectTO) {
            fillRelationships((GroupableRelatableTO) anyTO, ((GroupableRelatableTO) template));
            fillMemberships((GroupableRelatableTO) anyTO, ((GroupableRelatableTO) template));
        } else if (template instanceof UserTO) {
            if (StringUtils.isNotBlank(((UserTO) template).getUsername())) {
                String evaluated = JexlUtils.evaluate(((UserTO) template).getUsername(), anyTO, new MapContext());
                if (StringUtils.isNotBlank(evaluated)) {
                    ((UserTO) anyTO).setUsername(evaluated);
                }
            }

            if (StringUtils.isNotBlank(((UserTO) template).getPassword())) {
                String evaluated = JexlUtils.evaluate(((UserTO) template).getPassword(), anyTO, new MapContext());
                if (StringUtils.isNotBlank(evaluated)) {
                    ((UserTO) anyTO).setPassword(evaluated);
                }
            }

            fillRelationships((GroupableRelatableTO) anyTO, ((GroupableRelatableTO) template));
            fillMemberships((GroupableRelatableTO) anyTO, ((GroupableRelatableTO) template));
        } else if (template instanceof GroupTO) {
            if (StringUtils.isNotBlank(((GroupTO) template).getName())) {
                String evaluated = JexlUtils.evaluate(((GroupTO) template).getName(), anyTO, new MapContext());
                if (StringUtils.isNotBlank(evaluated)) {
                    ((GroupTO) anyTO).setName(evaluated);
                }
            }

            if (((GroupTO) template).getUserOwner() != null) {
                final User userOwner = userDAO.find(((GroupTO) template).getUserOwner());
                if (userOwner != null) {
                    ((GroupTO) anyTO).setUserOwner(userOwner.getKey());
                }
            }
            if (((GroupTO) template).getGroupOwner() != null) {
                final Group groupOwner = groupDAO.find(((GroupTO) template).getGroupOwner());
                if (groupOwner != null) {
                    ((GroupTO) anyTO).setGroupOwner(groupOwner.getKey());
                }
            }
        }
    }

    public void check(final Map<String, AnyTO> templates, final ClientExceptionType clientExceptionType) {
        SyncopeClientException sce = SyncopeClientException.build(clientExceptionType);

        templates.entrySet().forEach(entry -> {
            entry.getValue().getPlainAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().get(0))).
                    forEachOrdered(attrTO -> {
                        sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
                    });

            entry.getValue().getVirAttrs().stream().
                    filter(attrTO -> !attrTO.getValues().isEmpty()
                    && !JexlUtils.isExpressionValid(attrTO.getValues().get(0))).
                    forEachOrdered((attrTO) -> {
                        sce.getElements().add("Invalid JEXL: " + attrTO.getValues().get(0));
                    });

            if (entry.getValue() instanceof UserTO) {
                UserTO template = (UserTO) entry.getValue();
                if (StringUtils.isNotBlank(template.getUsername())
                        && !JexlUtils.isExpressionValid(template.getUsername())) {

                    sce.getElements().add("Invalid JEXL: " + template.getUsername());
                }
                if (StringUtils.isNotBlank(template.getPassword())
                        && !JexlUtils.isExpressionValid(template.getPassword())) {

                    sce.getElements().add("Invalid JEXL: " + template.getPassword());
                }
            } else if (entry.getValue() instanceof GroupTO) {
                GroupTO template = (GroupTO) entry.getValue();
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
