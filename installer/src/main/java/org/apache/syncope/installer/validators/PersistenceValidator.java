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
package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.data.InstallData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.syncope.installer.enums.DBs;
import org.apache.syncope.installer.utilities.DriverLoader;

public class PersistenceValidator extends AbstractValidator {

    private String persistenceUrl;

    private String persistenceDbuser;

    private String persistenceDbPassword;

    private StringBuilder error;

    private StringBuilder warning;

    @Override
    public Status validateData(final InstallData installData) {

        final DBs selectedDB = DBs.fromDbName(
                installData.getVariable("install.type.selection"));

        persistenceUrl = installData.getVariable("persistence.url");
        persistenceDbuser = installData.getVariable("persistence.dbuser");
        persistenceDbPassword = installData.getVariable("persistence.dbpassword");

        boolean verified = true;
        error = new StringBuilder("Required fields:\n");
        if (isEmpty(persistenceUrl)) {
            error.append("Persistence URL\n");
            verified = false;
        }
        if (isEmpty(persistenceDbuser)) {
            error.append("Persistence user\n");
            verified = false;
        }
        if (isEmpty(persistenceDbPassword)) {
            error.append("Persistence password\n");
            verified = false;
        }

        if (!verified) {
            return Status.ERROR;
        }

        switch (selectedDB) {
            case POSTGRES:
                return checkConnection(selectedDB);
            case MYSQL:
                return checkConnection(selectedDB);
            case SQLSERVER:
                warning = new StringBuilder("Remember to check your SqlServer db connection");
                return Status.WARNING;
            case ORACLE:
                warning = new StringBuilder("Remember to check your Oracle db connection");
                return Status.WARNING;
            default:
                error = new StringBuilder("DB not supported yet");
                return Status.ERROR;
        }
    }

    private Status checkConnection(final DBs selectedDb) {

        try {
            final Driver driver = DriverLoader.load(selectedDb);
            final Properties props = new Properties();
            props.put("user", persistenceDbuser);
            props.put("password", persistenceDbPassword);
            driver.connect(persistenceUrl, props);
            return Status.OK;
        } catch (SQLException ex) {
            error = new StringBuilder("Db connection error: please check your insert data");
            return Status.ERROR;
        }
    }

    @Override
    public String getErrorMessageId() {
        return error.toString();
    }

    @Override
    public String getWarningMessageId() {
        return warning.toString();
    }

    @Override
    public boolean getDefaultAnswer() {
        return true;
    }

}
