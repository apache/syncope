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
package org.apache.syncope.core.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.core.persistence.beans.SchemaMapping;

/**
 *
 * @author ilgrosso
 */
public class SchemaMappingWrapper {

    private SchemaMapping accountIdMapping = null;

    private SchemaMapping passwordMapping = null;

    private final Map<String, Set<SchemaMapping>> uMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> uVirMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> uDerMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> rMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> rVirMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> rDerMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> mMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> mVirMappings = new HashMap<String, Set<SchemaMapping>>();

    private final Map<String, Set<SchemaMapping>> mDerMappings = new HashMap<String, Set<SchemaMapping>>();

    public SchemaMappingWrapper(final Set<SchemaMapping> mappings) {
        if (mappings == null) {
            return;
        }
        for (SchemaMapping mapping : mappings) {
            if (mapping.isAccountid() && accountIdMapping == null) {
                accountIdMapping = mapping;
            } else if (mapping.isPassword() && passwordMapping == null) {
                passwordMapping = mapping;
            } else {
                final String intAttrName = SchemaMappingUtil.getIntAttrName(mapping);
                switch (mapping.getIntMappingType()) {
                    case Password:
                        if (passwordMapping == null) {
                            passwordMapping = mapping;
                        }
                        break;

                    case Username:
                    case SyncopeUserId:
                    case UserSchema:
                        if (uMappings.get(intAttrName) == null) {
                            uMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        uMappings.get(intAttrName).add(mapping);
                        break;

                    case RoleSchema:
                        if (rMappings.get(intAttrName) == null) {
                            rMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        rMappings.get(intAttrName).add(mapping);
                        break;

                    case MembershipSchema:
                        if (mMappings.get(intAttrName) == null) {
                            mMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        mMappings.get(intAttrName).add(mapping);
                        break;

                    case UserDerivedSchema:
                        if (uDerMappings.get(intAttrName) == null) {
                            uDerMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        uDerMappings.get(intAttrName).add(mapping);
                        break;

                    case RoleDerivedSchema:
                        if (rDerMappings.get(intAttrName) == null) {
                            rDerMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        rDerMappings.get(intAttrName).add(mapping);
                        break;

                    case MembershipDerivedSchema:
                        if (mDerMappings.get(intAttrName) == null) {
                            mDerMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        mDerMappings.get(intAttrName).add(mapping);
                        break;

                    case UserVirtualSchema:
                        if (uVirMappings.get(intAttrName) == null) {
                            uVirMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        uVirMappings.get(intAttrName).add(mapping);
                        break;

                    case RoleVirtualSchema:
                        if (rVirMappings.get(intAttrName) == null) {
                            rVirMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        rVirMappings.get(intAttrName).add(mapping);
                        break;

                    case MembershipVirtualSchema:
                        if (mVirMappings.get(intAttrName) == null) {
                            mVirMappings.put(intAttrName, new HashSet<SchemaMapping>());
                        }
                        mVirMappings.get(intAttrName).add(mapping);
                        break;

                    default:
                }
            }
        }
    }

    public SchemaMapping getAccountIdMapping() {
        return accountIdMapping;
    }

    public Map<String, Set<SchemaMapping>> getmDerMappings() {
        return mDerMappings;
    }

    public Map<String, Set<SchemaMapping>> getmMappings() {
        return mMappings;
    }

    public Map<String, Set<SchemaMapping>> getmVirMappings() {
        return mVirMappings;
    }

    public SchemaMapping getPasswordMapping() {
        return passwordMapping;
    }

    public Map<String, Set<SchemaMapping>> getrDerMappings() {
        return rDerMappings;
    }

    public Map<String, Set<SchemaMapping>> getrMappings() {
        return rMappings;
    }

    public Map<String, Set<SchemaMapping>> getrVirMappings() {
        return rVirMappings;
    }

    public Map<String, Set<SchemaMapping>> getuDerMappings() {
        return uDerMappings;
    }

    public Map<String, Set<SchemaMapping>> getuMappings() {
        return uMappings;
    }

    public Map<String, Set<SchemaMapping>> getuVirMappings() {
        return uVirMappings;
    }
}
