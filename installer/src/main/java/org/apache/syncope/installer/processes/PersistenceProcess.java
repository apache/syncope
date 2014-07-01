package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.File;
import org.apache.syncope.installer.enums.DBs;
import org.apache.syncope.installer.files.OrmXml;
import org.apache.syncope.installer.files.PersistenceProperties;
import org.apache.syncope.installer.utilities.Commands;

public class PersistenceProcess extends AbstractProcess {

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
                writeOrmFile(handler, OrmXml.ORACLE_ORM);
                break;
            case SQLSERVER:
                persistenceProperties.append(String.format(
                        PersistenceProperties.SQLSERVER, persistenceUrl, persistenceUser, persistencePassword));
                writeOrmFile(handler, OrmXml.SQLSERVER_ORM);
                break;
        }

        writeToFile(new File(
                installPath + "/" + artifactId + PersistenceProperties.PATH), persistenceProperties.toString());

    }

    private void writeOrmFile(final AbstractUIProcessHandler handler, final String content) {
        exec(String.format(Commands.createDirectory, installPath + "/" + artifactId + OrmXml.PATH_DIR), handler, null);
        final File orm = new File(installPath + "/" + artifactId + OrmXml.PATH_COMPLETE);
        writeToFile(orm, content);
    }
}
