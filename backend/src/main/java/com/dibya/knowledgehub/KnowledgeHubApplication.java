package com.dibya.knowledgehub;

import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication(exclude = QdrantVectorStoreAutoConfiguration.class)
@EnableAsync
@EnableMethodSecurity
public class KnowledgeHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowledgeHubApplication.class, args);
	}

}
