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

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.SchemaTool;

public class GenerateUpgradeSQL {

    private static final String INIT_SQL_STATEMENTS =
            """
            INSERT INTO GroupTypeExtension SELECT * FROM TypeExtension;
            INSERT INTO GroupTypeExtension_Class SELECT * FROM TypeExtension_AnyTypeClass;
            DROP TABLE TypeExtension_AnyTypeClass;
            DROP TABLE TypeExtension;

            UPDATE SyncopeGroup SET userOwner_id=uManager_id;
            ALTER TABLE SyncopeGroup DROP COLUMN userOwner_id;
            UPDATE SyncopeGroup SET groupOwner_id=gManager_id;
            ALTER TABLE SyncopeGroup DROP COLUMN groupOwner_id;

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

    private final JDBCConfiguration jdbcConf;

    public GenerateUpgradeSQL(final JDBCConfiguration jdbcConf) {
        this.jdbcConf = jdbcConf;
    }

    public void run(final Writer out) throws IOException, SQLException {
        WiserSchemaTool schemaTool = new WiserSchemaTool(jdbcConf, SchemaTool.ACTION_ADD);
        schemaTool.setSchemaGroup(jdbcConf.getSchemaFactoryInstance().readSchema());
        schemaTool.setWriter(out);

        try (out) {
            // run OpenJPA's SchemaTool to get the update statements
            schemaTool.run();

            out.append('\n').append(INIT_SQL_STATEMENTS).append('\n');
        }
    }
}
