package com.sergio.planix.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de integração: sobe um Postgres de verdade no Docker, aplica as migrations
 * do Flyway e valida os mapeamentos JPA (ddl-auto=validate) contra o schema real.
 *
 * <p>{@code @ServiceConnection} liga o container ao datasource — não precisa configurar
 * url/usuário/senha.
 *
 * <p>Container <b>singleton</b>: subimos no bloco estático em vez de usar
 * {@code @Testcontainers}/{@code @Container}. A extensão do JUnit derruba o container ao fim de
 * cada classe de teste, mas o contexto do Spring fica em cache e é reaproveitado pelas classes
 * seguintes — que então tentariam conectar numa porta morta. Subindo uma vez por JVM, o container
 * vive tanto quanto o contexto.
 */
@SpringBootTest
@TestPropertySource(properties = "planix.upload-dir=target/test-uploads")
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static {
        postgres.start();
    }
}
