package com.streamer.extractor.service

import com.github.javafaker.Faker
import com.streamer.extractor.entity.Address
import com.streamer.extractor.entity.Customer
import com.streamer.extractor.repository.AddressRepository
import com.streamer.extractor.repository.CustomerRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.sql.Timestamp

@Slf4j
@Service
class DataGeneratorService {

    @Autowired
    CustomerRepository customerRepository
    @Autowired
    AddressRepository addressRepository

    @Transactional
    void generateTestData() {
        Faker faker = new Faker()
        List customers = []
        List addresses = []
        for (int id = 0; id <= 10000; id++) {
            customers.add(createCustomerRecord(faker, id))
            addresses.add(createAddress(faker, id))
        }
        customerRepository.saveAll(customers)
        addressRepository.saveAll(addresses)
    }

    private static Customer createCustomerRecord(Faker faker, int id) {
        Customer customer = new Customer(id: id)
        customer.firstName = faker.name().firstName()
        customer.lastName = faker.name().lastName()
        customer.email = faker.internet().emailAddress()
        customer.phoneNumber = faker.phoneNumber().cellPhone()
                .replace('.', '')
                .replace('(', '')
                .replace(')', '')
                .replace(' ', '')
                .replace('-', '')[0..9]
        customer.createdAt = new Timestamp(System.currentTimeMillis())
        customer.updatedAt = new Timestamp(System.currentTimeMillis())
        return customer
    }

    private static createAddress(Faker faker, int customerId) {
        Address address = new Address()
        address.id = customerId
        address.customerId = customerId
        address.address = faker.address().streetAddress()
        address.city = faker.address().city()
        address.state = faker.address().stateAbbr()
        address.zipCode = faker.address().zipCode()[0..4]
        address.createdAt = new Timestamp(System.currentTimeMillis())
        address.updatedAt = new Timestamp(System.currentTimeMillis())
        return address
    }
}
