package com.streamer.extractor.service


import groovy.util.logging.Slf4j
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import java.util.concurrent.ConcurrentHashMap

@Slf4j
@Service
class KeepAliveService {

    static String END_OF_LINE = '\n'
    static String KEEP_ALIVE_MESSAGE = 'BOB'
    static Map<String, Boolean> KEEP_ALIVE = [:] as ConcurrentHashMap
    static long SLEEP_SECONDS = 2

    static void stop(String tableName) {
        KEEP_ALIVE.put(tableName, false)
        Thread.currentThread().sleep(SLEEP_SECONDS * 1001)
    }

    static void start(String tableName) {
        KEEP_ALIVE.put(tableName, true)
    }

    @Async
    void keepAlive(Writer writer, String tableName) {
        log.info("Keeping thread alive for ${tableName}")
        start(tableName)
        while (KEEP_ALIVE.get(tableName)) {
            log.info("Writing keep alive record for ${tableName}")
            writeRecord(writer)
            Thread.currentThread().sleep(SLEEP_SECONDS * 1000)
        }
        log.info("Done keeping thread alive for ${tableName}")
    }

    static void writeRecord(Writer writer) {
        writer.write(KEEP_ALIVE_MESSAGE)
        writer.write(END_OF_LINE)
        writer.flush()
    }
}
