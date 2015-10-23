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
package org.apache.syncope.installer.enums;

public enum DBs {

    POSTGRES("postgres"),
    MYSQL("mysql"),
    MARIADB("mariadb"),
    SQLSERVER("sqlserver"),
    ORACLE("oracle");

    DBs(final String name) {
        this.name = name;
    }

    private final String name;

    public String getName() {
        return name;
    }

    public static DBs fromDbName(final String containerName) {
        DBs db = null;
        if (POSTGRES.getName().equalsIgnoreCase(containerName)) {
            db = POSTGRES;
        } else if (MYSQL.getName().equalsIgnoreCase(containerName)) {
            db = MYSQL;
        } else if (MARIADB.getName().equalsIgnoreCase(containerName)) {
            db = MARIADB;
        } else if (ORACLE.getName().equalsIgnoreCase(containerName)) {
            db = ORACLE;
        } else {
            db = SQLSERVER;
        }

        return db;
    }
}
