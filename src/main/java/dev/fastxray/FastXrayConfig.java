package dev.fastxray;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class FastXrayConfig {
    private static final String DEFAULT_PATH = "D:/resourcepacks/Xray_Ultimate_26.X_v5.4.3.zip";

    public static volatile Path packPath = Paths.get(DEFAULT_PATH);

    private FastXrayConfig() {}

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("fastxray.properties");
        Properties props = new Properties();
        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            } else {
                props.setProperty("packPath", DEFAULT_PATH);
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out, "Fast X-Ray. Use forward slashes (D:/dir/pack.zip) or doubled backslashes.");
                }
            }
            String value = props.getProperty("packPath", DEFAULT_PATH).trim();
            packPath = Paths.get(value);
        } catch (IOException | RuntimeException e) {
            FastXrayClient.LOGGER.error("Failed to load config, using default path", e);
            packPath = Paths.get(DEFAULT_PATH);
        }
    }
}
