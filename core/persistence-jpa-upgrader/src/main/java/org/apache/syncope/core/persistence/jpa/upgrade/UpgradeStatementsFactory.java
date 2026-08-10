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

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MariaDBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;

public final class UpgradeStatementsFactory {

    public static UpgradeStatements getInstance(final DBDictionary dictionary) {
        if (dictionary instanceof PostgresDictionary) {
            return new PostgreSQLUpgradeStatements();
        }
        if (dictionary instanceof MySQLDictionary) {
            return new MySQLUpgradeStatements();
        }
        if (dictionary instanceof MariaDBDictionary) {
            return new MariaDBUpgradeStatements();
        }
        if (dictionary instanceof OracleDictionary) {
            return new OracleUpgradeStatements();
        }

        throw new IllegalArgumentException(dictionary.platform);
    }

    private UpgradeStatementsFactory() {
    }
}
