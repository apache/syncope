/*
 *  Copyright 2010 ilgrosso.
 * 
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
package org.syncope.core.persistence.validation;

import org.syncope.core.persistence.beans.UserAttributeSchema;
import org.syncope.core.persistence.beans.UserAttributeValue;
import org.syncope.core.persistence.beans.UserAttributeValueAsBoolean;

public class AlwaysTrueValidator extends UserAttributeValidator {

    public AlwaysTrueValidator(UserAttributeSchema schema)
            throws ClassNotFoundException {

        super(schema);
    }

    @Override
    protected void doValidate(UserAttributeValue userAttributeValue)
            throws ValidationFailedException {

        Boolean value =
                ((UserAttributeValueAsBoolean) userAttributeValue).getActualValue();

        if (!value) {
            throw new ValidationFailedException(userAttributeValue);
        }
    }
}
