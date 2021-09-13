package com.life4ever.shadowsocks4j.proxy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.life4ever.shadowsocks4j.proxy.callback.FileEventCallback;
import com.life4ever.shadowsocks4j.proxy.config.Shadowsocks4jProxyConfig;
import com.life4ever.shadowsocks4j.proxy.exception.Shadowsocks4jProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.life4ever.shadowsocks4j.proxy.constant.ProxyConfigConstant.SHADOWSOCKS4J_CONF_DIR;
import static com.life4ever.shadowsocks4j.proxy.constant.ProxyConfigConstant.SHADOWSOCKS4J_PROXY_JSON_LOCATION;
import static com.life4ever.shadowsocks4j.proxy.constant.StringConstant.BLANK_STRING;

public class FileUtil {

    private static final String FILE_MONITOR_THREAD_NAME = "Shadowsocks4j-FileMonitor";

    private static final long FILE_MODIFY_EVENT_DELAY_TIME = 1000L;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    private static Map<String, FileEventCallback> fileEventCallbackMap;

    private static Map<String, Long> lastModifyTimeMap;

    private FileUtil() {
    }

    public static Shadowsocks4jProxyConfig loadConfigurationFile() throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(SHADOWSOCKS4J_PROXY_JSON_LOCATION))) {
            return OBJECT_MAPPER.readValue(bufferedReader, Shadowsocks4jProxyConfig.class);
        }
    }

    public static void createRuleFile(String ruleFileLocation) throws IOException {
        File ruleFile = new File(ruleFileLocation);
        if (ruleFile.exists()) {
            LOG.warn("Failed to create file {}, because it already exists.", ruleFile.getName());
            return;
        }
        Files.createFile(ruleFile.toPath());
        LOG.info("Succeed to create file {}.", ruleFile.getName());
    }

    public static void updateFile(String filePath, String content) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            // 清空文件
            writer.write(BLANK_STRING);
            writer.flush();
            // 写入数据
            writer.write(content);
            writer.flush();
        }
    }

    public static void startFileWatchService(List<FileEventCallback> fileEventCallbackList) {
        fileEventCallbackMap = fileEventCallbackList.stream()
                .collect(Collectors.toMap(FileEventCallback::getFileName, fileEventCallback -> fileEventCallback));

        lastModifyTimeMap = fileEventCallbackList.stream()
                .collect(Collectors.toMap(FileEventCallback::getFileName, fileEventCallback -> 0L));

        Thread thread = new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                Path path = FileSystems.getDefault().getPath(SHADOWSOCKS4J_CONF_DIR);
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                for (; ; ) {
                    doFileWatchService(watchService);
                }
            } catch (IOException | Shadowsocks4jProxyException e) {
                LOG.error(e.getMessage(), e);
            }
        }, FILE_MONITOR_THREAD_NAME);

        thread.setDaemon(true);
        thread.start();
        LOG.info("Start daemon thread {}.", thread.getName());
    }

    private static void doFileWatchService(WatchService watchService) throws Shadowsocks4jProxyException {
        try {
            WatchKey watchKey = watchService.take();
            for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                WatchEvent.Kind<?> kind = watchEvent.kind();
                String fileName = ((Path) watchEvent.context()).getFileName().toString();
                FileEventCallback fileEventCallback = fileEventCallbackMap.get(fileName);
                Long lastModifyTime = lastModifyTimeMap.get(fileName);

                if (fileEventCallback == null) {
                    break;
                }

                if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
                    LOG.info("Trigger file {} create event.", fileName);
                    fileEventCallback.onCreate();
                } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
                    LOG.info("Trigger file {} delete event.", fileName);
                    fileEventCallback.onDelete();
                } else if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)
                        && ((getCurrentTime() - lastModifyTime >= FILE_MODIFY_EVENT_DELAY_TIME) || lastModifyTime == 0L)) {
                    LOG.info("Trigger file {} modify event.", fileName);
                    fileEventCallback.onModify();
                    lastModifyTimeMap.put(fileName, getCurrentTime());
                }
            }
            watchKey.reset();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Shadowsocks4jProxyException(e.getMessage(), e);
        }
    }

    private static long getCurrentTime() {
        return System.currentTimeMillis();
    }

}
