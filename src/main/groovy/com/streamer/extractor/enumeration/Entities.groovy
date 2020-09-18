package com.streamer.extractor.enumeration

/**
 * The order of how these are listed determines the order in which the tables are loaded.
 */
enum Entities {

    Address('address', 'address_id_seq', Stages.FIRST_STAGE),
    Customer('customer', 'customer_id_seq', Stages.FIRST_STAGE)

    final String tableName
    final String sequenceName
    final Stages stage

    Entities(String tableName, String sequenceName, Stages stage) {
        this.sequenceName = sequenceName
        this.tableName = tableName
        this.stage = stage
    }
}