package org.appmeta.launch;
/*
 * @project app-meta-server
 * @file    org.appmeta.launch.WatchThread
 * CREATE   2023年11月29日 14:06 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class WatchWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(WatchWorker.class);

    private Path dir;
    private List<String> filenames;
    private WatchService watcher;
    private WatchHandler handler;
    private int delay = 10;              //指定秒内仅触发一次

    private Map<String, Long> timeMap = new HashMap<>();

    public WatchWorker(Path dir, List<String> names, WatchHandler handler) throws IOException {
        this.dir = dir;
        this.filenames = names;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.handler = handler;

        this.dir.register(
                watcher,
                new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_MODIFY},
                SensitivityWatchEventModifier.LOW
        );
        logger.info("开始监听目录 {}，目标{}", dir, filenames);
    }

    @Override
    public void run() {
        while (true) try {
            // 等待文件的变化（阻塞）
            WatchKey key = watcher.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW)   continue;

                if(logger.isDebugEnabled()) logger.debug("监听到变化, KIND={} COUNT={}", event.kind(), event.count());

                Path target = (Path) event.context();
                if(filenames.contains(target.toString())){
                    //判断是否重复
                    long last = timeMap.getOrDefault(target.toString(), 0L);
                    if(System.currentTimeMillis() - last > delay * 1000L){
                        if(logger.isDebugEnabled()) logger.debug("监听到文件 {} 变动", target);
                        if(handler != null) handler.onChange(target);
                    }
                    timeMap.put(target.toString(), System.currentTimeMillis());
                }
            }

            key.reset();
        } catch (Exception e) {
            logger.error("监听作业出错", e);
        }
    }
}
