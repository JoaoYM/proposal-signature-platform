package com.powermobile.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

public class SignatureSteps {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private String email;
    private String propostaId;
    private String contratoId;
    private int ultimoStatus;

    @Before
    public void preparar() { email = "bdd." + System.nanoTime() + "@example.com"; }

    @Dado("que uma nova proposta foi criada")
    public void criarProposta() throws Exception {
        String body = "{\"clienteNome\":\"Cliente BDD\",\"clienteEmail\":\"" + email
                + "\",\"itens\":[{\"nome\":\"Plano BDD\",\"quantidade\":1,\"precoUnitario\":100}]}";
        JsonNode proposta = json.readTree(enviar("POST", "http://localhost:8081/api/v1/propostas", body).body());
        propostaId = proposta.get("id").asText();
        for (int tentativa = 0; tentativa < 30; tentativa++) {
            HttpResponse<String> response = enviar("GET", "http://localhost:8082/api/v1/contratos?propostaId=" + propostaId, null);
            if (response.statusCode() == 200) { contratoId = json.readTree(response.body()).get("id").asText(); return; }
            Thread.sleep(1000);
        }
        throw new AssertionError("Contrato não criado para a proposta " + propostaId);
    }

    @Quando("o cliente aceita o contrato") public void clienteAceita() throws Exception { assinar(email, true); }
    @E("o diretor aceita o contrato") public void diretorAceita() throws Exception { assinar("diretor@powermobile.com", true); }
    @Quando("o diretor tenta assinar antes do cliente") public void diretorForaDeOrdem() throws Exception { assinar("diretor@powermobile.com", true); }
    @Quando("o cliente recusa o contrato") public void clienteRecusa() throws Exception { assinar(email, false); }

    @Entao("a operação deve retornar HTTP {int}") public void validarHttp(int esperado) { assertThat(ultimoStatus).isEqualTo(esperado); }

    @Entao("o contrato deve estar {string}")
    public void validarContrato(String esperado) throws Exception {
        aguardarStatus("http://localhost:8082/api/v1/contratos/" + contratoId, esperado);
    }

    @E("a proposta deve estar {string}")
    public void validarProposta(String esperado) throws Exception {
        aguardarStatus("http://localhost:8081/api/v1/propostas/" + propostaId, esperado);
    }

    private void assinar(String participante, boolean aceitou) throws Exception {
        String body = "{\"email\":\"" + participante + "\",\"aceitou\":" + aceitou + "}";
        ultimoStatus = enviar("POST", "http://localhost:8082/api/v1/contratos/" + contratoId + "/assinaturas", body).statusCode();
    }

    private void aguardarStatus(String url, String esperado) throws Exception {
        String atual = null;
        for (int tentativa = 0; tentativa < 30; tentativa++) {
            JsonNode response = json.readTree(enviar("GET", url, null).body());
            atual = response.get("status").asText();
            if (esperado.equals(atual)) return;
            Thread.sleep(1000);
        }
        assertThat(atual).isEqualTo(esperado);
    }

    private HttpResponse<String> enviar(String method, String url, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if (body == null) request.GET(); else request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
