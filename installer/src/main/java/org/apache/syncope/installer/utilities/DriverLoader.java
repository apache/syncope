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
package org.apache.syncope.installer.utilities;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.apache.syncope.installer.enums.DBs;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;

public final class DriverLoader extends URLClassLoader {

    private static final String POSTGRES_JAR =
            "http://repo1.maven.org/maven2/postgresql/postgresql/9.1-901.jdbc4/postgresql-9.1-901.jdbc4.jar";

    private static final String MYSQL_JAR =
            "http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.34/mysql-connector-java-5.1.34.jar";

    private static final String MARIADB_JAR =
            "http://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/1.1.8/mariadb-java-client-1.1.8.jar";

    private static final String POSTGRES_CLASS_DRIVER = "org.postgresql.Driver";

    private static final String MYSQL_CLASS_DRIVER = "com.mysql.jdbc.Driver";

    private static final String MARIADB_CLASS_DRIVER = "org.mariadb.jdbc.Driver";

    private DriverLoader(final URL[] urls) {
        super(urls);
        addURL(urls[0]);
    }

    private static DriverLoader DRIVER_LOADER;

    public static Driver load(final DBs selectedDB, final boolean isProxyEnabled, final String proxyHost,
            final String proxyPort, final String proxyUser, final String proxyPwd) {

        Driver driver = null;
        switch (selectedDB) {
            case POSTGRES:
                driver = downloadDriver(POSTGRES_JAR, POSTGRES_CLASS_DRIVER, isProxyEnabled, proxyHost, proxyPort,
                        proxyUser, proxyPwd);
                break;
            case MYSQL:
                driver = downloadDriver(MYSQL_JAR, MYSQL_CLASS_DRIVER, isProxyEnabled, proxyHost, proxyPort,
                        proxyUser, proxyPwd);
                break;
            case MARIADB:
                driver = downloadDriver(MARIADB_JAR, MARIADB_CLASS_DRIVER, isProxyEnabled, proxyHost, proxyPort,
                        proxyUser, proxyPwd);
                break;
            case SQLSERVER:
                break;
            case ORACLE:
                break;
            default:
                break;
        }
        return driver;
    }

    private static Driver downloadDriver(final String driverUrl, final String driverClassName,
            final boolean isProxyEnabled, final String proxyHost, final String proxyPort, final String proxyUser,
            final String proxyPwd) {

        Driver driver = null;
        try {
            if (isProxyEnabled) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
                if (proxyUser != null && !proxyUser.isEmpty() && proxyPwd != null) {
                    Authenticator.setDefault(new Authenticator() {

                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUser, proxyPwd.toCharArray());
                        }
                    });
                    System.setProperty("http.proxyUser", proxyUser);
                    System.setProperty("http.proxyPassword", proxyPwd);
                }
            }
            final URL[] url = { new URL(driverUrl) };
            DRIVER_LOADER = new DriverLoader(url);
            driver = (Driver) DRIVER_LOADER.loadClass(driverClassName).newInstance();
        } catch (Exception e) {
            // ignore
        }

        return driver;
    }
}
