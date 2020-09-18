package com.streamer.extractor.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.streamer.extractor.enumeration.ServiceType
import groovy.util.logging.Slf4j
import org.apache.http.HttpEntity
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service

import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.Future

@Slf4j
@Service
class DestinationService {

    static ObjectMapper MAPPER = new ObjectMapper()

    @Value('${service.source.http}')
    String http
    @Value('${service.source.host}')
    String host
    @Value('${service.source.port}')
    String port
    @Value('${service.use-httpclient}')
    Boolean useHttpClient

    @Autowired
    TransactionalService transactionalService

    Future<Map> countAll(String tableName, JpaRepository repository) {
        Map<String, Long> countInfo = [:]

        // get destination count info
        Long destinationCount = repository.count()
        countInfo.put(ServiceType.DESTINATION.toString(), destinationCount)

        // get source count info
        long sourceCount = countSource(tableName)
        countInfo.put(ServiceType.SOURCE.toString(), sourceCount)

        // determine the difference
        countInfo.put('DIFFERENCE', sourceCount - destinationCount)

        Map<String, Map<String, Long>> response = [:]
        response.put(tableName, countInfo)

        new AsyncResult<Map>(response)
    }

    private Long countSource(String tableName) {
        try {
            HttpGet httpGet = createHttpClientRequest(tableName, 'count')
            CloseableHttpClient client = createHttpClient()
            HttpEntity entity = client.execute(httpGet).entity
            Long sourceCount = EntityUtils.toString(entity) as Long
            sourceCount
        } catch (Exception e) {
            log.error("EXCEPTION getting count for ${tableName}", e)
            return -1
        }
    }

    @Async('tableExecutor')
    Future<Map> requestDataByRange(String tableName, JpaRepository repository, Class domainClass, Integer lowerLimit, Integer upperLimit) {
        return internalRequestData(tableName, repository, domainClass, lowerLimit, upperLimit)
    }

    Future<Map> requestData(String tableName, JpaRepository repository, Class domainClass) {
        return internalRequestData(tableName, repository, domainClass, null, null)
    }

    /**
     * This calls an http endpoint as a get to initiate the process. I then grab the response stream directly
     * and convert that into a stream reader. Because of this the normal http response headers of content type
     * are used. The stream is then read until it is closed on the other end.
     *
     * @param tableName
     * @return
     */
    Future<Map> internalRequestData(String tableName, JpaRepository repository, Class domainClass, Integer lowerLimit, Integer upperLimit) {
        log.info("Starting extraction of data for ${tableName}")
        long startTime = System.currentTimeMillis()
        final int recordCount
        if (useHttpClient) {
            recordCount = requestDataUsingHttpClient(tableName, lowerLimit, upperLimit, repository, domainClass)
        } else {
            recordCount = requestDataUsingURLConnection(tableName, lowerLimit, upperLimit, repository, domainClass)
        }
        long duration = System.currentTimeMillis() - startTime
        log.info("Completed extraction of data for ${tableName} Duration=${duration} RecordCount=${recordCount} lowerLimit=${lowerLimit} upperLimit=${upperLimit}")
        return new AsyncResult<Map>([tableName: tableName, duration: duration, recordCount: recordCount])
    }

    private int requestDataUsingURLConnection(String tableName, Integer lowerLimit, Integer upperLimit, JpaRepository repository, Class domainClass) {
        // create connection
        URL url = new URL(createUrl(tableName, 'extract', lowerLimit, upperLimit))

        final HttpURLConnection con
        if (http.toLowerCase() == 'http') {
            con = (HttpURLConnection) url.openConnection()
        } else {
            con = (HttpsURLConnection) url.openConnection()
        }
        con.setRequestMethod('GET')

        // execute connection
        int recordCount = 0
        try {
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream stream = con.getInputStream()
                recordCount = this.streamDataIn(stream, repository, domainClass)
                stream.close()
            }
        } catch (Exception e) {
            log.error("EXCEPTION requesting data for ${tableName}", e)
        }

