package com.powermobile.crm.integration;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.repository.PropostaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(properties={"spring.rabbitmq.listener.simple.auto-startup=false","spring.cache.type=none"})
@Testcontainers
class PersistenceIT {
 @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0");
 @Container static final RabbitMQContainer RABBIT=new RabbitMQContainer("rabbitmq:3.13-management");
 @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);r.add("spring.rabbitmq.host",RABBIT::getHost);r.add("spring.rabbitmq.port",RABBIT::getAmqpPort);}
 @Autowired PropostaRepository propostas; @Autowired JdbcTemplate jdbc; @Autowired RabbitTemplate rabbit;
 @Test void flywayEJpa(){var p=propostas.saveAndFlush(Proposta.builder().clienteNome("IT").clienteEmail("it@example.com").status(PropostaStatus.CRIADA).build());assertThat(propostas.findById(p.getId())).isPresent();assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history",Integer.class)).isPositive();}
 @Test void deadLetterQueuePreservaMensagem(){rabbit.convertAndSend(RabbitMQConfig.DLX_NAME,RabbitMQConfig.DLQ_CONTRATO_STATUS,"evento-invalido");assertThat(rabbit.receiveAndConvert(RabbitMQConfig.DLQ_CONTRATO_STATUS,5000)).isEqualTo("evento-invalido");}
}
