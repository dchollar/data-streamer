package com.streamer.extractor.service

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import java.util.stream.Stream

@Slf4j
@Service
class SourceService {

    static String END_OF_LINE = '\n'
    static ObjectMapper MAPPER = new ObjectMapper()

    @PersistenceContext
    EntityManager entityManager
    @Autowired
    KeepAliveService keepAliveService

    /**
     * This will read the data from the database as a java.utl.stream and write it to the given stream Writer.
     * The stream writer is from HttpServletRequest. For testing this can be a file writer. This data is
     * written to the stream in a non-standard html content type but this doesn't matter because it is
     * not read in any standard way. The database object is converted to json and then base64 encoded. This
     * is written directly to the stream. An end-of-line character is then written as a standard line terminator
     * for a standard stream reader to understand later.
     *
     * @param writer
     * @return the number of records written to the stream
     */
    @Transactional(readOnly = true)
    int streamDataOut(Writer writer, JpaRepository repository, Class domainClass, Integer lowerLimit, Integer upperLimit) {
        long startTime = System.currentTimeMillis()
        String tableName = domainClass.simpleName
        keepAliveService.keepAlive(writer, tableName)

        log.info("Starting extraction of data for ${tableName}")
        Map counts = [sentCount: 0, nullCount: 0]
        Stream<Object> dbStream
        try {
            dbStream = getDataStream(repository, lowerLimit, upperLimit)
            processStream(tableName, dbStream, writer, counts)
        } catch (Exception e) {
            log.error("ERROR reading data for ${tableName}", e)
        }

        if (dbStream) {
            KeepAliveService.stop(tableName)
            dbStream.close()
        }

        long duration = System.currentTimeMillis() - startTime
        log.info("Completed extraction of data for ${tableName}, duration = ${duration}, sentCount = ${counts.sentCount}, nullCount = ${counts.nullCount}")

        return counts.sentCount
    }

    static private Stream<Object> getDataStream(JpaRepository repository, Integer lowerLimit, Integer upperLimit) {
        Stream<Object> dbStream
        if (lowerLimit && upperLimit) {
            dbStream = repository.findByRangeAndStream(lowerLimit, upperLimit)
        } else {
            dbStream = repository.findAllAndStream()
        }
        dbStream
    }

    private void processStream(String tableName, Stream<Object> dbStream, Writer writer, Map counts) {
        try {
            KeepAliveService.stop(tableName)
            dbStream.each { Object entity ->
                if (entity) {
                    writeEntity(writer, entity)
                    counts.sentCount++
                    if ((counts.sentCount % 100000) == 0) {
                        log.info("Domain Record for ${tableName} sent ${counts.sentCount}")
                    }
                } else {
                    counts.nullCount++
                }
            }
        } catch (Exception e) {
            log.error("EXCEPTION extracting data for ${tableName}", e)
        }
    }

    private void writeEntity(Writer writer, def record) {
        writer.write(MAPPER.writeValueAsBytes(record).encodeBase64())
        writer.write(END_OF_LINE)
        writer.flush()
        entityManager.detach(record)
    }

}
