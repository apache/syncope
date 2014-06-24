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
package org.apache.syncope.core.rest.data;

import java.util.Collections;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConfTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.conf.CAttr;
import org.apache.syncope.core.persistence.beans.conf.CSchema;
import org.apache.syncope.core.persistence.beans.conf.SyncopeConf;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDataBinder extends AbstractAttributableDataBinder {

    public ConfTO getConfTO(final SyncopeConf conf) {
        final ConfTO confTO = new ConfTO();
        confTO.setId(conf.getId());

        fillTO(confTO, conf.getAttrs(),
                conf.getDerAttrs(), conf.getVirAttrs(), Collections.<ExternalResource>emptySet());

        return confTO;
    }

    public AttributeTO getAttributeTO(final CAttr attr) {
        final AttributeTO attributeTO = new AttributeTO();
        attributeTO.setSchema(attr.getSchema().getName());
        attributeTO.getValues().addAll(attr.getValuesAsStrings());
        attributeTO.setReadonly(attr.getSchema().isReadonly());

        return attributeTO;
    }

    public CAttr getAttribute(final AttributeTO attributeTO) {
        CSchema schema = getNormalSchema(attributeTO.getSchema(), CSchema.class);
        if (schema == null) {
            throw new NotFoundException("Conf schema " + attributeTO.getSchema());
        } else {
            SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

            CAttr attr = new CAttr();
            attr.setSchema(schema);
            fillAttribute(attributeTO.getValues(), AttributableUtil.getInstance(AttributableType.CONFIGURATION),
                    schema, attr, invalidValues);

            if (!invalidValues.isEmpty()) {
                throw invalidValues;
            }
            return attr;
        }
    }

}
