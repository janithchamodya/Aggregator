package com.cbc.Aggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RefreshScope
@Component
public class PropertyLoader {

    private final Environment environment;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, String> serviceUrls = new HashMap<>();
    private Map<String, List<String>> servicePaths = new HashMap<>();

    private final String propertiesFilePath = "D:/pro3_email-otp/v2/external.properties"; // Update path as necessary
    private long lastModifiedTime = 0;
    private WatchService watchService;
    private Path filePath;

    @Autowired
    public PropertyLoader(Environment environment) throws Exception {
        this.environment = environment;
        this.filePath = Paths.get(propertiesFilePath).getParent();
        this.watchService = FileSystems.getDefault().newWatchService();
        filePath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        loadProperties();
    }

    private void loadProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesFilePath));
            serviceUrls.clear();
            servicePaths.clear();

            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                if (key.startsWith("routing.") && key.endsWith(".paths")) {
                    String serviceName = key.substring(8, key.length() - 6);
                    List<String> paths = Arrays.asList(value.split(","));
                    servicePaths.put(serviceName, paths);
                    String serviceUrl = properties.getProperty(serviceName + ".service.url");
                    if (serviceUrl != null) {
                        serviceUrls.put(serviceName, serviceUrl);
                    }

                    logger.debug("Loaded service: {} with paths: {}", serviceName, paths);
                }
            }

            logger.info("Properties reloaded successfully.");
        } catch (IOException e) {
            logger.error("Error reloading properties", e);
        }
    }


    public String getTargetUrl(String path) {
        try {

            WatchKey key = watchService.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (key != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        File propertiesFile = new File(propertiesFilePath);
                        long currentModifiedTime = propertiesFile.lastModified();

                        if (currentModifiedTime > lastModifiedTime) {
                            logger.info("Properties file modified. Reloading properties...");
                            lastModifiedTime = currentModifiedTime;
                            loadProperties();
                        }
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            logger.error("Error while monitoring file changes", e);
        }

        return getLoadPro(path);
    }


    private String getLoadPro(String path) {


        for (String serviceName : servicePaths.keySet()) {
            List<String> paths = servicePaths.get(serviceName);

            if (paths.contains(path)) {
                String serviceUrl = serviceUrls.get(serviceName);
                logger.info("Forwarding to service '{}' with URL: {}{}", serviceName, serviceUrl, path);
                return serviceUrl + path;
            }
        }

        logger.error("No matching service found for path: {}", path);
        return null;
    }
}
