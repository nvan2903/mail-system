package com.email.mailclient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationLoader {
    private final Properties config = new Properties();
    private final String configFileLocation;

    public ConfigurationLoader() {
        this("config/mailclient.properties");
    }

    public ConfigurationLoader(String configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    public void loadConfiguration() throws IOException {
        File configFile = new File(configFileLocation);
        if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
            System.err.println("Không thể đọc tệp cấu hình tại " + configFile.getAbsolutePath() + ". Sử dụng giá trị mặc định.");
            // Bạn có thể xử lý lỗi này theo cách cần thiết cho ứng dụng của bạn.
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);

            }
        }
    }

    public String getProperty(String key) {
        return config.getProperty(key);
    }
}
