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
package org.apache.syncope.server.provisioning.java.data;

import org.apache.syncope.server.provisioning.api.data.ConfigurationDataBinder;
import java.util.Collections;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.server.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.server.persistence.api.entity.conf.Conf;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationDataBinderImpl extends AbstractAttributableDataBinder implements ConfigurationDataBinder {

    @Override
    public ConfTO getConfTO(final Conf conf) {
        final ConfTO confTO = new ConfTO();
        confTO.setKey(conf.getKey());

        fillTO(confTO, conf.getPlainAttrs(),
                conf.getDerAttrs(), conf.getVirAttrs(), Collections.<ExternalResource>emptySet());

        return confTO;
    }

    @Override
    public AttrTO getAttrTO(final CPlainAttr attr) {
        final AttrTO attributeTO = new AttrTO();
        attributeTO.setSchema(attr.getSchema().getKey());
        attributeTO.getValues().addAll(attr.getValuesAsStrings());
        attributeTO.setReadonly(attr.getSchema().isReadonly());

        return attributeTO;
    }

    @Override
    public CPlainAttr getAttribute(final AttrTO attributeTO) {
        CPlainSchema schema = getPlainSchema(attributeTO.getSchema(), CPlainSchema.class);
        if (schema == null) {
            throw new NotFoundException("Conf schema " + attributeTO.getSchema());
        } else {
            SyncopeClientException invalidValues = SyncopeClientException.build(ClientExceptionType.InvalidValues);

            CPlainAttr attr = entityFactory.newEntity(CPlainAttr.class);
            attr.setSchema(schema);
            fillAttribute(attributeTO.getValues(), attrUtilFactory.getInstance(AttributableType.CONFIGURATION),
                    schema, attr, invalidValues);

            if (!invalidValues.isEmpty()) {
                throw invalidValues;
            }
            return attr;
        }
    }

}
