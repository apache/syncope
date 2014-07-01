package org.apache.syncope.installer.validators;

import com.izforge.izpack.api.data.InstallData;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.syncope.installer.enums.DBs;

public class PersistenceValidator extends AbstractValidator {

    private static final String POSTGRES_CLASS_DRIVER = "org.postgresql.Driver";

    private static final String MYSQL_CLASS_DRIVER = "com.mysql.jdbc.Driver";

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
                return checkConnection(POSTGRES_CLASS_DRIVER);
            case MYSQL:
                return checkConnection(MYSQL_CLASS_DRIVER);
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

    private Status checkConnection(final String driverClass) {
        try {
            Class.forName(driverClass);
            DriverManager.getConnection(persistenceUrl, persistenceDbuser, persistenceDbPassword);
            return Status.OK;
        } catch (SQLException ex) {
            error = new StringBuilder("Db connection error: please check your insert data");
            return Status.ERROR;
        } catch (ClassNotFoundException ex) {
            error = new StringBuilder("General error please contact Apache Syncope developers!");
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
