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
package org.syncope.core.persistence.beans.membership;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.IAttrUniqueValue;

@Entity
@Table(uniqueConstraints =
@UniqueConstraint(columnNames = {
    "booleanValue",
    "dateValue",
    "doubleValue",
    "longValue",
    "StringValue",
    "schema_name"
}))
public class MAttrUniqueValue extends MAttrValue
        implements IAttrUniqueValue {

    @OneToOne(optional = false)
    @Valid
    private MSchema schema;

    @Override
    public <T extends AbstractSchema> T getSchema() {
        return (T) schema;
    }

    @Override
    public <T extends AbstractSchema> void setSchema(final T schema) {
        this.schema = (MSchema) schema;
    }
}
