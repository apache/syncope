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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.resource.Item;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.java.data.JEXLItemTransformerImpl;
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
import org.apache.syncope.core.spring.ImplementationManager;

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    public static Optional<? extends MappingItem> getConnObjectKeyItem(final Provision provision) {
        Mapping mapping = null;
        if (provision != null) {
            mapping = provision.getMapping();
        }

        return mapping == null
                ? Optional.empty()
                : mapping.getConnObjectKeyItem();
    }

    public static Stream<? extends Item> getPropagationItems(final Stream<? extends Item> items) {
        return items.filter(
                item -> item.getPurpose() == MappingPurpose.PROPAGATION || item.getPurpose() == MappingPurpose.BOTH);
    }

    public static Stream<? extends Item> getPullItems(final Stream<? extends Item> items) {
        return items.filter(
                item -> item.getPurpose() == MappingPurpose.PULL || item.getPurpose() == MappingPurpose.BOTH);
    }

    public static List<ItemTransformer> getItemTransformers(final Item item) {
        List<ItemTransformer> result = new ArrayList<>();

        // First consider the JEXL transformation expressions
        if (StringUtils.isNotBlank(item.getPropagationJEXLTransformer())
                || StringUtils.isNotBlank(item.getPullJEXLTransformer())) {

            JEXLItemTransformer jexlTransformer = (JEXLItemTransformer) ApplicationContextProvider.getBeanFactory().
                    createBean(JEXLItemTransformerImpl.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, false);

            jexlTransformer.setPropagationJEXL(item.getPropagationJEXLTransformer());
            jexlTransformer.setPullJEXL(item.getPullJEXLTransformer());
            result.add(jexlTransformer);
        }

        // Then other custom transformers
        item.getTransformers().forEach(impl -> {
            try {
                result.add(ImplementationManager.build(impl));
            } catch (Exception e) {
                LOG.error("While building {}", impl, e);
            }
        });

        return result;
    }

    /**
     * Build options for requesting all mapped connector attributes.
     *
     * @param items items
     * @param moreAttrsToGet additional attributes to get
     * @return options for requesting all mapped connector attributes
     * @see OperationOptions
     */
    public static OperationOptions buildOperationOptions(
            final Stream<? extends Item> items,
            final String... moreAttrsToGet) {

        OperationOptionsBuilder builder = new OperationOptionsBuilder();

        Set<String> attrsToGet = new HashSet<>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);
        if (!ArrayUtils.isEmpty(moreAttrsToGet)) {
            attrsToGet.addAll(List.of(moreAttrsToGet));
        }

        items.filter(item -> item.getPurpose() != MappingPurpose.NONE).
                forEach(item -> attrsToGet.add(item.getExtAttrName()));

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
