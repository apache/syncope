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
package org.apache.syncope.core.provisioning.api.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;

public interface ConnInstanceDataBinder {

    static ConnConfPropSchema build(final ConfigurationProperty property) {
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();

        connConfPropSchema.setName(property.getName());
        connConfPropSchema.setDisplayName(property.getDisplayName(property.getName()));
        connConfPropSchema.setHelpMessage(property.getHelpMessage(property.getName()));
        connConfPropSchema.setRequired(property.isRequired());
        connConfPropSchema.setType(property.getType().getName());
        connConfPropSchema.setOrder(((ConfigurationPropertyImpl) property).getOrder());
        connConfPropSchema.setConfidential(property.isConfidential());

        if (property.getValue() != null) {
            if (property.getValue().getClass().isArray()) {
                connConfPropSchema.getDefaultValues().addAll(List.of((Object[]) property.getValue()));
            } else if (property.getValue() instanceof Collection<?> collection) {
                connConfPropSchema.getDefaultValues().addAll(collection);
            } else {
                connConfPropSchema.getDefaultValues().add(property.getValue());
            }
        }

        return connConfPropSchema;
    }

    static List<ConnConfProperty> newConf(
            final List<ConnConfProperty> previousConf,
            final List<ConnConfProperty> toConf) {

        List<ConnConfProperty> newConf = new ArrayList<>();
        toConf.forEach(property -> {
            if (property.getSchema().isConfidential()
                    || GuardedString.class.getName().equals(property.getSchema().getType())) {

                if (property.getValues().isEmpty()) {
                    // no values provided, keep existing
                    previousConf.stream().
                            filter(p -> p.getSchema().getName().equals(property.getSchema().getName())).
                            findFirst().ifPresent(newConf::add);
                } else {
                    // translate confidential properties' cleartext values into GuardedStrings
                    ConnConfProperty newProperty = new ConnConfProperty();
                    newProperty.setSchema(property.getSchema());
                    newProperty.setOverridable(property.isOverridable());
                    property.getValues().forEach(value -> {
                        if (value instanceof String string) {
                            newProperty.getValues().add(new GuardedString(string.toCharArray()));
                        } else {
                            newProperty.getValues().add(value);
                        }
                    });
                }
            }

            newConf.add(property);
        });

        return newConf;
    }

    ConnInstanceTO getConnInstanceTO(ConnInstance connInstance);

    ConnInstance create(ConnInstanceTO connInstanceTO);

    ConnInstance update(ConnInstanceTO connInstanceTO);
}
