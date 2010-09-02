package org.syncope.identityconnectors.bundles.staticwebservice.wstarget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.datatype.DefaultDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DefaultContentLoader implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(
            DefaultContentLoader.class);

    private final String DBSCHEMA = "/schema.sql";

    public static DataSource localDataSource = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        WebApplicationContext springContext =
                WebApplicationContextUtils.getWebApplicationContext(
                sce.getServletContext());

        if (springContext == null) {
            log.error("Invalid Spring context");
            return;
        }

        DataSource dataSource =
                (DataSource) springContext.getBean("localDataSource");

        DefaultContentLoader.localDataSource = dataSource;

        DefaultDataTypeFactory dbUnitDataTypeFactory =
                (DefaultDataTypeFactory) springContext.getBean(
                "dbUnitDataTypeFactory");

        Connection conn = DataSourceUtils.getConnection(dataSource);

        // create schema
        StringBuilder statement = new StringBuilder();

        InputStream dbschema =
                DefaultContentLoader.class.getResourceAsStream(DBSCHEMA);

        BufferedReader buff = new BufferedReader(
                new InputStreamReader(dbschema));

        String line = null;

        try {
            while ((line = buff.readLine()) != null) {
                statement.append(line);
            }
        } catch (IOException e) {
            log.error("Error reading file " + DBSCHEMA, e);
            return;
        }

        Statement st = null;

        try {

            st = conn.createStatement();
            st.execute(statement.toString());

        } catch (SQLException e) {
            log.error("Error creating schema:\n" + statement.toString(), e);
            return;
        } finally {
            try {
                st.close();
            } catch (Throwable t) {
                // ignore exception
            }
        }

        try {
            IDatabaseConnection dbUnitConn = new DatabaseConnection(conn);

            DatabaseConfig config = dbUnitConn.getConfig();
            config.setProperty(
                    "http://www.dbunit.org/properties/datatypeFactory",
                    dbUnitDataTypeFactory);

            boolean existingData = false;
            IDataSet existingDataSet = dbUnitConn.createDataSet();
            for (ITableIterator itor = existingDataSet.iterator();
                    itor.next() && !existingData;) {

                existingData = (itor.getTable().getRowCount() > 0);
            }


            FlatXmlDataSetBuilder dataSetBuilder =
                    new FlatXmlDataSetBuilder();
            dataSetBuilder.setColumnSensing(true);
            IDataSet dataSet = dataSetBuilder.build(
                    getClass().getResourceAsStream("/content.xml"));

            DatabaseOperation.REFRESH.execute(dbUnitConn, dataSet);

        } catch (Throwable t) {
            log.error("Error loding default content", t);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
