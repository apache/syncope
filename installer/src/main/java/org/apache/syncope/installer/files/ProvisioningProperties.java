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

public final class ProvisioningProperties {

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
            + "# under the License.\n"
            + "userProvisioningManager=org.apache.syncope.core.provisioning.java.DefaultUserProvisioningManager\n"
            + "groupProvisioningManager=org.apache.syncope.core.provisioning.java.DefaultGroupProvisioningManager\n"
            + "anyObjectProvisioningManager="
            + "org.apache.syncope.core.provisioning.java.DefaultAnyObjectProvisioningManager\n"
            + "virAttrCache=org.apache.syncope.core.provisioning.java.cache.MemoryVirAttrCache\n";

    public static final String POSTGRES = ""
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate\n"
            + "quartz.sql=tables_postgres.sql\n";

    public static final String MYSQL = ""
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.StdJDBCDelegate\n";

    public static final String MYSQL_QUARTZ_INNO_DB = "quartz.sql=tables_mysql_innodb.sql";

    public static final String MYSQL_QUARTZ = "quartz.sql=tables_mysql.sql";

    public static final String MARIADB = ""
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.StdJDBCDelegate\n"
            + "quartz.sql=tables_mariadb.sql\n";

    public static final String ORACLE = ""
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.oracle.OracleDelegate\n"
            + "quartz.sql=tables_oracle.sql\n";

    public static final String SQLSERVER = ""
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.MSSQLDelegate\n"
            + "quartz.sql=tables_sqlServer.sql\n";

    private ProvisioningProperties() {
        // private constructor for static utility class
    }
}
