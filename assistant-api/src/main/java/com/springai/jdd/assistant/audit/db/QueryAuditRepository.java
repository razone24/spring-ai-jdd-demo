package com.springai.jdd.assistant.audit.db;

import org.springframework.data.jpa.repository.JpaRepository;

interface QueryAuditRepository extends JpaRepository<QueryAuditEntity, String> {
}
