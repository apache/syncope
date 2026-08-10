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

            UpgradeStatements statements = UpgradeStatementsFactory.getInstance(jdbcConf.getDBDictionaryInstance());

            out.append('\n');
            out.append(statements.getStatements());
            out.append('\n');
        }
    }
}
