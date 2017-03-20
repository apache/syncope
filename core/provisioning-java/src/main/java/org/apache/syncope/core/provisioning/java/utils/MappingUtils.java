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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.MappingItemTO;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.data.JEXLMappingItemTransformer;
import org.apache.syncope.core.provisioning.api.data.MappingItemTransformer;
import org.apache.syncope.core.provisioning.java.data.JEXLMappingItemTransformerImpl;
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

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    public static MappingItem getConnObjectKeyItem(final Provision provision) {
        Mapping mapping = null;
        if (provision != null) {
            mapping = provision.getMapping();
        }

        return mapping == null
                ? null
                : mapping.getConnObjectKeyItem();
    }

    private static List<MappingItem> getMappingItems(final Provision provision, final MappingPurpose purpose) {
        List<? extends MappingItem> items = Collections.<MappingItem>emptyList();
        if (provision != null) {
            items = provision.getMapping().getItems();
        }

        List<MappingItem> result = new ArrayList<>();

        switch (purpose) {
            case PULL:
                for (MappingItem item : items) {
                    if (MappingPurpose.PROPAGATION != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case PROPAGATION:
                for (MappingItem item : items) {
                    if (MappingPurpose.PULL != item.getPurpose()
                            && MappingPurpose.NONE != item.getPurpose()) {

                        result.add(item);
                    }
                }
                break;

            case BOTH:
                for (MappingItem item : items) {
                    if (MappingPurpose.NONE != item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;

            case NONE:
                for (MappingItem item : items) {
                    if (MappingPurpose.NONE == item.getPurpose()) {
                        result.add(item);
                    }
                }
                break;

            default:
        }

        return result;
    }

    public static List<MappingItem> getPropagationMappingItems(final Provision provision) {
        return getMappingItems(provision, MappingPurpose.PROPAGATION);
    }

    public static List<MappingItem> getPullMappingItems(final Provision provision) {
        return getMappingItems(provision, MappingPurpose.PULL);
    }

    /**
     * Build __NAME__ for propagation. First look if there ia a defined connObjectLink for the given resource (and in
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

    private static List<MappingItemTransformer> getMappingItemTransformers(
            final String propagationJEXLTransformer,
            final String pullJEXLTransformer,
            final List<String> mappingItemTransformerClassNames) {

        List<MappingItemTransformer> result = new ArrayList<>();

        // First consider the JEXL transformation expressions
        if (StringUtils.isNotBlank(propagationJEXLTransformer) || StringUtils.isNotBlank(pullJEXLTransformer)) {
            JEXLMappingItemTransformer jexlTransformer =
                    (JEXLMappingItemTransformer) ApplicationContextProvider.getBeanFactory().
                            createBean(JEXLMappingItemTransformerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME,
                                    false);

            jexlTransformer.setPropagationJEXL(propagationJEXLTransformer);
            jexlTransformer.setPullJEXL(pullJEXLTransformer);
            result.add(jexlTransformer);
        }

        // Then other custom tranaformers
        for (String className : mappingItemTransformerClassNames) {
            try {
                Class<?> transformerClass = ClassUtils.getClass(className);

                result.add((MappingItemTransformer) ApplicationContextProvider.getBeanFactory().
                        createBean(transformerClass, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false));
            } catch (Exception e) {
                LOG.error("Could not instantiate {}, ignoring...", className, e);
            }
        }

        return result;
    }

    public static List<MappingItemTransformer> getMappingItemTransformers(final MappingItemTO mappingItem) {
        return getMappingItemTransformers(
                mappingItem.getPropagationJEXLTransformer(),
                mappingItem.getPullJEXLTransformer(),
                mappingItem.getMappingItemTransformerClassNames());
    }

    public static List<MappingItemTransformer> getMappingItemTransformers(final MappingItem mappingItem) {
        return getMappingItemTransformers(
                mappingItem.getPropagationJEXLTransformer(),
                mappingItem.getPullJEXLTransformer(),
                mappingItem.getMappingItemTransformerClassNames());
    }

    /**
     * Build options for requesting all mapped connector attributes.
     *
     * @param mapItems mapping items
     * @return options for requesting all mapped connector attributes
     * @see OperationOptions
     */
    public static OperationOptions buildOperationOptions(final Iterator<? extends MappingItem> mapItems) {
        OperationOptionsBuilder builder = new OperationOptionsBuilder();

        Set<String> attrsToGet = new HashSet<>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);

        while (mapItems.hasNext()) {
            MappingItem mapItem = mapItems.next();
            if (mapItem.getPurpose() != MappingPurpose.NONE) {
                attrsToGet.add(mapItem.getExtAttrName());
            }
        }

        builder.setAttributesToGet(attrsToGet);
        // -------------------------------------

        return builder.build();
    }

    /**
     * Build options for requesting connector attributes for the given orgUnit.
     *
     * @param orgUnit orgUnit
     * @return options for requesting connector attributes for the given orgUnit
     * @see OperationOptions
     */
    public static OperationOptions buildOperationOptions(final OrgUnit orgUnit) {
        return new OperationOptionsBuilder().setAttributesToGet(Name.NAME, Uid.NAME, orgUnit.getExtAttrName()).build();
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private MappingUtils() {
    }
}
