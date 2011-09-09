/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.validation.entity;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.types.EntityViolationType;
import org.syncope.types.SourceMappingType;

public class SchemaMappingValidator extends AbstractValidator
        implements ConstraintValidator<SchemaMappingCheck, SchemaMapping> {

    @Override
    public void initialize(final SchemaMappingCheck constraintAnnotation) {
    }

    @Override
    public boolean isValid(
            final SchemaMapping object,
            final ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();

        if (object.getDestAttrName() == null
                && !object.isAccountid()
                && !object.isPassword()) {
            context.buildConstraintViolationWithTemplate(
                    "Missing destination attribute name").addNode(
                    EntityViolationType.InvalidSchemaMapping.toString()).
                    addConstraintViolation();

            return false;
        }

        if (object.getSourceAttrName() == null
                && SourceMappingType.SyncopeUserId
                != object.getSourceMappingType()
                && SourceMappingType.Password
                != object.getSourceMappingType()) {
            context.buildConstraintViolationWithTemplate(
                    "Missing source attribute name").addNode(
                    EntityViolationType.InvalidSchemaMapping.toString()).
                    addConstraintViolation();

            return false;
        }

        return true;
    }
}
