package br.com.rafaelvieira.taskmanagement.config;

import org.apache.catalina.Context;
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
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(@NotNull Context context) {
                context.setAllowCasualMultipartParsing(true);
            }
        };
    }

    @Bean
    public WebServerFactoryCustomizer<@NotNull TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            // Configura o limite de file count para multipart
            factory.addContextCustomizers(context -> context.setAllowCasualMultipartParsing(true));

            factory.addConnectorCustomizers(
                    (Connector connector) -> {
                        if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
                            // Permite payloads maiores antes de descartar o corpo
                            protocol.setMaxSwallowSize(MAX_REQUEST_SIZE);
                        }
                        connector.setMaxPostSize(MAX_REQUEST_SIZE);
                        connector.setMaxSavePostSize(MAX_REQUEST_SIZE);

                        // Configura o limite de partes do multipart
                        connector.setProperty("maxParameterCount", "10000");
                    });
        };
    }
}
