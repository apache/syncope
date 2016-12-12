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
package org.apache.syncope.installer.files;

public final class MasterProperties {

    public static final String HEADER = "# Licensed to the Apache Software Foundation (ASF) under one\n"
            + "# or more contributor license agreements.  See the NOTICE file\n"
            + "# distributed with this work for additional information\n"
            + "# regarding copyright ownership.  The ASF licenses this file\n"
            + "# to you under the Apache License, Version 2.0 (the\n"
            + "# \"License\"); you may not use this file except in compliance\n"
            + "# with the License.  You may obtain a copy of the License at\n" + "#\n"
            + "#   http://www.apache.org/licenses/LICENSE-2.0\n" + "#\n"
            + "# Unless required by applicable law or agreed to in writing,\n"
            + "# software distributed under the License is distributed on an\n"
            + "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n"
            + "# KIND, either express or implied.  See the License for the\n"
            + "# specific language governing permissions and limitations\n"
            + "# under the License.\n";

    public static final String POSTGRES = ""
            + "Master.driverClassName=org.postgresql.Driver%n"
            + "Master.url=%s%n"
            + "Master.schema=%n"
            + "Master.username=%s%n"
            + "Master.password=%s%n"
            + "Master.databasePlatform=org.apache.openjpa.jdbc.sql.PostgresDictionary%n"
            + "Master.orm=META-INF/spring-orm.xml%n"
            + "Master.pool.validationQuery=SELECT 1%n"
            + "Master.audit.sql=audit.sql%n";

    public static final String MYSQL = ""
            + "Master.driverClassName=com.mysql.jdbc.Driver%n"
            + "Master.url=%s%n"
            + "Master.schema=%n"
            + "Master.username=%s%n"
            + "Master.password=%s%n"
            + "Master.databasePlatform=org.apache.openjpa.jdbc.sql.MySQLDictionary%n"
            + "Master.orm=META-INF/spring-orm.xml%n"
            + "Master.pool.validationQuery=SELECT 1%n"
            + "Master.audit.sql=audit.sql%n";

    public static final String MARIADB = ""
            + "Master.driverClassName=org.mariadb.jdbc.Driver%n"
            + "Master.url=%s%n"
            + "Master.schema=%n"
            + "Master.username=%s%n"
            + "Master.password=%s%n"
            + "Master.databasePlatform=org.apache.openjpa.jdbc.sql.MariaDBDictionary%n"
            + "Master.orm=META-INF/spring-orm.xml%n"
            + "Master.pool.validationQuery=SELECT 1%n"
            + "Master.audit.sql=audit.sql%n";

    public static final String ORACLE = ""
            + "Master.driverClassName=oracle.jdbc.OracleDriver%n"
            + "Master.url=%s%n"
            + "Master.schema=%s%n"
            + "Master.username=%s%n"
            + "Master.password=%s%n"
            + "Master.databasePlatform=org.apache.openjpa.jdbc.sql.OracleDictionary%n"
            + "Master.orm=META-INF/spring-orm-oracle.xml%n"
            + "Master.pool.validationQuery=SELECT 1 FROM DUAL%n"
            + "Master.audit.sql=audit_oracle.sql%n";

    public static final String SQLSERVER = ""
            + "Master.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver%n"
            + "Master.url=%s%n"
            + "Master.schema=%s%n"
            + "Master.username=%s%n"
            + "Master.password=%s%n"
            + "Master.databasePlatform=org.apache.openjpa.jdbc.sql.SQLServerDictionary%n"
            + "Master.orm=META-INF/spring-orm-sqlserver.xml%n"
            + "Master.pool.validationQuery=SELECT 1%n"
            + "Master.audit.sql=audit_sqlserver.sql%n";

    private MasterProperties() {
        // private constructor for static utility class
    }
}
