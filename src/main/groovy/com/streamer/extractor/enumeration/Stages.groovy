package com.streamer.extractor.enumeration

/**
 * A stage is just a grouping of one or more tables. All tables in a stage are loaded together. The stage must complete
 * before the next group of tables or stage is started. The order below is the order in which the stages are executed.
 */
enum Stages {
    FIRST_STAGE,
    SECOND_STAGE,
}