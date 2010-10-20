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

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(Provisioning.class);

    @Override
    public String delete(String accountid) throws ProvisioningException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        Connection conn = null;

        try {
            conn = connect();

            Statement statement = conn.createStatement();

            String query =
                    "DELETE FROM user WHERE userId='" + accountid + "';";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute query: " + query);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        return Boolean.FALSE;
    }

    @Override
    public String checkAlive() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        try {

            close(connect());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Services available");
            }

            return "OK";

        } catch (Exception e) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Services not available");
            }

            return "KO";
        }
    }

    @Override
    public String update(String accountid, List<WSAttributeValue> data)
            throws ProvisioningException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        if (data == null || data.isEmpty()) {
            LOG.warn("Empty data recevied");
            return accountid;
        }

        Connection conn = null;

        try {
            conn = connect();
            Statement statement = conn.createStatement();

            String value;

            StringBuilder set = new StringBuilder();
            for (WSAttributeValue attr : data) {

                if (attr.getValues() == null || attr.getValues().isEmpty()) {
                    value = null;
                } else if (attr.getValues().size() == 1) {
                        value = attr.getValues().get(0).toString();
                    } else {
                        value = attr.getValues().toString();
                    }

                if (!attr.isKey()
                        || !accountid.equals(value)) {
                    if (set.length() > 0) {
                        set.append(",");
                    }

                    if ("__NAME__".equals(attr.getName())) {
                        set.append("userId=");
                    } else if ("__PASSWORD__".equals(attr.getName())) {
                            set.append("password=");
                        } else {
                            set.append(attr.getName()).append('=');
                        }

                    set.append(value == null ? null : "'" + value + "'");
                }
            }

            String query =
                    "UPDATE user SET " + set.toString()
                    + " WHERE userId='" + accountid + "';";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute query: " + query);
            }

            statement.executeUpdate(query);

            return accountid;
        } catch (SQLException ex) {
            LOG.error("Update failed", ex);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        List<WSUser> results = new ArrayList<WSUser>();

        Connection conn = null;

        try {
            String queryString =
                    "SELECT * FROM user WHERE " + query.toString();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute query: " + queryString);
            }

            if (queryString == null || queryString.length() == 0) {
                throw new SQLException("Invalid query [" + queryString + "]");
            }

            conn = connect();
            Statement statement = conn.createStatement();

            ResultSet rs = statement.executeQuery(queryString);

            ResultSetMetaData metaData = rs.getMetaData();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Metadata: " + metaData.toString());
            }

            WSUser user = null;
            WSAttributeValue attr = null;

            while (rs.next()) {

                user = new WSUser();

                for (int i = 0; i < metaData.getColumnCount(); i++) {
                    attr = new WSAttributeValue();
                    attr.setName(metaData.getColumnLabel(i + 1));
                    attr.setValues(
                            Collections.singletonList(rs.getString(i + 1)));

                    if ("userId".equalsIgnoreCase(
                            metaData.getColumnName(i + 1))) {
                        attr.setKey(true);
                        user.setAccountid(rs.getString(i + 1));
                    }

                    user.addAttribute(attr);
                }

                results.add(user);
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Retrieved users: " + results);
            }

        } catch (SQLException ex) {
            LOG.error("Search operation failed", ex);
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        Connection conn = null;
        try {
            conn = connect();
            Statement statement = conn.createStatement();

            StringBuffer keys = new StringBuffer();
            StringBuffer values = new StringBuffer();

            String accountid = null;
            String value;

            for (WSAttributeValue attr : data) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Bind attribute: " + attr);
                }

                if (attr.getValues() == null || attr.getValues().isEmpty()) {
                    value = null;
                } else if (attr.getValues().size() == 1) {
                        value = attr.getValues().get(0).toString();
                    } else {
                        value = attr.getValues().toString();
                    }

                if (keys.length() > 0) {
                    keys.append(",");
                }

                if ("__NAME__".equals(attr.getName())) {
                    keys.append("userId");
                } else if ("__PASSWORD__".equals(attr.getName())) {
                        keys.append("password");
                    } else {
                        keys.append(attr.getName());
                    }

                if (values.length() > 0) {
                    values.append(",");
                }

                values.append(value == null ? null : "'" + value + "'");

                if (attr.isKey() && !attr.getValues().isEmpty()) {
                    accountid = attr.getValues().get(0).toString();
                }
            }

            String query =
                    "INSERT INTO user (" + keys.toString() + ")"
                    + "VALUES (" + values.toString() + ");";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute query: " + query);
            }

            statement.executeUpdate(query);

            return accountid;
        } catch (SQLException ex) {
            LOG.error("Creation failed", ex);
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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        return 0;
    }

    @Override
    public List<WSChange> sync() throws ProvisioningException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public String resolve(String username) throws ProvisioningException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        Connection conn = null;

        try {
            conn = connect();
            Statement statement = conn.createStatement();

            String query =
                    "SELECT userId FROM user WHERE userId='" + username + "';";

            if (LOG.isDebugEnabled()) {
                LOG.debug("Execute query: " + query);
            }

            ResultSet rs = statement.executeQuery(query);

            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }

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
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
        }

        return username;
    }

    @Override
    public Boolean isAuthenticationSupported() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Operation request received");
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
            LOG.error("Data Source is null");
            return null;
        }

        Connection conn = DataSourceUtils.getConnection(
                DefaultContentLoader.localDataSource);

        if (conn == null) {
            LOG.error("Connection is null");
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
