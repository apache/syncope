package org.syncope.identityconnectors.bundles.staticwebservice;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WebServiceConnection {

    /**
     * Logger definition.
     */
    private static final Logger log =
            LoggerFactory.getLogger(WebServiceConnection.class);

    private final String SUCCESS = "OK";

    private final String APPLICATIONCONTEXT = "/beans.xml";

    private Provisioning provisioning;

    public WebServiceConnection(WebServiceConfiguration configuration) {
        try {

            ApplicationContext context = new ClassPathXmlApplicationContext(
                    new String[]{APPLICATIONCONTEXT});

            JaxWsProxyFactoryBean proxyFactory =
                    (JaxWsProxyFactoryBean) context.getBean(
                    JaxWsProxyFactoryBean.class);

            configuration.validate();

            proxyFactory.setAddress(
                    configuration.getEndpoint());

            proxyFactory.setServiceClass(
                    Class.forName(configuration.getServicename()));

            provisioning = (Provisioning) proxyFactory.create();

        } catch (IllegalArgumentException e) {

            if (log.isErrorEnabled()) {
                log.error("Invalid confoguration", e);
            }

        } catch (ClassNotFoundException e) {

            if (log.isErrorEnabled()) {
                log.error("Provisioning class" +
                        " \"" + configuration.getServicename() + "\" " +
                        "not found", e);
            }

        } catch (Throwable t) {

            if (log.isErrorEnabled()) {
                log.error("Unknown exception", t);
            }

        }
    }

    /**
     * Release internal resources
     */
    public void dispose() {
        provisioning = null;
    }

    /**
     * If internal connection is not usable, throw IllegalStateException
     */
    public void test() {
        if (provisioning == null)
            throw new IllegalStateException("Service port not found.");

        String res = provisioning.checkAlive();

        if (!SUCCESS.equals(res))
            throw new IllegalStateException("Invalid response.");
    }

    public Provisioning getProvisioning() {
        return provisioning;
    }
}
