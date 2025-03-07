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
package org.apache.syncope.fit.buildtools.cxf;

import jakarta.jws.WebService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import net.tirasa.connid.bundles.soap.exceptions.ProvisioningException;
import net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning;
import net.tirasa.connid.bundles.soap.to.WSAttribute;
import net.tirasa.connid.bundles.soap.to.WSAttributeValue;
import net.tirasa.connid.bundles.soap.to.WSChange;
import net.tirasa.connid.bundles.soap.to.WSUser;
import net.tirasa.connid.bundles.soap.utilities.Operand;
import org.identityconnectors.common.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

@WebService(
        endpointInterface = "net.tirasa.connid.bundles.soap.provisioning.interfaces.Provisioning",
        serviceName = "Provisioning")
public class ProvisioningImpl implements Provisioning {

    private static final Logger LOG = LoggerFactory.getLogger(Provisioning.class);

    private final DataSource dataSource;

    public ProvisioningImpl(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String delete(final String accountid) throws ProvisioningException {
        LOG.debug("Delete request received");

        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);

            try (PreparedStatement statement = conn.prepareStatement("DELETE FROM testuser WHERE userId=?")) {
                statement.setString(1, accountid);

                String query = "DELETE FROM testuser WHERE userId='" + accountid + "';";
                LOG.debug("Execute query: {}", query);

                statement.executeUpdate();
            }

            return accountid;
        } catch (SQLException e) {
            throw new ProvisioningException("Delete operation failed", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public Boolean isSyncSupported() {
        LOG.debug("isSyncSupported request received");

        return Boolean.FALSE;
    }

    @Override
    public String checkAlive() {
        return "OK";
    }

    @Override
    public String update(final String accountid, final List<WSAttributeValue> data)
            throws ProvisioningException {

        LOG.debug("Update request received");

        if (data == null || data.isEmpty()) {
            LOG.warn("Empty data received");
            return accountid;
        }

        List<WSAttribute> schema = schema();
        Set<String> schemaNames = new HashSet<>();
        schema.forEach(attr -> schemaNames.add(attr.getName()));
        schemaNames.add("__NAME__");
        schemaNames.add("__PASSWORD__");

        Connection conn = null;

        try {
            conn = DataSourceUtils.getConnection(dataSource);

            String value;

            StringBuilder set = new StringBuilder();
            for (WSAttributeValue attr : data) {
                if (schemaNames.contains(attr.getName())) {
                    if (attr.getValues() == null || attr.getValues().isEmpty()) {
                        value = null;
                    } else if (attr.getValues().size() == 1) {
                        value = attr.getValues().getFirst().toString();
                    } else {
                        value = attr.getValues().toString();
                    }

                    if (!attr.isKey() || !accountid.equals(value)) {
                        if (set.length() > 0) {
                            set.append(',');
                        }

                        if (null == attr.getName()) {
                            set.append(attr.getName()).append('=');
                        } else {
                            switch (attr.getName()) {
                                case "__NAME__" ->
                                    set.append("userId=");
                                case "__PASSWORD__" ->
                                    set.append("password=");
                                default ->
                                    set.append(attr.getName()).append('=');
                            }
                        }

                        set.append(Optional.ofNullable(value).map(s -> '\'' + s + '\'').orElse(null));
                    }
                }
            }

            if (set.length() > 0) {
                try (PreparedStatement statement =
                        conn.prepareStatement("UPDATE testuser SET " + set + " WHERE userId=?")) {
                    statement.setString(1, accountid);
                    String query = "UPDATE testuser SET " + set + " WHERE userId='" + accountid + "';";
                    LOG.debug("Execute query: {}", query);

                    statement.executeUpdate();
                }
            }

            return accountid;
        } catch (SQLException e) {
            LOG.error("Update failed", e);
            throw new ProvisioningException("Update operation failed", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public List<WSUser> query(final Operand query) {
        LOG.debug("Query request received");

        List<WSUser> results = new ArrayList<>();

        Connection conn = null;
        try {

            String queryString = "SELECT * FROM testuser" + (Optional.ofNullable(query)
                    .map(operand -> " WHERE " + operand).orElse(""));

            queryString = queryString.replaceAll("__NAME__", "userId").
                    replaceAll("__UID__", "userId").
                    replaceAll("__PASSWORD__", "password");

            LOG.debug("Execute query: {}", queryString);

            if (queryString == null || queryString.length() == 0) {
                throw new SQLException("Invalid query [" + queryString + ']');
            }

            conn = DataSourceUtils.getConnection(dataSource);
            try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(queryString)) {
                ResultSetMetaData metaData = rs.getMetaData();
                LOG.debug("Metadata: {}", metaData);

                while (rs.next()) {
                    WSUser user = new WSUser();

                    for (int i = 0; i < metaData.getColumnCount(); i++) {
                        WSAttributeValue attr = new WSAttributeValue();
                        attr.setName(metaData.getColumnLabel(i + 1));
                        if (StringUtil.isNotBlank(rs.getString(i + 1))) {
                            attr.addValue(rs.getString(i + 1));
                        }
                        if ("userId".equalsIgnoreCase(metaData.getColumnName(i + 1))) {
                            attr.setKey(true);
                            user.setAccountid(rs.getString(i + 1));
                        }

                        user.addAttribute(attr);
                    }

                    results.add(user);
                }

                LOG.debug("Retrieved users: {}", results);
            }
        } catch (SQLException e) {
            LOG.error("Search operation failed", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        return results;
    }

    @Override
    public String create(final List<WSAttributeValue> data) throws ProvisioningException {
        LOG.debug("Create request received with data {}", data);

        final List<WSAttribute> schema = schema();
        final Set<String> schemaNames = new HashSet<>();
        schema.forEach(attr -> schemaNames.add(attr.getName()));
        schemaNames.add("__NAME__");
        schemaNames.add("__PASSWORD__");

        Connection conn = null;
        String query = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            String accountid;
            try (Statement statement = conn.createStatement()) {
                final StringBuilder keys = new StringBuilder();
                final StringBuilder values = new StringBuilder();
                accountid = null;
                String value;
                for (WSAttributeValue attr : data) {
                    if (schemaNames.contains(attr.getName())) {
                        LOG.debug("Bind attribute: {}", attr);

                        if (attr.getValues() == null || attr.getValues().isEmpty()) {
                            value = null;
                        } else if (attr.getValues().size() == 1) {
                            value = attr.getValues().getFirst().toString();
                        } else {
                            value = attr.getValues().toString();
                        }

                        if (keys.length() > 0) {
                            keys.append(',');
                        }

                        if (null == attr.getName()) {
                            keys.append(attr.getName());
                        } else {
                            switch (attr.getName()) {
                                case "__NAME__" ->
                                    keys.append("userId");
                                case "__PASSWORD__" ->
                                    keys.append("password");
                                default ->
                                    keys.append(attr.getName());
                            }
                        }

                        if (values.length() > 0) {
                            values.append(',');
                        }

                        values.append(Optional.ofNullable(value).map(s -> '\'' + s + '\'').orElse(null));

                        if (attr.isKey() && !attr.getValues().isEmpty()) {
                            accountid = attr.getValues().getFirst().toString();
                        }
                    }
                }
                query = "INSERT INTO testuser (" + keys + ") VALUES (" + values + ')';
                LOG.debug("Execute query: {}", query);
                statement.executeUpdate(query);
            }

            return accountid;
        } catch (SQLException e) {
            LOG.error("Creation failed:\n{}", query, e);
            throw new ProvisioningException("Create operation failed", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public int getLatestChangeNumber() {

        LOG.debug("getLatestChangeNumber request received");

        return 0;
    }

    @Override
    public List<WSChange> sync() {

        LOG.debug("sync request received");

        return List.of();
    }

    @Override
    public String resolve(final String username) throws ProvisioningException {

        LOG.debug("Resolve request operation received: {}", username);

        String resolved = "";

        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            PreparedStatement statement = conn.prepareStatement("SELECT userId FROM testuser WHERE userId=?");
            statement.setString(1, username);

            String query = "SELECT userId FROM testuser WHERE userId='" + username + "';";
            LOG.debug("Execute query: {}", query);

            ResultSet rs = statement.executeQuery();

            resolved = rs.next() ? rs.getString(1) : null;

            statement.close();

            if (resolved == null) {
                statement = conn.prepareStatement("SELECT roleName FROM role WHERE roleName=?");
                statement.setString(1, username);
                final String roleQuery = "SELECT roleName FROM role WHERE roleName='" + username + "';";
                LOG.debug("Execute query: {}", roleQuery);

                rs = statement.executeQuery();

                resolved = rs.next() ? rs.getString(1) : null;

                statement.close();
            }
        } catch (SQLException e) {
            throw new ProvisioningException("Resolve operation failed", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }

        return resolved;
    }

    @Override
    public List<WSAttribute> schema() {
        LOG.debug("schema request received");

        final List<WSAttribute> attrs = new ArrayList<>();

        WSAttribute attr = new WSAttribute();
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
        attr.setName("fullname");
        attr.setNullable(false);
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
    public String authenticate(final String username, final String password) {

        LOG.debug("authenticate request received");

        return username;
    }

    @Override
    public Boolean isAuthenticationSupported() {
        LOG.debug("isAuthenticationSupported request received");

        return Boolean.FALSE;
    }
}
