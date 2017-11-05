package net.twasi.core.webinterface;

import com.sun.net.httpserver.HttpServer;
import net.twasi.core.config.Config;
import net.twasi.core.logger.TwasiLogger;
import net.twasi.core.webinterface.registry.ApiRegistry;
import net.twasi.core.webinterface.registry.AuthRegistry;

import java.net.InetSocketAddress;

public class WebInterfaceApp {

        public static void start() {
            try {
                // Create server
                HttpServer server = HttpServer.create(new InetSocketAddress(Config.catalog.webinterface.port), 0);

                // Register all handlers
                ApiRegistry.register(server);
                AuthRegistry.register(server);

                // Handle all other request static
                server.createContext("/", new StaticHandler());

                // Start server, show message
                server.start();
                TwasiLogger.log.info("Web interface started on port " + Config.catalog.webinterface.port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }