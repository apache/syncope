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
package org.apache.syncope.core.persistence.jpa.upgrade;

public abstract class AbstractUpgradeStatements implements UpgradeStatements {

    protected static String commonStatements() {
        return """
                INSERT INTO GroupTypeExtension SELECT * FROM TypeExtension;
                INSERT INTO GroupTypeExtension_Class SELECT * FROM TypeExtension_AnyTypeClass;

                DROP TABLE TypeExtension_AnyTypeClass;
                DROP TABLE TypeExtension;
                DROP TABLE SyncopeRole_DynRealm;
                DROP TABLE DynRealmMembership;
                DROP TABLE DynRealm;
                DROP TABLE UDynGroupMembership;
                DROP TABLE ADynGroupMembership;
                DROP TABLE UDynGroupMembers;
                DROP TABLE ADynGroupMembers;
                DROP TABLE DynRoleMembers;
                DROP TABLE DynRealmMembers;
                """;
    }
}
