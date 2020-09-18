package com.streamer.extractor.service

import com.streamer.extractor.enumeration.Constraints
import com.streamer.extractor.enumeration.Entities
import com.streamer.extractor.enumeration.Stages
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import org.springframework.core.NestedExceptionUtils
import org.springframework.stereotype.Service

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@Slf4j
@Service
class NativeSqlService {

    @PersistenceContext
    EntityManager entityManager

    List truncateTables(String tableName, Stages stage) {
        List<String> errors = []
        Entities.values().each { entity ->
            if ((!tableName && !stage) || (tableName && entity.name() == tableName) || (stage && entity.stage == stage)) {
                truncateTable(entity, errors)
            }
        }
        return errors
    }

    List dropConstraints(String tableName, Stages stage) {
        List<String> errors = []
        Constraints.values().each { constraint ->
            if ((!tableName && !stage) || (tableName && constraint.entity.name() == tableName) || (stage && constraint.entity.stage == stage)) {
                dropConstraint(constraint, errors)
            }
        }
        return errors
    }

    List createConstraints(String tableName, Stages stage) {
        List<String> errors = []
        Constraints.values().each { constraint ->
            if ((!tableName && !stage) || (tableName && constraint.entity.name() == tableName) || (stage && constraint.entity.stage == stage)) {
                createConstraint(constraint, errors)
            }
        }
        return errors
    }

    List resetSequences(String tableName, Stages stage) {
        List<String> errors = []
        Entities.values().each { entity ->
            if ((!tableName && !stage) || (tableName && entity.name() == tableName) || (stage && entity.stage == stage)) {
                resetSequence(entity, errors)
            }
        }
        return errors
    }

    private void dropConstraint(Constraints constraint, List<String> errors) {
        String sqlCommand = "ALTER TABLE public.${constraint.entity.tableName} DROP CONSTRAINT ${constraint.name()};"
        executeSqlCommand(sqlCommand, errors)
    }

    private void createConstraint(Constraints constraint, List<String> errors) {
        String sqlCommand = "ALTER TABLE public.${constraint.entity.tableName} ADD CONSTRAINT ${constraint.name()} FOREIGN KEY (${constraint.attributeName}) REFERENCES public.${constraint.reference.tableName} (${constraint.referenceAttributeName}) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE ${constraint.deleteAction};"
        executeSqlCommand(sqlCommand, errors)
    }

    private void resetSequence(Entities entity, List<String> errors) {
        if (entity.sequenceName) {
            String attributeName = 'id'
            String sqlCommand = "SELECT setval('${entity.sequenceName}', coalesce((select max(${attributeName})+1 from ${entity.tableName}), 1), false)"
            executeSqlCommand(sqlCommand, errors)
        }
    }

    private void truncateTable(Entities entity, List<String> errors) {
        log.info("Truncating ${entity.name()}")
        String sqlCommand = "truncate table ${entity.tableName} cascade;"
        executeSqlCommand(sqlCommand, errors)
    }

    private void executeSqlCommand(String sqlCommand, List<String> errors) {
        SessionFactory factory = entityManager.unwrap(Session.class).sessionFactory
        Transaction tx
        Session session = factory.openSession()
        try {
            tx = session.beginTransaction()
            session.createSQLQuery(sqlCommand).executeUpdate()
            tx.commit()
        } catch (Exception e) {
            if (tx) {
                tx.rollback()
            }
            String rootCauseMessage = NestedExceptionUtils.getRootCause(e).message
            if (!rootCauseMessage.contains('A result was returned when none was expected')) {
                errors.add(rootCauseMessage)
            }
        }
        session.close()
    }
}
