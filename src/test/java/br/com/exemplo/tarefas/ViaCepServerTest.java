package br.com.exemplo.tarefas;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ViaCepServerTest {
    private HttpServer viaCepStub;
    private HttpServer servidor;
    private ExecutorService executor;
    private HttpClient client;

    @Before
    public void iniciarServidores() throws Exception {
        viaCepStub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        viaCepStub.createContext("/ws", requisicao -> {
            String caminho = requisicao.getRequestURI().getPath();
            int status = caminho.contains("99999999") ? 503 : 200;
            String corpo = caminho.contains("00000000")
                    ? "{\"erro\": true}"
                    : "{\"cep\":\"01001-000\",\"logradouro\":\"Praça da Sé\"}";
            byte[] resposta = corpo.getBytes(StandardCharsets.UTF_8);
            requisicao.sendResponseHeaders(status, resposta.length);
            try (var saida = requisicao.getResponseBody()) {
                saida.write(resposta);
            }
        });
        viaCepStub.start();

        URI baseUri = URI.create("http://127.0.0.1:" + viaCepStub.getAddress().getPort() + "/ws");
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newFixedThreadPool(2);
        servidor.setExecutor(executor);
        servidor.createContext("/api/cep", new ViaCepServer(baseUri, HttpClient.newHttpClient())::consultarCep);
        servidor.start();
        client = HttpClient.newHttpClient();
    }

    @After
    public void pararServidores() {
        servidor.stop(0);
        executor.shutdownNow();
        viaCepStub.stop(0);
    }

    @Test
    public void cepInvalidoRetorna400SemConsultarServico() throws Exception {
        HttpResponse<String> resposta = requisitar("GET", "/api/cep/123");

        assertEquals(400, resposta.statusCode());
        assertTrue(resposta.body().contains("8 dígitos"));
    }

    @Test
    public void metodoDiferenteDeGetRetorna405() throws Exception {
        HttpResponse<String> resposta = requisitar("POST", "/api/cep/01001000");

        assertEquals(405, resposta.statusCode());
        assertEquals("GET", resposta.headers().firstValue("Allow").orElse(""));
    }

    @Test
    public void caminhoDesconhecidoRetorna404() throws Exception {
        HttpResponse<String> resposta = requisitar("GET", "/api/cepfoo");

        assertEquals(404, resposta.statusCode());
    }

    @Test
    public void cepEncontradoRetornaJsonDoViaCep() throws Exception {
        HttpResponse<String> resposta = requisitar("GET", "/api/cep/01001000");

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("\"logradouro\":\"Praça da Sé\""));
    }

    @Test
    public void cepNaoEncontradoRetorna404() throws Exception {
        HttpResponse<String> resposta = requisitar("GET", "/api/cep/00000000");

        assertEquals(404, resposta.statusCode());
        assertTrue(resposta.body().contains("CEP não encontrado"));
    }

    @Test
    public void falhaDoViaCepRetorna502() throws Exception {
        HttpResponse<String> resposta = requisitar("GET", "/api/cep/99999999");

        assertEquals(502, resposta.statusCode());
    }

    @Test
    public void erroInesperadoRetorna500() throws Exception {
        HttpServer servidorComErro = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidorComErro.createContext(
                "/api/cep",
                new ViaCepServer(URI.create("ftp://via-cep-invalido/ws/"), HttpClient.newHttpClient())::consultarCep);
        servidorComErro.start();

        try {
            URI uri = URI.create("http://127.0.0.1:" + servidorComErro.getAddress().getPort()
                    + "/api/cep/01001000");
            HttpRequest requisicao = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> resposta = client.send(requisicao, HttpResponse.BodyHandlers.ofString());

            assertEquals(500, resposta.statusCode());
        } finally {
            servidorComErro.stop(0);
        }
    }

    private HttpResponse<String> requisitar(String metodo, String caminho) throws Exception {
        URI uri = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + caminho);
        HttpRequest requisicao = HttpRequest.newBuilder(uri)
                .method(metodo, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
