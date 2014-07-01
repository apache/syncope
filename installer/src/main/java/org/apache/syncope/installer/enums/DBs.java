package org.apache.syncope.installer.enums;

public enum DBs {

    POSTGRES("postgres"),
    MYSQL("mysql"),
    SQLSERVER("sqlserver"),
    ORACLE("oracle");

    private DBs(final String name) {
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
        } else if (ORACLE.getName().equalsIgnoreCase(containerName)) {
            db = ORACLE;
        }
        return db;
    }
}
