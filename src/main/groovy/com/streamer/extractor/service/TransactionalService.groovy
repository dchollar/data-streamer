package com.streamer.extractor.service

import groovy.util.logging.Slf4j
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

import javax.transaction.Transactional

@Slf4j
@Service
class TransactionalService {

    @Transactional
    void saveAll(JpaRepository repository, Set entities) {
        repository.saveAll(entities)
    }

}