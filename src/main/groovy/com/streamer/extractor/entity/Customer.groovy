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
@Table(name = 'customer', schema = 'public')
class Customer implements Serializable, Persistable<Integer>  {
    @Id
    @Column(name = 'id', nullable = false)
    Integer id

    @Basic
    @Column(name = 'first_name', nullable = false, length = 50)
    String firstName

    @Basic
    @Column(name = 'last_name', nullable = false, length = 50)
    String lastName

    @Basic
    @Column(name = 'email', nullable = false, length = 200)
    String email

    @Basic
    @Column(name = 'phone_number', nullable = false, length = 10)
    String phoneNumber

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
