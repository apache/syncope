package org.apache.syncope.client.lib;

import java.net.URI;
import java.util.List;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;

public final class WebClientBuilder {

    private WebClientBuilder() {
    }

    public static WebClient build(final String address,
            final String username,
            final String password,
            final List<?> providers) {
        return setAsync(WebClient.create(address, providers, username, password, null));
    }

    public static WebClient build(final String address) {
        return setAsync(WebClient.create(address));
    }

    public static WebClient build(final URI uri) {
        return setAsync(WebClient.create(uri));
    }

    protected static WebClient setAsync(final WebClient webClient) {
        ClientConfiguration config = WebClient.getConfig(webClient);
        config.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);

        return webClient;
    }
}
