package com.example.orderservice.fix;

import com.example.orderservice.OrderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FixServer {
    private static final Logger logger = LoggerFactory.getLogger(FixServer.class);

    private final int port;
    private final OrderManager orderManager;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private volatile ServerSocket serverSocket;      // may be rebound
    private volatile boolean running = false;

    public FixServer(int port, OrderManager orderManager) {
        this.port = port;
        this.orderManager = orderManager;
    }

    /* ------------------------------------------------------------------- */
    /** Opens the FIX listener if it is not already running. */
    public synchronized void start() throws IOException {
        if (running) {
            logger.info("FIX server already running on {}", port);
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);          // <<< quick re-bind
        serverSocket.bind(new InetSocketAddress(port));
        running = true;

        logger.info("FIX server STARTED on port {}", port);

        new Thread(this::acceptLoop, "fix-listener-" + port) {{
            setDaemon(true);
            start();
        }};
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                executorService.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running) logger.error("Accept error", e);
            }
        }
    }

    private void handleClient(Socket client) {
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            logger.info("FIX client connected: {}", client.getInetAddress());
            out.println("Connected to Mock FIX Order Service");
            out.println("Total orders: " + orderManager.getOrderCount());
            out.println("Service Status: " + (running ? "RUNNING" : "STOPPED"));
            
            // Simple order summary without JSON serialization
            var orders = orderManager.getAllOrders();
            out.println("Orders count: " + orders.size());
            for (var order : orders) {
                out.println("Order: " + order.getOrderId() + " - " + order.getSymbol() + " " + order.getSide() + " " + order.getOrderQty());
            }

            Thread.sleep(2000);                      // demo pause
        } catch (Exception ex) {
            logger.error("FIX client handler error", ex);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    /* ------------------------------------------------------------------- */
    /** Closes the listener so the Consul FIX check will fail. */
    public synchronized void shutdown() {
        if (!running) {
            logger.info("FIX server already stopped on port {}", port);
            return;
        }

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();                // release the port immediately
                logger.info("FIX server socket CLOSED on port {}", port);
            }
        } catch (IOException e) {
            logger.warn("Error closing server socket", e);
        }
        
        // Shutdown the executor service to stop any pending client handlers
        executorService.shutdown();
        
        logger.info("FIX server STOPPED on port {}", port);
    }

    public boolean isRunning() { return running; }
    
    /**
     * Force close the server socket to ensure the port is released immediately.
     * This is used to make Consul health checks fail quickly.
     */
    public synchronized void forceShutdown() {
        logger.warn("FORCE SHUTDOWN of FIX server on port {}", port);
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.info("FIX server socket FORCE CLOSED on port {}", port);
            }
        } catch (IOException e) {
            logger.warn("Error force closing server socket", e);
        }
        
        executorService.shutdownNow(); // Force shutdown all threads
        logger.info("FIX server FORCE STOPPED on port {}", port);
    }
}