package com.laptophub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Không active ở profile "test" vì {@code LaptopHubApplicationTests} loại trừ
 * Hibernate/DataSource để chạy không cần MySQL sống — không có EntityManagerFactory
 * nào để JpaMetamodelMappingContext build, nên bật auditing sẽ làm context load thất bại.
 */
@Configuration
@Profile("!test")
@EnableJpaAuditing
public class JpaAuditingConfig {
}
