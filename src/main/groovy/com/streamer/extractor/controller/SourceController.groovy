package com.streamer.extractor.controller

import com.streamer.extractor.enumeration.ServiceType
import com.streamer.extractor.service.DataGeneratorService
import com.streamer.extractor.service.SourceService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
class SourceController extends BaseController {

    @Autowired
    DataGeneratorService dataGeneratorService

    @GetMapping('/extract')
    ResponseEntity streamAll(HttpServletResponse response,
                             @RequestParam(name = 'tableName', required = true) String tableName,
                             @RequestParam(name = 'lowerLimit', required = false) Integer lowerLimit,
                             @RequestParam(name = 'upperLimit', required = false) Integer upperLimit) {
        validate(ServiceType.SOURCE, 'retrieve data')
        response.addHeader('Cache-Control', 'no-cache')
        response.addHeader('X-Accel-Buffering', 'no')
        response.setCharacterEncoding('UTF-8')
        getService(context, tableName).streamDataOut(response.getWriter(), lowerLimit, upperLimit)
        return new ResponseEntity(HttpStatus.OK)
    }

    @GetMapping('/count')
    ResponseEntity count(@RequestParam(name = 'tableName', required = true) String tableName) {
        validate(ServiceType.SOURCE, 'count data')
        Long count = getService(context, tableName).totalCount
        return new ResponseEntity(count, HttpStatus.OK)
    }

    @GetMapping('/generateData')
    ResponseEntity generateData() {
        validate(ServiceType.SOURCE, 'generate data')
        dataGeneratorService.generateTestData()
        return new ResponseEntity('Done', HttpStatus.OK)
    }

}
