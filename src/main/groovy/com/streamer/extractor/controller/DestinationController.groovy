package com.streamer.extractor.controller

import com.streamer.extractor.enumeration.Entities
import com.streamer.extractor.enumeration.ServiceType
import com.streamer.extractor.enumeration.Stages
import com.streamer.extractor.service.NativeSqlService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.Future

@Slf4j
@RestController
class DestinationController extends BaseController {

    @Autowired
    NativeSqlService nativeSqlService

    @GetMapping('/countAll')
    ResponseEntity countAll(@RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'counting all data')
        log.info('Starting count all data')

        List<Future<Map>> futures = []
        if (stage) {
            futures.addAll(countTables(stage))
        } else {
            Stages.values().each {
                futures.addAll(countTables(it))
            }
        }

        List<Map> results = futures.collect { it.get() }

        LinkedHashMap<String, List> segmentedResults = organizeResults(results)

        long duration = System.currentTimeMillis() - startTime
        log.info("Completed count all data: duration=${duration}")

        return new ResponseEntity(segmentedResults, HttpStatus.OK)
    }

    static private LinkedHashMap<String, List> organizeResults(List<Map> results) {
        Map<String, List<Map>> segmentedResults = ['ISSUES': [], 'NO_ISSUES': []]
        results.each { Map result ->
            result.each { key, value ->
                if (value.DIFFERENCE == 0) {
                    segmentedResults.NO_ISSUES.add(result)
                } else {
                    segmentedResults.ISSUES.add(result)
                }
            }
        }
        segmentedResults
    }

    private List<Future<Map>> countTables(Stages stage) {
        List<Future<Map>> futures = []
        findTableNames(stage).each { String tableName ->
            Future<Map> result = getService(context, tableName).countAll(tableName)
            futures.add(result)
        }
        futures
    }

    @GetMapping('/dropConstraints')
    ResponseEntity dropConstraints(@RequestParam(name = 'tableName', required = false) String tableName,
                                   @RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'drop constraints')
        List<String> errors = nativeSqlService.dropConstraints(tableName, stage)
        Map results = [message: 'Done', errors: errors, duration: (System.currentTimeMillis() - startTime)]
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/createConstraints')
    ResponseEntity createConstraints(@RequestParam(name = 'tableName', required = false) String tableName,
                                     @RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'create constraints')
        List<String> errors = nativeSqlService.createConstraints(tableName, stage)
        Map results = [message: 'Done', errors: errors, duration: (System.currentTimeMillis() - startTime)]
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/resetSequences')
    ResponseEntity resetSequences(@RequestParam(name = 'tableName', required = false) String tableName,
                                  @RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'reset sequences')
        List<String> errors = nativeSqlService.resetSequences(tableName, stage)
        Map results = [message: 'Done', errors: errors, duration: (System.currentTimeMillis() - startTime)]
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/truncateTables')
    ResponseEntity truncateTables(@RequestParam(name = 'tableName', required = false) String tableName,
                                  @RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'reset sequences')
        List<String> errors = nativeSqlService.truncateTables(tableName, stage)
        Map results = [message: 'Done', errors: errors, duration: (System.currentTimeMillis() - startTime)]
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/getDataByRange')
    ResponseEntity getDataByRange(@RequestParam(name = 'tableName', required = true) String tableName,
                                  @RequestParam(name = 'lowerLimit', required = true) int lowerLimit,
                                  @RequestParam(name = 'upperLimit', required = true) int upperLimit) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'write data')
        log.info("Starting getDataByRange for ${tableName} from ${lowerLimit} to ${upperLimit}")
        Map results = getService(context, tableName).loadDataByRange(tableName, lowerLimit, upperLimit)
        long duration = System.currentTimeMillis() - startTime
        log.info("Completed getDataByRange for ${tableName} from ${lowerLimit} to ${upperLimit} duration=${duration}")
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/getData')
    ResponseEntity getData(@RequestParam(name = 'tableName', required = true) String tableName) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'write data')
        log.info("Starting getData for ${tableName}")
        Future<Map> result = getService(context, tableName).requestData(tableName)
        List<Map> results = []
        results.add(result.get())
        long duration = System.currentTimeMillis() - startTime
        results.add([TotalDuration: duration])
        log.info("Completed getData for ${tableName} duration=${duration}")
        return new ResponseEntity(results, HttpStatus.OK)
    }

    @GetMapping('/getAllData')
    ResponseEntity getAllData(@RequestParam(name = 'stage', required = false) Stages stage) {
        long startTime = System.currentTimeMillis()
        validate(ServiceType.DESTINATION, 'write data')
        log.info('Starting getAllData')

        List<Map> results = []
        if (stage) {
            results.addAll(processEntities(findTableNames(stage)))
        } else {
            Stages.values().each {
                if (!(it.name().startsWith('PRE') || it.name().startsWith('POST'))) {
                    long stageStartTime = System.currentTimeMillis()
                    results.addAll(processEntities(findTableNames(it)))
                    long endTime = System.currentTimeMillis()
                    long stageDuration = endTime - stageStartTime
                    long totalDuration = endTime - startTime
                    Map message = [stage: it.name(), stageDuration: stageDuration, totalDuration: totalDuration]
                    log.info(message.toString())
                    results.add(message)
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime
        results.add([TotalDuration: duration])
        log.info("Completed getAllData: duration=${duration}")
        results.each { log.info(it.toString()) }
        return new ResponseEntity(results, HttpStatus.OK)
    }

    private List<Map> processEntities(List<String> tableNames) {
        List<Future<Map>> futures = []
        tableNames.each { tableName ->
            Future<Map> result = getService(context, tableName).requestData(tableName)
            futures.add(result)
        }

        List<Map> results = futures.collect { it.get() }
        results
    }

    private static List<String> findTableNames(Stages stage) {
        List<String> tableNames = []
        Entities.values().each {
            if (it.stage == stage) {
                tableNames.add(it.toString())
            }
        }
        return tableNames
    }


}
