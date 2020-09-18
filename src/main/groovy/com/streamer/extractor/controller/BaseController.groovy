package com.streamer.extractor.controller

import com.streamer.extractor.enumeration.ServiceType
import com.streamer.extractor.service.BaseService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RestController

import javax.security.sasl.AuthenticationException
import javax.xml.bind.ValidationException

@Slf4j
@RestController
abstract class BaseController {

    @Value('${service.type}')
    ServiceType serviceType

    @Autowired
    ApplicationContext context

    static BaseService getService(ApplicationContext context, String tableName) {
        String className = "com.streamer.extractor.service.entity.${tableName}Service"
        Class clazz = Class.forName(className)
        return (BaseService) context.getBean(clazz)
    }

    void validate(ServiceType validServiceType, String action) {
        if (serviceType != validServiceType) {
            String message = "Trying to ${action} and this is not ${validServiceType}"
            log.error(message)
            throw new ValidationException(message)
        }
    }

}
