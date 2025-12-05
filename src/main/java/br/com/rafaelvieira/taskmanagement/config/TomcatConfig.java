package br.com.rafaelvieira.taskmanagement.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração customizada do Tomcat para corrigir o erro FileCountLimitExceededException. Este
 * erro ocorre quando há muitos campos de formulário multipart.
 */
@Configuration
public class TomcatConfig {

    private static final int MAX_REQUEST_SIZE = 50 * 1024 * 1024;

    @Bean
    public WebServerFactoryCustomizer<@NotNull TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory ->
                factory.addConnectorCustomizers(
                        (Connector connector) -> {
                            if (connector.getProtocolHandler()
                                    instanceof Http11NioProtocol protocol) {
                                protocol.setMaxSwallowSize(MAX_REQUEST_SIZE);
                            }
                            connector.setMaxPostSize(MAX_REQUEST_SIZE);
                            connector.setMaxSavePostSize(MAX_REQUEST_SIZE);
                        });
    }
}