        return recordCount
    }

    private int requestDataUsingHttpClient(String tableName, Integer lowerLimit, Integer upperLimit, JpaRepository repository, Class domainClass) {
        HttpGet httpGet = createHttpClientRequest(tableName, 'extract', lowerLimit, upperLimit)
        CloseableHttpClient client = createHttpClient()
        int recordCount = processHttpClientResponse(client, httpGet, repository, domainClass, tableName)
        return recordCount
    }

    private int processHttpClientResponse(CloseableHttpClient client, HttpGet httpGet, JpaRepository repository, Class domainClass, String tableName) {
        int totalRecordCount = 0
        CloseableHttpResponse response = null
        try {
            response = client.execute(httpGet)
            log.debug("headers are: ${response.getAllHeaders()}")
            final HttpEntity entity = response.getEntity()
            if (entity && entity.isStreaming()) {
                totalRecordCount = this.streamDataIn(entity.getContent(), repository, domainClass)
            }
        } catch (Exception e) {
            log.error("EXCEPTION requesting data for ${tableName}", e)
        }

        if (response != null) {
            try {
                response.close()
            } catch (Exception e) {
                log.error("ERROR closing response for ${tableName}", e)
            }
        }

        try {
            client.close()
        } catch (Exception e) {
            log.error("ERROR closing client for ${tableName}", e)
        }
        totalRecordCount
    }


    private static CloseableHttpClient createHttpClient() {
        int timeoutInSeconds = 1000
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeoutInSeconds * 1000)
                .setConnectionRequestTimeout(timeoutInSeconds * 1000)
                .setSocketTimeout(timeoutInSeconds * 1000).build()
        CloseableHttpClient client = HttpClientBuilder.create()
                .disableAutomaticRetries()
                .setDefaultRequestConfig(config)
                .build()
        client
    }

    private HttpGet createHttpClientRequest(String tableName, String action, Integer lowerLimit = null, Integer upperLimit = null) {
        String url = createUrl(tableName, action, lowerLimit, upperLimit)
        HttpGet httpGet = new HttpGet(url)
        httpGet.addHeader('TE', 'chunked')
        httpGet.addHeader('X-Accel-Buffering', 'no')
        httpGet
    }

    /**
     * The data is read from the stream and converted back into a database entity. Every thousand records
     * are sent to be committed to the database.
     *
     * @param reader
     * @return
     */
    private int streamDataIn(InputStream reader, JpaRepository repository, Class domainClass) {
        int lineCount = 0
        Set entities = [] as Set
        reader.eachLine { String record, int count ->
            if (record.startsWith(KeepAliveService.KEEP_ALIVE_MESSAGE)) {
                log.info("Keep Alive Record for ${domainClass.simpleName} read ${count}")
            } else {
                saveRecord(record, domainClass, entities, repository)
                lineCount++
                if ((lineCount % 100000) == 0) {
                    log.info("Domain Record for ${domainClass.simpleName} saved ${lineCount}")
                }
            }
        }

        if (entities.size() > 0) {
            transactionalService.saveAll(repository, entities)
            entities.clear()
        }
        return lineCount
    }

    private void saveRecord(String record, Class domainClass, Set entities, JpaRepository repository) {
        String json = new String(record.decodeBase64())
        def domainObject = MAPPER.readValue(json, domainClass)
        entities.add(domainObject)

        if (entities.size() == 1000) {
            transactionalService.saveAll(repository, entities)
            entities.clear()
        }
    }

    private String createUrl(String tableName, String action, Integer lowerLimit = null, Integer upperLimit = null) {
        String url
        if (!port || port == '80') {
            url = "${http}://${host}/extractor/${action}?tableName=${tableName}"
        } else {
            url = "${http}://${host}:${port}/extractor/${action}?tableName=${tableName}"
        }
        if (lowerLimit && upperLimit) {
            url = "${url}&lowerLimit=${lowerLimit}&upperLimit=${upperLimit}"
        }
        log.debug("The url is ${url}")
        url
    }

}
