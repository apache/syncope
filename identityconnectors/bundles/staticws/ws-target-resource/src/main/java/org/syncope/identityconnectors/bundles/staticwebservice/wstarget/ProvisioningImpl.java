/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.identityconnectors.bundles.staticwebservice.wstarget;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttribute;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.exceptions.ProvisioningException;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;

@WebService(endpointInterface = "org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning",
serviceName = "Provisioning")
public class ProvisioningImpl implements Provisioning {

    private static final Logger log =
            LoggerFactory.getLogger(Provisioning.class);

    @Override
    public String delete(String accountid) throws ProvisioningException {

        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        Connection conn = null;

        try {
            conn = connect();

            Statement statement = conn.createStatement();

            String query =
                    "DELETE FROM user WHERE userId='" + accountid + "';";

            if (log.isDebugEnabled()) {
                log.debug("Execute query: " + query);
            }

            statement.executeUpdate(query);

            return accountid;

        } catch (SQLException ex) {
            throw new ProvisioningException("Delete operation failed");
        } finally {

            if (conn != null) {
                try {
                    close(conn);
                } catch (SQLException ignore) {
                    // ignore exception
                }
            }

        }
    }

    @Override
    public Boolean isSyncSupported() {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        return Boolean.FALSE;
    }

    @Override
    public String checkAlive() {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        try {

            close(connect());

            if (log.isInfoEnabled()) {
                log.info("Services available");
            }

            return "OK";

        } catch (Exception e) {

            if (log.isInfoEnabled()) {
                log.info("Services not available");
            }

            return "KO";
        }
    }

    @Override
    public String update(String accountid, List<WSAttributeValue> data)
            throws ProvisioningException {

        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        Connection conn = null;

        try {

            conn = connect();
            Statement statement = conn.createStatement();

            StringBuffer set = new StringBuffer();

            for (WSAttributeValue attr : data) {
                if (!attr.isKey()) {

                    if (set.length() > 0) set.append(",");
                    set.append(attr.getName() + "='" + attr.getValue().toString() + "'");

                }
            }

            String query =
                    "UPDATE user SET " + set.toString() +
                    " WHERE userId='" + accountid + "';";

            if (log.isDebugEnabled()) {
                log.debug("Execute query: " + query);
            }

            statement.executeUpdate(query);

            return accountid;

        } catch (SQLException ex) {
            log.error("Update failed", ex);
            throw new ProvisioningException("Update operation failed");
        } finally {

            if (conn != null) {
                try {
                    close(conn);
                } catch (SQLException ignore) {
                    // ignore exception
                }
            }

        }
    }

    @Override
    public List<WSUser> query(Operand query) {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        List<WSUser> results = new ArrayList<WSUser>();

        Connection conn = null;

        try {
            String queryString =
                    "SELECT * FROM user WHERE " + query.toString();

            if (log.isDebugEnabled()) {
                log.debug("Execute query: " + queryString);
            }

            if (queryString == null || queryString.length() == 0)
                throw new SQLException("Invalid query [" + queryString + "]");

            conn = connect();
            Statement statement = conn.createStatement();

            ResultSet rs = statement.executeQuery(queryString);

            ResultSetMetaData metaData = rs.getMetaData();

            if (log.isDebugEnabled()) {
                log.debug("Metadata: " + metaData.toString());
            }

            WSUser user = null;
            WSAttributeValue attr = null;

            while (rs.next()) {

                user = new WSUser();

                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    attr = new WSAttributeValue();
                    attr.setName(metaData.getColumnLabel(i + 1));
                    attr.setValue(rs.getString(i + 1));

                    if ("userId".equalsIgnoreCase(
                            metaData.getColumnName(i + 1))) {
                        attr.setKey(true);
                        user.setAccountid(attr.getValue().toString());
                    }

                    user.addAttribute(attr);
                }

                results.add(user);
            }

            if (log.isDebugEnabled()) {
                log.debug("Retrieved users: " + results);
            }

        } catch (SQLException ex) {
            log.error("Search operation failed", ex);
        } finally {

            if (conn != null) {
                try {
                    close(conn);
                } catch (SQLException ignore) {
                    // ignore exception
                }
            }

        }

