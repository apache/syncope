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
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.identityconnectors.common.security.GuardedString;

public interface ResourceDataBinder {

    ResourceTO getResourceTO(ExternalResource resource);

    ExternalResource create(ResourceTO resourceTO);

    ExternalResource update(ExternalResource resource, ResourceTO resourceTO);

    static Optional<List<ConnConfProperty>> newConf(
            final Optional<List<ConnConfProperty>> previousConfOverride,
            final Optional<List<ConnConfProperty>> toConfOverride) {

        if (toConfOverride.isEmpty()) {
            return Optional.empty();
        }

        if (previousConfOverride.isEmpty()) {
            return toConfOverride;
        }

        List<ConnConfProperty> newConf = new ArrayList<>();

        toConfOverride.get().forEach(property -> {
            if (property.getSchema().isConfidential()
                    || GuardedString.class.getName().equals(property.getSchema().getType())) {

                if (property.getValues().isEmpty()) {
                    // no values provided, keep existing
                    previousConfOverride.get().stream().
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

        return Optional.of(newConf);
    }
}
