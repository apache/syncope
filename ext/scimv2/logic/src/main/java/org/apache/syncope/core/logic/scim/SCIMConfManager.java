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
package org.apache.syncope.core.logic.scim;

import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.syncope.common.lib.scim.types.SCIMEntitlement;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.logic.SchemaLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

public class SCIMConfManager {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfManager.class);

    protected final ConfParamOps confParamOps;

    protected final SchemaLogic schemaLogic;

    public SCIMConfManager(final ConfParamOps confParamOps, final SchemaLogic schemaLogic) {
        this.confParamOps = confParamOps;
        this.schemaLogic = schemaLogic;
    }

    @PreAuthorize("hasRole('" + SCIMEntitlement.SCIM_CONF_GET + "')")
    public SCIMConf get() {
        return Optional.ofNullable(confParamOps.get(AuthContextUtils.getDomain(), SCIMConf.KEY, null, String.class)).
                map(confString -> {
                    try {
                        return POJOHelper.deserialize(
                                new String(Base64.getDecoder().decode(confString)), SCIMConf.class);
                    } catch (Exception e) {
                        LOG.error("Could not deserialize, reverting to default", e);
                        return null;
                    }
                }).
                orElseGet(() -> {
                    SCIMConf scimConf = new SCIMConf();
                    set(scimConf);
                    return scimConf;
                });
    }

    @PreAuthorize("hasRole('" + SCIMEntitlement.SCIM_CONF_SET + "')")
    public void set(final SCIMConf conf) {
        try {
            schemaLogic.read(SchemaType.PLAIN, SCIMConf.KEY);
        } catch (NotFoundException e) {
            PlainSchemaTO scimConf = new PlainSchemaTO();
            scimConf.setKey(SCIMConf.KEY);
            scimConf.setType(AttrSchemaType.Binary);
            scimConf.setMimeType(MediaType.APPLICATION_JSON);
            schemaLogic.create(SchemaType.PLAIN, scimConf);
        }
        if (conf.getGeneralConf() == null) {
            conf.setGeneralConf(new SCIMGeneralConf());
        }
        conf.getGeneralConf().setLastChangeDate(OffsetDateTime.now());

        if (conf.getExtensionUserConf() != null) {
            conf.getExtensionUserConf().getAttributes().forEach(scimItem -> {
                try {
                    PlainSchemaTO schema = schemaLogic.read(SchemaType.PLAIN, scimItem.getExtAttrName());
                    SyncopeClientException invalidMapping =
                            SyncopeClientException.build(ClientExceptionType.InvalidMapping);
                    if (!scimItem.getMandatoryCondition().equals(schema.getMandatoryCondition())) {
                        invalidMapping.getElements().add('\'' + scimItem.getIntAttrName()
                                + "' should " + (Boolean.parseBoolean(schema.getMandatoryCondition()) ? "" : "not")
                                + " be required");
                    }
                    if (scimItem.isMultiValued() != schema.isMultivalue()) {
                        invalidMapping.getElements().add('\'' + scimItem.getIntAttrName()
                                + "' should " + (schema.isMultivalue() ? "" : "not") + " be multi-value");
                    }
                    if (scimItem.isMutability() != schema.isReadonly()) {
                        invalidMapping.getElements().add('\'' + scimItem.getIntAttrName()
                                + "' should " + (schema.isReadonly() ? "" : "not") + " be readonly");
                    }
                    if (scimItem.isUniqueness() != schema.isUniqueConstraint()) {
                        invalidMapping.getElements().add('\'' + scimItem.getIntAttrName()
                                + "' should " + (schema.isUniqueConstraint() ? "" : "not") + " be unique");
                    }
                    if (!invalidMapping.getElements().isEmpty()) {
                        throw invalidMapping;
                    }
                } catch (NotFoundException e) {
                    PlainSchemaTO schema = schemaLogic.read(SchemaType.VIRTUAL, scimItem.getExtAttrName());
                    if (scimItem.isMutability() != schema.isReadonly()) {
                        SyncopeClientException invalidMapping =
                                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
                        invalidMapping.getElements().add('\'' + scimItem.getIntAttrName()
                                + "' should " + (schema.isReadonly() ? "" : "not") + " be readonly");
                        throw invalidMapping;
                    }
                }
            });
        }

        confParamOps.set(AuthContextUtils.getDomain(),
                SCIMConf.KEY, Base64.getEncoder().encodeToString(POJOHelper.serialize(conf).getBytes()));
    }
}
