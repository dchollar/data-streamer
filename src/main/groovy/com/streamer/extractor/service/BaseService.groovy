package com.streamer.extractor.service

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.util.concurrent.Future

@Slf4j
@Service
abstract class BaseService {

    @Value('${service.table-threads}')
    Integer numberOfTableThreads
    @Autowired
    DestinationService destinationService
    @Autowired
    SourceService sourceService

    abstract JpaRepository getRepository()

    abstract Class getDomainClass()

    @Transactional(readOnly = true)
    long getTotalCount() {
        repository.count()
    }

    @Transactional(readOnly = true)
    int streamDataOut(Writer writer, Integer lowerLimit, Integer upperLimit) {
        sourceService.streamDataOut(writer, getRepository(), getDomainClass(), lowerLimit, upperLimit)
    }

    @Async
    Future<Map> requestData(String tableName) {
        long startTime = System.currentTimeMillis()
        Map<String, Integer> threadedTables = getThreadedTables()
        if (threadedTables.containsKey(tableName)) {
            int recordCount = requestDataByRange(tableName, threadedTables.get(tableName))
            long duration = System.currentTimeMillis() - startTime
            log.info("ALL Threads. Completed extraction of data for ${tableName}. duration=${duration} totalRecordCount=${recordCount}")
            return new AsyncResult<Map>([tableName: tableName, duration: duration, recordCount: recordCount])
        } else {
            return destinationService.requestData(tableName, getRepository(), getDomainClass())
        }
    }

    @Async
    Future<Map> countAll(String tableName) {
        destinationService.countAll(tableName, getRepository())
    }

    @Transactional
    Map loadDataByRange(String tableName, int lowerLimit, int upperLimit) {
        int records = repository.deleteDataRange(lowerLimit, upperLimit)
        log.info("Number of records deleted for ${tableName} in range ${lowerLimit} to ${upperLimit}: ${records}")
        Future<Map> future = destinationService.requestDataByRange(tableName, getRepository(), getDomainClass(), lowerLimit, upperLimit)
        Map results = future.get()
        results
    }

    private Map<String, Integer> getThreadedTables() {
        return [:]
    }

    private int requestDataByRange(String tableName, int numberOfRecords) {
        int numberOfSlices = calculateNumberOfSlices(numberOfRecords)
        int interval = (numberOfRecords / numberOfSlices) as Integer
        List<Future<Map>> futures = []
        for (int i = 0; i < numberOfSlices; i++) {
            int lowerLimit = (i * interval) + 1
            int upperLimit = ((i + 1) * interval)
            if (i == 0) {
                futures.add(destinationService.requestDataByRange(tableName, getRepository(), getDomainClass(), Integer.MIN_VALUE, interval))
            } else if (i == numberOfSlices - 1) {
                futures.add(destinationService.requestDataByRange(tableName, getRepository(), getDomainClass(), lowerLimit, Integer.MAX_VALUE))
            } else {
                futures.add(destinationService.requestDataByRange(tableName, getRepository(), getDomainClass(), lowerLimit, upperLimit))
            }
            log.info("tableName=${tableName} i=${i} interval=${interval} lowerLimit=${lowerLimit} upperLimit=${upperLimit}")
        }

        List<Map> results = futures.collect { it.get() }

        int recordCount = 0
        results.each {
            int count = (Integer) it.recordCount
            recordCount = recordCount + count
        }
        recordCount
    }

    private int calculateNumberOfSlices(int numberOfRecords) {
        if (numberOfRecords > 80000000) {
            return (numberOfTableThreads * 60)
        } else if (numberOfRecords > 20000000) {
            return (numberOfTableThreads * 10)
        } else if (numberOfRecords > 4000000) {
            return (numberOfTableThreads * 5)
        } else {
            return numberOfTableThreads
        }
    }

}
