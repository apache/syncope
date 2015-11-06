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
package org.apache.syncope.core.persistence.jpa.validation.entity;

import java.util.Collection;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.EntityViolationType;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;

@SuppressWarnings("rawtypes")
public class AnyValidator extends AbstractValidator<AnyCheck, Any> {

    @Override
    public boolean isValid(final Any any, final ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!(any instanceof Conf)) {
            Collection<String> allowedPlainSchemas = CollectionUtils.collect(new JPAAnyUtilsFactory().
                    getInstance(any.getType().getKind()).getAllowedSchemas(any, PlainSchema.class),
                    new Transformer<PlainSchema, String>() {

                @Override
                public String transform(final PlainSchema schema) {
                    return schema.getKey();
                }
            });

            for (PlainAttr<?> attr : ((Any<?>) any).getPlainAttrs()) {
                if (attr != null && !allowedPlainSchemas.contains(attr.getSchema().getKey())) {
                    context.buildConstraintViolationWithTemplate(
                            getTemplate(EntityViolationType.InvalidPlainSchema,
                                    attr.getSchema().getKey() + " not allowed for this instance")).
                            addPropertyNode("plainAttrs").addConstraintViolation();
                    return false;
                }
            }
        }

        return true;
    }
}
