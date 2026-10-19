# CRUD de tarefas com Java e H2

Aplicação de console para criar, listar, editar e excluir tarefas. Os dados ficam
em um banco H2 em memória e são apagados quando a aplicação é encerrada.

O projeto também inclui um serviço HTTP de exemplo que consulta endereços pelo
ViaCEP.

## Requisitos

- Java 17 ou superior
- Maven

## Executar

Na raiz do projeto:

```sh
mvn clean compile exec:java
```

Informe a opção desejada no menu e siga as instruções exibidas no terminal.

## Serviço ViaCEP

Inicie o servidor na raiz do projeto:

```sh
mvn compile exec:java -Dexec.mainClass=br.com.exemplo.tarefas.ViaCepServer
```

O endpoint `GET http://localhost:8080/api/cep/{cep}` recebe um CEP com 8
dígitos e retorna o JSON do ViaCEP. Por exemplo:

```sh
curl http://localhost:8080/api/cep/01001000
```

É possível informar outra porta como argumento do programa:

```sh
mvn compile exec:java -Dexec.mainClass=br.com.exemplo.tarefas.ViaCepServer -Dexec.args=8081
```
