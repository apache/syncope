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
package org.syncope.core.persistence.transaction;

import java.io.Serializable;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.TransactionFactory;

/**
 * TransactionManager for usage with Spring.
 * It normally goes as per <tx:jta-transaction-manager/> but,
 * when not available, uses a configured JpaTransactionManager as fallback.
 *
 * @see JtaTransactionManager
 * @see JpaTransactionManager
 */
public class SpringJTAWithJPAFallbackTransactionManager
        implements PlatformTransactionManager,
        TransactionFactory, InitializingBean, Serializable {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            SpringJTAWithJPAFallbackTransactionManager.class);
    /**
     * Spring's JPA Transaction Manager.
     */
    private JpaTransactionManager jpaTransactionManager;
    /**
     * Spring's JTA Transaction Manager.
     */
    private JtaTransactionManager jtaTransactionManager;

    public SpringJTAWithJPAFallbackTransactionManager() {
        jtaTransactionManager = new JtaTransactionManager();
    }

    public SpringJTAWithJPAFallbackTransactionManager(
            final JpaTransactionManager jpaTransactionManager) {

        this();
        setJpaTransactionManager(jpaTransactionManager);
    }

    /**
     * @return fallback JPA Transaction Manager
     */
    public final JpaTransactionManager getJpaTransactionManager() {
        return jpaTransactionManager;
    }

    public final void setJpaTransactionManager(
            final JpaTransactionManager jpaTransactionManager) {

        this.jpaTransactionManager = jpaTransactionManager;
    }

    private TransactionFactory getTransactionFactory() {
        return (TransactionFactory) (jtaTransactionManager != null
                ? jtaTransactionManager : jpaTransactionManager);
    }

    private PlatformTransactionManager getPlatformTransactionManager() {
        return jtaTransactionManager != null
                ? jtaTransactionManager : jpaTransactionManager;
    }

    @Override
    public final TransactionStatus getTransaction(
            final TransactionDefinition definition)
            throws TransactionException {

        return getPlatformTransactionManager().getTransaction(definition);
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        getPlatformTransactionManager().commit(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        getPlatformTransactionManager().rollback(status);
    }

    @Override
    public Transaction createTransaction(String name, int timeout)
            throws NotSupportedException, SystemException {

        return getTransactionFactory().createTransaction(name, timeout);
    }

    @Override
    public boolean supportsResourceAdapterManagedTransactions() {
        return getTransactionFactory().
                supportsResourceAdapterManagedTransactions();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            jtaTransactionManager.afterPropertiesSet();
        } catch (Throwable t) {
            jtaTransactionManager = null;

            LOG.error("Could not instantiate JtaTransactionManager, "
                    + "reverting to JpaTransactionManager");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Here's why", t);
            }
        }
    }
}