        return results;
    }

    @Override
    public String create(List<WSAttributeValue> data)
            throws ProvisioningException {

        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        Connection conn = null;
        try {
            conn = connect();
            Statement statement = conn.createStatement();

            StringBuffer keys = new StringBuffer();
            StringBuffer values = new StringBuffer();

            String accountid = null;

            for (WSAttributeValue attr : data) {
                if (keys.length() > 0) keys.append(",");
                keys.append(attr.getName());

                if (values.length() > 0) values.append(",");
                values.append(
                        "'" +
                        (attr.getValue() == null ? null : attr.getValue().toString()) +
                        "'");

                if (attr.isKey()) {
                    accountid = attr.getValue().toString();
                }
            }

            String query =
                    "INSERT INTO user (" + keys.toString() + ")" +
                    "VALUES (" + values.toString() + ");";

            if (log.isDebugEnabled()) {
                log.debug("Execute query: " + query);
            }

            statement.executeUpdate(query);

            return accountid;
        } catch (SQLException ex) {
            log.error("Creation failed", ex);
            throw new ProvisioningException("Create operation failed");
        } finally {

            if (conn != null) {
                try {
                    close(conn);
                } catch (SQLException ignore) {
                    // ignore exception
                }
            }

        }
    }

    @Override
    public int getLatestChangeNumber() throws ProvisioningException {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        return 0;
    }

    @Override
    public List<WSChange> sync() throws ProvisioningException {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public String resolve(String username) throws ProvisioningException {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        Connection conn = null;

        try {
            conn = connect();
            Statement statement = conn.createStatement();

            String query =
                    "SELECT userId FROM user WHERE userId='" + username + "';";

            if (log.isDebugEnabled()) {
                log.debug("Execute query: " + query);
            }

            ResultSet rs = statement.executeQuery(query);

            if (rs.next())
                return rs.getString(1);
            else
                return null;

        } catch (SQLException ex) {
            throw new ProvisioningException("Resolve operation failed");
        } finally {

            if (conn != null) {
                try {
                    close(conn);
                } catch (SQLException ignore) {
                    // ignore exception
                }
            }

        }
    }

    @Override
    public List<WSAttribute> schema() {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        List<WSAttribute> attrs = new ArrayList<WSAttribute>();

        WSAttribute attr = null;

        attr = new WSAttribute();
        attr.setName("userId");
        attr.setNullable(false);
        attr.setPassword(false);
        attr.setKey(true);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("password");
        attr.setNullable(false);
        attr.setPassword(true);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("type");
        attr.setNullable(false);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("residence");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("telephone");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("fax");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("preference");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("name");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("surname");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("birthdate");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("Date");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("telephone");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("gender");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("taxNumber");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("state");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("studyTitle");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("studyArea");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("job");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("companyType");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("companyName");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("vatNumber");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("String");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("mandatoryDisclaimer");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("Boolean");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("promoRCSDisclaimer");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("Boolean");
        attrs.add(attr);

        attr = new WSAttribute();
        attr.setName("promoThirdPartyDisclaimer");
        attr.setNullable(true);
        attr.setPassword(false);
        attr.setKey(false);
        attr.setType("Boolean");
        attrs.add(attr);

        return attrs;
    }

    @Override
    public String authenticate(String username, String password)
            throws ProvisioningException {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        return username;
    }

    @Override
    public Boolean isAuthenticationSupported() {
        if (log.isInfoEnabled()) {
            log.info("Operation request received");
        }

        return Boolean.FALSE;
    }

    /**
     * Establish a connection to db addressbook
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Connection connect() throws SQLException {

        if (DefaultContentLoader.localDataSource == null) {
            log.error("Data Source is null");
            return null;
        }

        Connection conn = DataSourceUtils.getConnection(
                DefaultContentLoader.localDataSource);

        if (conn == null) {
            log.error("Connection is null");
        }

        return conn;

    }

    /**
     * Close connection to db addressbook
     * @throws SQLException
     */
    private void close(Connection conn) throws SQLException {
        conn.close();
    }
}
