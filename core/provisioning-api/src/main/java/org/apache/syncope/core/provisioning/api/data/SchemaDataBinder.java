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

import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;

public interface SchemaDataBinder {

    PlainSchema create(PlainSchemaTO schemaTO);

    DerSchema create(DerSchemaTO schemaTO);

    VirSchema create(VirSchemaTO schemaTO);

    PlainSchema update(PlainSchemaTO schemaTO, PlainSchema schema);

    DerSchema update(DerSchemaTO schemaTO, DerSchema derSchema);

    VirSchema update(VirSchemaTO schemaTO, VirSchema virSchema);

    PlainSchemaTO getPlainSchemaTO(String key);

    DerSchemaTO getDerSchemaTO(String key);

    VirSchemaTO getVirSchemaTO(String key);
}
