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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.java.data.JEXLItemTransformerImpl;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.apache.syncope.core.provisioning.api.data.ItemTransformer;
import org.apache.syncope.core.provisioning.api.data.JEXLItemTransformer;

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    public static Optional<MappingItem> getConnObjectKeyItem(final Provision provision) {
        Mapping mapping = null;
        if (provision != null) {
            mapping = provision.getMapping();
        }

        return Optional.ofNullable(mapping == null
                ? null
                : mapping.getConnObjectKeyItem().get());
    }

    public static List<? extends Item> getPropagationItems(final List<? extends Item> items) {
        return items.stream().
                        filter(item -> item.getPurpose() == MappingPurpose.PROPAGATION
                        || item.getPurpose() == MappingPurpose.BOTH).collect(Collectors.toList());
    }

    public static List<? extends Item> getPullItems(final List<? extends Item> items) {
        return items.stream().
                        filter(item -> item.getPurpose() == MappingPurpose.PULL
                        || item.getPurpose() == MappingPurpose.BOTH).collect(Collectors.toList());
    }

    private static Name evaluateNAME(final String evalConnObjectLink, final String connObjectKey) {
        // If connObjectLink evaluates to an empty string, just use the provided connObjectKey as Name(),
        // otherwise evaluated connObjectLink expression is taken as Name().
        Name name;
        if (StringUtils.isBlank(evalConnObjectLink)) {
            // add connObjectKey as __NAME__ attribute ...
            LOG.debug("Add connObjectKey [{}] as __NAME__", connObjectKey);
            name = new Name(connObjectKey);
        } else {
            LOG.debug("Add connObjectLink [{}] as __NAME__", evalConnObjectLink);
            name = new Name(evalConnObjectLink);

            // connObjectKey not propagated: it will be used to set the value for __UID__ attribute
            LOG.debug("connObjectKey will be used just as __UID__ attribute");
        }

        return name;
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
    public static Name evaluateNAME(final Any<?> any, final Provision provision, final String connObjectKey) {
        if (StringUtils.isBlank(connObjectKey)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing ConnObjectKey for '{}': ", provision.getResource());
        }

        // Evaluate connObjectKey expression
        String connObjectLink = provision == null || provision.getMapping() == null
                ? null
                : provision.getMapping().getConnObjectLink();
        String evalConnObjectLink = null;
        if (StringUtils.isNotBlank(connObjectLink)) {
            JexlContext jexlContext = new MapContext();
            JexlUtils.addFieldsToContext(any, jexlContext);
            JexlUtils.addPlainAttrsToContext(any.getPlainAttrs(), jexlContext);
            JexlUtils.addDerAttrsToContext(any, jexlContext);
            evalConnObjectLink = JexlUtils.evaluate(connObjectLink, jexlContext);
        }

        return evaluateNAME(evalConnObjectLink, connObjectKey);
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
    public static Name evaluateNAME(final Realm realm, final OrgUnit orgUnit, final String connObjectKey) {
        if (StringUtils.isBlank(connObjectKey)) {
            // LOG error but avoid to throw exception: leave it to the external resource
            LOG.error("Missing ConnObjectKey for '{}': ", orgUnit.getResource());
        }

        // Evaluate connObjectKey expression
        String connObjectLink = orgUnit == null
                ? null
                : orgUnit.getConnObjectLink();
        String evalConnObjectLink = null;
        if (StringUtils.isNotBlank(connObjectLink)) {
            JexlContext jexlContext = new MapContext();
            JexlUtils.addFieldsToContext(realm, jexlContext);
            evalConnObjectLink = JexlUtils.evaluate(connObjectLink, jexlContext);
        }

        return evaluateNAME(evalConnObjectLink, connObjectKey);
    }

    private static List<ItemTransformer> getItemTransformers(
            final String propagationJEXLTransformer,
            final String pullJEXLTransformer,
            final List<String> mappingItemTransformerClassNames) {

        List<ItemTransformer> result = new ArrayList<>();

        // First consider the JEXL transformation expressions
        if (StringUtils.isNotBlank(propagationJEXLTransformer) || StringUtils.isNotBlank(pullJEXLTransformer)) {
            JEXLItemTransformer jexlTransformer =
                    (JEXLItemTransformer) ApplicationContextProvider.getBeanFactory().
                            createBean(JEXLItemTransformerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME,
                                    false);

            jexlTransformer.setPropagationJEXL(propagationJEXLTransformer);
            jexlTransformer.setPullJEXL(pullJEXLTransformer);
            result.add(jexlTransformer);
        }

        // Then other custom tranaformers
        mappingItemTransformerClassNames.forEach(className -> {
            try {
                Class<?> transformerClass = ClassUtils.getClass(className);

                result.add((ItemTransformer) ApplicationContextProvider.getBeanFactory().
                        createBean(transformerClass, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false));
            } catch (Exception e) {
                LOG.error("Could not instantiate {}, ignoring...", className, e);
            }
        });

        return result;
    }

    public static List<ItemTransformer> getItemTransformers(final ItemTO item) {
        return getItemTransformers(
                item.getPropagationJEXLTransformer(),
                item.getPullJEXLTransformer(),
                item.getTransformerClassNames());
    }

    public static List<ItemTransformer> getItemTransformers(final Item item) {
        return getItemTransformers(
                item.getPropagationJEXLTransformer(),
                item.getPullJEXLTransformer(),
                item.getTransformerClassNames());
    }

    /**
     * Build options for requesting all mapped connector attributes.
     *
     * @param iterator items
     * @return options for requesting all mapped connector attributes
     * @see OperationOptions
     */
    public static OperationOptions buildOperationOptions(final Iterator<? extends Item> iterator) {
        OperationOptionsBuilder builder = new OperationOptionsBuilder();

        Set<String> attrsToGet = new HashSet<>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);

        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.getPurpose() != MappingPurpose.NONE) {
                attrsToGet.add(item.getExtAttrName());
            }
        }

        builder.setAttributesToGet(attrsToGet);
        // -------------------------------------

        return builder.build();
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private MappingUtils() {
    }
}
