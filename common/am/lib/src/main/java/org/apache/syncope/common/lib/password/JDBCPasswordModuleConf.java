package org.apache.syncope.common.lib.password;

import java.util.Map;
import org.apache.syncope.common.lib.AbstractJDBCConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;

public class JDBCPasswordModuleConf extends AbstractJDBCConf implements PasswordModuleConf {

    private static final long serialVersionUID = 1335379501740158360L;

    /**
     * SQL query to change the password and update.
     */
    private String sqlChangePassword;

    /**
     * SQL query to locate the user email address.
     */
    private String sqlFindEmail;

    /**
     * SQL query to locate the user phone number.
     */
    private String sqlFindPhone;

    /**
     * SQL query to locate the user via email.
     */
    private String sqlFindUser;

    /**
     * SQL query to locate security questions for the account, if any.
     */
    private String sqlGetSecurityQuestions;

    /**
     * SQL query to update security questions for the account, if any.
     */
    private String sqlUpdateSecurityQuestions;

    /**
     * SQL query to unlock accounts.
     */
    private String sqlUnlockAccount;

    /**
     * SQL query to delete security questions for the account, if any.
     */
    private String sqlDeleteSecurityQuestions;

    public String getSqlChangePassword() {
        return sqlChangePassword;
    }

    public void setSqlChangePassword(final String sqlChangePassword) {
        this.sqlChangePassword = sqlChangePassword;
    }

    public String getSqlFindEmail() {
        return sqlFindEmail;
    }

    public void setSqlFindEmail(final String sqlFindEmail) {
        this.sqlFindEmail = sqlFindEmail;
    }

    public String getSqlFindPhone() {
        return sqlFindPhone;
    }

    public void setSqlFindPhone(final String sqlFindPhone) {
        this.sqlFindPhone = sqlFindPhone;
    }

    public String getSqlFindUser() {
        return sqlFindUser;
    }

    public void setSqlFindUser(final String sqlFindUser) {
        this.sqlFindUser = sqlFindUser;
    }

    public String getSqlGetSecurityQuestions() {
        return sqlGetSecurityQuestions;
    }

    public void setSqlGetSecurityQuestions(final String sqlGetSecurityQuestions) {
        this.sqlGetSecurityQuestions = sqlGetSecurityQuestions;
    }

    public String getSqlUpdateSecurityQuestions() {
        return sqlUpdateSecurityQuestions;
    }

    public void setSqlUpdateSecurityQuestions(final String sqlUpdateSecurityQuestions) {
        this.sqlUpdateSecurityQuestions = sqlUpdateSecurityQuestions;
    }

    public String getSqlUnlockAccount() {
        return sqlUnlockAccount;
    }

    public void setSqlUnlockAccount(final String sqlUnlockAccount) {
        this.sqlUnlockAccount = sqlUnlockAccount;
    }

    public String getSqlDeleteSecurityQuestions() {
        return sqlDeleteSecurityQuestions;
    }

    public void setSqlDeleteSecurityQuestions(final String sqlDeleteSecurityQuestions) {
        this.sqlDeleteSecurityQuestions = sqlDeleteSecurityQuestions;
    }

    @Override
    public Map<String, Object> map(final PasswordModuleTO passwordModuleTO, final Mapper mapper) {
        return mapper.map(passwordModuleTO, this);
    }
}
