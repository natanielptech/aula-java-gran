package br.com.exemplo.tarefas;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;

public class ViaCepServer {
    private static final String ENDPOINT = "/api/cep";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) throws IOException {
        int porta = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.createContext(ENDPOINT, ViaCepServer::consultarCep);
        servidor.setExecutor(Executors.newFixedThreadPool(10));
        servidor.start();
        System.out.println("Serviço ViaCEP disponível em http://localhost:" + porta + ENDPOINT + "/{cep}");
    }

    private static void consultarCep(HttpExchange requisicao) throws IOException {
        String caminho = requisicao.getRequestURI().getPath();
        String prefixo = ENDPOINT + "/";

        if (!"GET".equals(requisicao.getRequestMethod())) {
            requisicao.getResponseHeaders().set("Allow", "GET");
            responder(requisicao, 405, "{\"erro\":\"Método não permitido. Use GET.\"}");
            return;
        }

        if (!caminho.startsWith(prefixo) || caminho.length() != prefixo.length() + 8) {
            responder(requisicao, 400, "{\"erro\":\"Informe um CEP com 8 dígitos.\"}");
            return;
        }

        String cep = caminho.substring(prefixo.length());
        if (!cep.matches("\\d{8}")) {
            responder(requisicao, 400, "{\"erro\":\"Informe um CEP com 8 dígitos.\"}");
            return;
        }

        URI uri = URI.create("https://viacep.com.br/ws/" + cep + "/json/");
        HttpRequest consulta = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> resposta = CLIENT.send(consulta, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() >= 200 && resposta.statusCode() < 300) {
                responder(requisicao, 200, resposta.body());
            } else {
                responder(requisicao, 502, "{\"erro\":\"O serviço ViaCEP retornou um erro.\"}");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responder(requisicao, 502, "{\"erro\":\"Não foi possível consultar o ViaCEP.\"}");
        } catch (IOException e) {
            responder(requisicao, 502, "{\"erro\":\"Não foi possível consultar o ViaCEP.\"}");
        }
    }

    private static void responder(HttpExchange requisicao, int status, String corpo) throws IOException {
        byte[] resposta = corpo.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        requisicao.sendResponseHeaders(status, resposta.length);
        try (var saida = requisicao.getResponseBody()) {
            saida.write(resposta);
        }
    }
}
