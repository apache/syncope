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
package org.apache.syncope.installer.processes;

import org.apache.syncope.installer.utilities.FileSystemUtils;
import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.enums.DBs;
import org.apache.syncope.installer.files.OrmXml;
import org.apache.syncope.installer.files.PersistenceProperties;

public class PersistenceProcess {

    private String installPath;

    private String artifactId;

    private DBs dbSelected;

    private String persistenceUrl;

    private String persistenceUser;

    private String persistencePassword;

    private boolean mysqlInnoDB;

    private String oracleTableSpace;

    public void run(final AbstractUIProcessHandler handler, final String[] args) {

        installPath = args[0];
        artifactId = args[1];
        dbSelected = DBs.fromDbName(args[2]);
        persistenceUrl = args[3];
        persistenceUser = args[4];
        persistencePassword = args[5];
        mysqlInnoDB = Boolean.valueOf(args[6]);
        oracleTableSpace = args[7];

        final FileSystemUtils fileSystemUtils = new FileSystemUtils(handler);

        final StringBuilder persistenceProperties = new StringBuilder(PersistenceProperties.HEADER);

        switch (dbSelected) {
            case POSTGRES:
                persistenceProperties.append(String.format(
                        PersistenceProperties.POSTGRES, persistenceUrl, persistenceUser, persistencePassword));
                break;
            case MYSQL:
                persistenceProperties.append(String.format(
                        PersistenceProperties.MYSQL, persistenceUrl, persistenceUser, persistencePassword));
                if (mysqlInnoDB) {
                    persistenceProperties.append(PersistenceProperties.QUARTZ_INNO_DB);
                } else {
                    persistenceProperties.append(PersistenceProperties.QUARTZ);
                }
                break;
            case ORACLE:
                persistenceProperties.append(String.format(
                        PersistenceProperties.ORACLE, persistenceUrl, persistenceUser, persistencePassword,
                        oracleTableSpace));
                writeOrmFile(fileSystemUtils, OrmXml.ORACLE_ORM);
                break;
            case SQLSERVER:
                persistenceProperties.append(String.format(
                        PersistenceProperties.SQLSERVER, persistenceUrl, persistenceUser, persistencePassword));
                writeOrmFile(fileSystemUtils, OrmXml.SQLSERVER_ORM);
                break;
        }

        fileSystemUtils.writeToFile(new File(
                installPath + "/" + artifactId + PersistenceProperties.PATH), persistenceProperties.toString());

    }

    private void writeOrmFile(final FileSystemUtils fileSystemUtils, final String content) {
        fileSystemUtils.createDirectory(installPath + "/" + artifactId + OrmXml.PATH_DIR, null);
        fileSystemUtils.writeToFile(new File(installPath + "/" + artifactId + OrmXml.PATH_COMPLETE), content);
    }
}
