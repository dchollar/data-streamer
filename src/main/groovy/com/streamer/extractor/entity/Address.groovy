package com.streamer.extractor.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.EqualsAndHashCode
import org.springframework.data.domain.Persistable

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Transient
import java.sql.Timestamp

@EqualsAndHashCode(includes = ['id'])
@Entity
@Table(name = 'address', schema = 'public')
class Address  implements Serializable, Persistable<Integer>  {

    @Id
    @Column(name = 'id', nullable = false)
    Integer id

    @Basic
    @Column(name = 'customer_id', nullable = false)
    Integer customerId

    @Basic
    @Column(name = 'address', nullable = false, length = 100)
    String address

    @Basic
    @Column(name = 'city', nullable = false, length = 100)
    String city

    @Basic
    @Column(name = 'state', nullable = false, length = 2)
    String state

    @Basic
    @Column(name = 'zip_code', nullable = false, length = 5)
    String zipCode

    @Basic
    @Column(name = 'created_at', nullable = false)
    Timestamp createdAt

    @Basic
    @Column(name = 'updated_at', nullable = false)
    Timestamp updatedAt

    @Override
    @Transient
    @JsonIgnore
    boolean isNew() {
        return true
    }
}
