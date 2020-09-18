package com.streamer.extractor.enumeration

enum Constraints {
    address_customer_fk(Entities.Address, 'customer_id', Entities.Customer)

    final Entities entity
    final String attributeName
    final Entities reference
    final String referenceAttributeName
    final String deleteAction

    Constraints(Entities entity, String attributeName, Entities reference, String referenceAttributeName = 'id', String deleteAction = 'NO ACTION') {
        this.entity = entity
        this.attributeName = attributeName
        this.reference = reference
        this.referenceAttributeName = referenceAttributeName
        this.deleteAction = deleteAction
    }
}