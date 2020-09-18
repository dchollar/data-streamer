package com.streamer.extractor.service.entity

import com.streamer.extractor.entity.Address
import com.streamer.extractor.repository.AddressRepository
import com.streamer.extractor.service.BaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AddressService extends BaseService {
    @Autowired
    AddressRepository repository

    Class domainClass = Address.class

}
