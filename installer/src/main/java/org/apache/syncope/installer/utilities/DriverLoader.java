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

import java.net.MalformedURLException;
import org.apache.syncope.installer.enums.DBs;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;

public class DriverLoader extends URLClassLoader {

    private final static String POSTGRES_JAR = "http://jdbc.postgresql.org/download/postgresql-9.3-1101.jdbc41.jar";

    private final static String MYSQL_JAR
            = "http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.6/mysql-connector-java-5.1.6.jar";

    private static final String POSTGRES_CLASS_DRIVER = "org.postgresql.Driver";

    private static final String MYSQL_CLASS_DRIVER = "com.mysql.jdbc.Driver";

    private DriverLoader(final URL[] urls) {
        super(urls);
        addURL(urls[0]);
    }

    private static DriverLoader driverLoader;

    public static Driver load(final DBs selectedDB) {
        Driver driver = null;
        switch (selectedDB) {
            case POSTGRES:
                driver = downloadDriver(POSTGRES_JAR, POSTGRES_CLASS_DRIVER);
                break;
            case MYSQL:
                driver = downloadDriver(MYSQL_JAR, MYSQL_CLASS_DRIVER);
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

    private static Driver downloadDriver(final String driverUrl, final String driverClassName) {
        Driver driver = null;
        try {
            final URL[] url = {new URL(driverUrl)};
            driverLoader = new DriverLoader(url);
            driver = (Driver) driverLoader.loadClass(driverClassName).newInstance();
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (MalformedURLException ex) {
        }

        return driver;
    }
}
