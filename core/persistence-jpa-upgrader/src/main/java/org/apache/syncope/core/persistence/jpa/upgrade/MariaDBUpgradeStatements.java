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

public class MariaDBUpgradeStatements extends AbstractUpgradeStatements {

    @Override
    public String getStatements() {
        String mariaDBUpgradeStatements =
                """
                UPDATE SyncopeGroup SET uManager_id=userOwner_id;

                SET @fk_name = (
                    SELECT CONSTRAINT_NAME
                    FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'SyncopeGroup'
                      AND COLUMN_NAME = 'userOwner_id'
                      AND REFERENCED_TABLE_NAME IS NOT NULL
                    LIMIT 1
                );
                
                SET @sql = CONCAT('ALTER TABLE SyncopeGroup DROP FOREIGN KEY `', @fk_name, '`');
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                ALTER TABLE SyncopeGroup DROP COLUMN userOwner_id;
                
                UPDATE SyncopeGroup SET gManager_id=groupOwner_id;
                SET @fk_name = (
                    SELECT CONSTRAINT_NAME
                    FROM information_schema.KEY_COLUMN_USAGE
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'SyncopeGroup'
                      AND COLUMN_NAME = 'groupOwner_id'
                      AND REFERENCED_TABLE_NAME IS NOT NULL
                    LIMIT 1
                );

                SET @sql = CONCAT('ALTER TABLE SyncopeGroup DROP FOREIGN KEY `', @fk_name, '`');
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                ALTER TABLE SyncopeGroup DROP COLUMN groupOwner_id;

                DELETE FROM AccessToken;
                ALTER TABLE AccessToken MODIFY COLUMN authorities TEXT;

                INSERT INTO OIDCOpEntity (id, jwks, customScopes)
                SELECT id, json ,'{}'
                FROM OIDCJWKS;

                DROP TABLE OIDCJWKS;
                """;

        return commonStatements() + mariaDBUpgradeStatements;
    }
}
