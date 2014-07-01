package org.apache.syncope.installer.files;

public class PersistenceProperties {

    public static final String PATH = "/core/src/main/resources/persistence.properties";

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
            + "# specific language governing permissions and limitations\n" + "# under the License.\n";

    public static final String POSTGRES = ""
            + "jpa.driverClassName=org.postgresql.Driver\n"
            + "jpa.url=%s\n"
            + "jpa.username=%s\n"
            + "jpa.password=%s\n"
            + "jpa.dialect=org.apache.openjpa.jdbc.sql.PostgresDictionary\n"
            + "jpa.pool.validationQuery=SELECT 1\n"
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.PostgreSQLDelegate\n"
            + "quartz.sql=tables_postgres.sql\n"
            + "audit.sql=audit.sql\n"
            + "logback.sql=postgresql.sql";

    public static final String MYSQL = ""
            + "jpa.driverClassName=com.mysql.jdbc.Driver\n"
            + "jpa.url=%s\n"
            + "jpa.username=%s\n"
            + "jpa.password=%s\n"
            + "jpa.dialect=org.apache.openjpa.jdbc.sql.MySQLDictionary\n"
            + "jpa.pool.validationQuery=SELECT 1\n"
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.StdJDBCDelegate\n"
            + "audit.sql=audit.sql\n"
            + "logback.sql=mysql.sql\n";

    public static final String ORACLE = ""
            + "jpa.driverClassName=oracle.jdbc.OracleDriver\n"
            + "jpa.url=%s\n"
            + "jpa.username=%s\n"
            + "jpa.password=%s\n"
            + "jpa.dialect=org.apache.openjpa.jdbc.sql.OracleDictionary\n"
            + "jpa.pool.validationQuery=SELECT 1\n"
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.oracle.OracleDelegate\n"
            + "quartz.sql=tables_oracle.sql\n"
            + "audit.sql=audit.sql\n"
            + "logback.sql=oracle.sql\n"
            + "database.schema=%s\n";

    public static final String SQLSERVER = ""
            + "jpa.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver\n"
            + "jpa.url=%s\n"
            + "jpa.username=%s\n"
            + "jpa.password=%s\n"
            + "jpa.dialect=org.apache.openjpa.jdbc.sql.SQLServerDictionary\n"
            + "quartz.jobstore=org.quartz.impl.jdbcjobstore.MSSQLDelegate\n"
            + "quartz.sql=tables_sqlServer.sql\n"
            + "logback.sql=sqlserver.sql\n"
            + "audit.sql=audit.sql\n";

    public static final String QUARTZ_INNO_DB = "quartz.sql=tables_mysql_innodb.sql";

    public static final String QUARTZ = "quartz.sql=tables_mysql.sql";

}
