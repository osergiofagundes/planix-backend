package com.sergio.planix.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@TestPropertySource(properties = {
        "planix.upload-dir=target/test-uploads",
        // Nenhuma tarefa de fundo na suíte: o banco é compartilhado entre os ITs, e uma
        // varredura concorrente deixaria as asserções à mercê do relógio. Quem testa relay e
        // scanner chama os métodos na mão.
        "planix.scheduling.enabled=false",
        "planix.outbox.relay.enabled=false",
        "planix.due-scan.enabled=false"
})
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    static {
        postgres.start();
    }
}
