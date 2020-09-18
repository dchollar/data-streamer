package com.streamer.extractor.service.entity

import com.streamer.extractor.entity.Customer
import com.streamer.extractor.repository.CustomerRepository
import com.streamer.extractor.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CustomerService extends BaseService{

    @Autowired
    CustomerRepository repository

    Class domainClass = Customer.class
}
