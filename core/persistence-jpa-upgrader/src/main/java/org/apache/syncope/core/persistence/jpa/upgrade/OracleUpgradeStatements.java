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

public class OracleUpgradeStatements extends AbstractUpgradeStatements {

    @Override
    public String getStatements() {

        String postgreSQLUpgradeStatements = """
                UPDATE SyncopeGroup SET uManager_id=userOwner_id;
                ALTER TABLE SyncopeGroup DROP COLUMN userOwner_id;

                UPDATE SyncopeGroup SET gManager_id=groupOwner_id;
                ALTER TABLE SyncopeGroup DROP COLUMN groupOwner_id;

                DELETE FROM ACCESSTOKEN;
                ALTER TABLE ACCESSTOKEN MODIFY AUTHORITIES CLOB;
                INSERT INTO OIDCOpEntity (id, jwks, customScopes)
                      SELECT id, clob_to_blob(json),'{}' FROM OIDCJWKS;

                DROP TABLE OIDCJWKS;
                """;

        return convertCLOBToBLOBFunction() + commonStatements() + postgreSQLUpgradeStatements;
    }

    private String convertCLOBToBLOBFunction() {
        return """
                CREATE OR REPLACE FUNCTION clob_to_blob(
                    p_clob IN CLOB
                ) RETURN BLOB IS
                    l_blob        BLOB;
                    l_dest_offset INTEGER := 1;
                    l_src_offset  INTEGER := 1;
                    l_lang_ctx    INTEGER := DBMS_LOB.DEFAULT_LANG_CTX;
                    l_warning     INTEGER;
                BEGIN
                    IF p_clob IS NULL THEN
                        RETURN NULL;
                    END IF;
                
                    DBMS_LOB.CREATETEMPORARY(l_blob, TRUE);
                
                    DBMS_LOB.CONVERTTOBLOB(
                        dest_lob     => l_blob,
                        src_clob     => p_clob,
                        amount       => DBMS_LOB.LOBMAXSIZE,
                        dest_offset  => l_dest_offset,
                        src_offset   => l_src_offset,
                        blob_csid    => NLS_CHARSET_ID('AL32UTF8'),
                        lang_context => l_lang_ctx,
                        warning      => l_warning
                    );

                    RETURN l_blob;
                END;

                """;
    }
}
