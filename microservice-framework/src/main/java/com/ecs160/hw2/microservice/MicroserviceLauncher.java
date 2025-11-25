package com.ecs160.hw2.microservice;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Launches microservices that are annotated with @Microservice and @Endpoint.
 */
public class MicroserviceLauncher {
    private Map<String, Method> endpointMap;
    private Map<String, Object> serviceInstances;
    private ExecutorService executorService;
    private int port;
    private boolean running;

    public MicroserviceLauncher() {
        this.endpointMap = new HashMap<>();
        this.serviceInstances = new HashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }

    /**
     * Scans for classes annotated with @Microservice and registers their endpoints.
     */
    public void scanAndRegisterServices(String packageName) throws Exception {
        // Get all classes in the package
        List<Class<?>> classes = getClassesInPackage(packageName);
        
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Microservice.class)) {
                registerMicroservice(clazz);
            }
        }
    }

    /**
     * Registers a single microservice class.
     */
    public void registerMicroservice(Class<?> clazz) throws Exception {
        Object serviceInstance = clazz.getDeclaredConstructor().newInstance();
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(Endpoint.class)) {
                Endpoint endpoint = method.getAnnotation(Endpoint.class);
                String url = endpoint.url();
                
                // Verify method signature: String handleRequest(String input)
                if (method.getReturnType() != String.class) {
                    throw new RuntimeException("Method " + method.getName() + 
                        " must return String");
                }
                
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1 || paramTypes[0] != String.class) {
                    throw new RuntimeException("Method " + method.getName() + 
                        " must have exactly one String parameter");
                }
                
                endpointMap.put(url, method);
                serviceInstances.put(url, serviceInstance);
                
                System.out.println("Registered endpoint: " + url);
            }
        }
    }

    /**
     * Launches the microservice server to listen at the specified port.
     * Runs an infinite loop, inspecting each incoming request, mapping it to the right 
     * microservice and invoking it, collecting the response, and sending it back to the client.
     */
    public boolean launch(int port) {
        this.port = port;
        
        if (endpointMap.isEmpty()) {
            System.err.println("No endpoints registered. Please register microservices first.");
            return false;
        }

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(executorService);
            
            // Register a handler for all paths
            server.createContext("/", new MicroserviceRequestHandler());
            
            this.running = true;
            server.start();
            
            System.out.println("Microservice server started on port " + port);
            System.out.println("Registered endpoints: " + endpointMap.keySet());
            
            // Keep the server running
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                server.stop(0);
                stop();
            }));
            
            return true;
        } catch (java.net.BindException e) {
            System.err.println("Error starting server: Port " + port + " is already in use.");
            System.err.println("Please stop any existing server or use a different port.");
            System.err.println("To kill the process using port " + port + ", run: lsof -ti:" + port + " | xargs kill");
            return false;
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HTTP request handler that routes requests to microservice endpoints.
     */
    private class MicroserviceRequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String uri = exchange.getRequestURI().toString();
            
            // Only handle GET requests for now
            if (!"GET".equalsIgnoreCase(method)) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            
            // Parse query parameters
            Map<String, String> params = parseQueryString(uri);
            String endpoint = extractEndpoint(uri);
            String input = params.getOrDefault("input", "");
            
            // Find the endpoint handler
            Method handlerMethod = endpointMap.get(endpoint);
            if (handlerMethod == null) {
                sendResponse(exchange, 404, "Endpoint not found: " + endpoint);
                return;
            }
            
            try {
                Object serviceInstance = serviceInstances.get(endpoint);
                handlerMethod.setAccessible(true);
                String result = (String) handlerMethod.invoke(serviceInstance, input);
                
                sendResponse(exchange, 200, result != null ? result : "");
            } catch (Exception e) {
                System.err.println("Error invoking endpoint " + endpoint + ": " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Extracts the endpoint from the URI (removes query string and leading /).
     */
    private String extractEndpoint(String uri) {
        int queryIndex = uri.indexOf('?');
        if (queryIndex >= 0) {
            uri = uri.substring(0, queryIndex);
        }
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        return uri;
    }

    /**
     * Parses query string parameters.
     */
    private Map<String, String> parseQueryString(String uri) {
        Map<String, String> params = new HashMap<>();
        int queryIndex = uri.indexOf('?');
        if (queryIndex < 0) {
            return params;
        }
        
        String queryString = uri.substring(queryIndex + 1);
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int eqIndex = pair.indexOf('=');
            if (eqIndex >= 0) {
                String key = pair.substring(0, eqIndex);
                String value = pair.substring(eqIndex + 1);
                // URL decode
                try {
                    key = URLDecoder.decode(key, StandardCharsets.UTF_8.toString());
                    value = URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
                } catch (Exception e) {
                    // Ignore
                }
                params.put(key, value);
            }
        }
        return params;
    }

    /**
     * Gets all classes in a package by scanning the classpath.
     * This is a simplified implementation that looks for classes in the package.
     */
    private List<Class<?>> getClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        
        // This is a simplified approach - in production, you'd use a library like Reflections
        // For now, we'll require manual registration of microservices
        // This method is kept for API compatibility but won't automatically discover classes
        
        return classes;
    }
    
    /**
     * Manually register a microservice class.
     */
    public void registerMicroservice(Class<?>... serviceClasses) throws Exception {
        for (Class<?> clazz : serviceClasses) {
            registerMicroservice(clazz);
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        this.running = false;
        executorService.shutdown();
    }
}

