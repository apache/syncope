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
package org.apache.syncope.core.persistence.jpa.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Converter;
import java.util.List;
import org.apache.syncope.common.lib.wa.MfaTrustedDevice;

@Converter
public class MfaTrustedDeviceListConverter extends SerializableListConverter<MfaTrustedDevice> {

    protected static final TypeReference<List<MfaTrustedDevice>> TYPEREF = new TypeReference<List<MfaTrustedDevice>>() {
    };

    @Override
    protected TypeReference<List<MfaTrustedDevice>> typeRef() {
        return TYPEREF;
    }
}
