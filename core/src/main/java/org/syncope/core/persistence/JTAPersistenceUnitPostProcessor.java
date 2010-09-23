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
package org.syncope.core.persistence;

import java.util.Properties;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

public class JTAPersistenceUnitPostProcessor
        implements PersistenceUnitPostProcessor {

    private boolean jtaMode = false;
    private DataSource jtaDataSource;
    private String transactionManagerLookupKey;
    private String transactionManagerLookupValue;
    private PersistenceUnitTransactionType transactionType =
            PersistenceUnitTransactionType.RESOURCE_LOCAL;

    @Override
    public final void postProcessPersistenceUnitInfo(
            final MutablePersistenceUnitInfo mutablePersistenceUnitInfo) {

        if (jtaMode) {
            transactionType = PersistenceUnitTransactionType.JTA;
            mutablePersistenceUnitInfo.setJtaDataSource(getJtaDataSource());

            Properties persistenceUnitProps =
                    mutablePersistenceUnitInfo.getProperties();
            persistenceUnitProps.setProperty(transactionManagerLookupKey,
                    transactionManagerLookupValue);
            mutablePersistenceUnitInfo.setProperties(persistenceUnitProps);
        }
        mutablePersistenceUnitInfo.setTransactionType(transactionType);
    }

    public final boolean isJtaMode() {
        return jtaMode;
    }

    public final void setJtaMode(final boolean jtaMode) {
        this.jtaMode = jtaMode;
    }

    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }

    public final void setJtaDataSource(final DataSource jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }

    public final String getTransactionManagerLookupKey() {
        return transactionManagerLookupKey;
    }

    public final void setTransactionManagerLookupKey(
            final String transactionManagerLookupKey) {

        this.transactionManagerLookupKey = transactionManagerLookupKey;
    }

    public final String getTransactionManagerLookupValue() {
        return transactionManagerLookupValue;
    }

    public final void setTransactionManagerLookupValue(
            final String transactionManagerLookupValue) {

        this.transactionManagerLookupValue = transactionManagerLookupValue;
    }
}
