package com.streamer.extractor.repository

import com.streamer.extractor.entity.Address
import com.streamer.extractor.entity.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository

import javax.persistence.QueryHint
import java.util.stream.Stream

import static org.hibernate.jpa.QueryHints.*

@Repository
interface AddressRepository extends JpaRepository<Address, Integer> {

    @QueryHints([
            @QueryHint(name = HINT_FETCH_SIZE, value = '1000'),
            @QueryHint(name = HINT_CACHEABLE, value = 'false'),
            @QueryHint(name = HINT_READONLY, value = 'true')
    ])
    @Query('select u from Address u')
    Stream<Address> findAllAndStream()

    @QueryHints([
            @QueryHint(name = HINT_FETCH_SIZE, value = '1000'),
            @QueryHint(name = HINT_CACHEABLE, value = 'false'),
            @QueryHint(name = HINT_READONLY, value = 'true')
    ])
    @Query('select u from Address u where u.id >= ?1 and u.id <= ?2')
    Stream<Address> findByRangeAndStream(int lowerLimit, int upperLimit)

}
