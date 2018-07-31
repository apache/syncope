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

import java.util.Base64;
import java.util.Date;
import javax.ws.rs.core.MediaType;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.syncope.common.lib.scim.types.SCIMEntitlement;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.logic.ConfigurationLogic;
import org.apache.syncope.core.logic.SchemaLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SCIMConfManager {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfManager.class);

    @Autowired
    private ConfigurationLogic configurationLogic;

    @Autowired
    private SchemaLogic schemaLogic;

    @PreAuthorize("hasRole('" + SCIMEntitlement.SCIM_CONF_GET + "')")
    public SCIMConf get() {
        AttrTO confTO = null;
        try {
            confTO = configurationLogic.get(SCIMConf.KEY);
        } catch (Exception e) {
            LOG.error("{} not found, reverting to default", SCIMConf.KEY);
        }

        SCIMConf conf = null;
        if (confTO != null) {
            try {
                conf = POJOHelper.deserialize(
                        new String(Base64.getDecoder().decode(confTO.getValues().get(0))), SCIMConf.class);
            } catch (Exception e) {
                LOG.error("Could not deserialize, reverting to default", e);
            }
        }
        if (conf == null) {
            conf = new SCIMConf();
            set(conf);
        }

        return conf;
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
        conf.setGeneralConf(new SCIMGeneralConf());
        conf.getGeneralConf().setLastChangeDate(new Date());

        configurationLogic.set(new AttrTO.Builder().
                schema(SCIMConf.KEY).
                value(Base64.getEncoder().encodeToString(POJOHelper.serialize(conf).getBytes())).
                build());
    }
}
