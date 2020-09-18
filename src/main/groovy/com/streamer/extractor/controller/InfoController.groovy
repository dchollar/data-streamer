package com.streamer.extractor.controller


import com.streamer.extractor.enumeration.ServiceType
import com.streamer.extractor.service.entity.CustomerService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class InfoController {

    @Value('${service.source.http}')
    String http
    @Value('${service.source.host}')
    String host
    @Value('${service.source.port}')
    String port
    @Value('${service.type}')
    ServiceType serviceType
    @Value('${service.version}')
    String version
    @Autowired
    CustomerService service

    /**
     * no validation or security required for this. used to ensure setup correctly.
     *
     * @return
     */
    @GetMapping('/info')
    ResponseEntity getInfo() {
        long count = service.getTotalCount()
        Map info = [version: version, serviceType: serviceType, count: count]
        if (serviceType == ServiceType.DESTINATION) {
            info << [source_http: http, source_host: host, source_port: port]
        }
        return new ResponseEntity(info, HttpStatus.OK)
    }

}
