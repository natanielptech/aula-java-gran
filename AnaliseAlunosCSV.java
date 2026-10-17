import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AnaliseAlunosCSV {

    public static void main(String[] args) {
        // Caminho do CSV (pode passar por argumento, ex: java AnaliseAlunosCSV alunos.csv)
        String caminhoCsv = args.length > 0 ? args[0] : "alunos.csv";

        analisarArquivo(caminhoCsv);
    }

    public static void analisarArquivo(String caminhoCsv) {
        int quantidade = 0;
        int somaIdades = 0;
        double maiorNota = Double.NEGATIVE_INFINITY;
        double menorNota = Double.POSITIVE_INFINITY;

        String alunoMaiorNota = "";
        String alunoMenorNota = "";

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoCsv))) {
            String linha;

            // Lê e descarta o cabeçalho (nome,idade,nota)
            br.readLine();

            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) {
                    continue;
                }

                String[] partes = linha.split(",");
                if (partes.length != 3) {
                    System.out.println("Linha ignorada (formato inválido): " + linha);
                    continue;
                }

                String nome = partes[0].trim();
                int idade;
                double nota;

                try {
                    idade = Integer.parseInt(partes[1].trim());
                    nota = Double.parseDouble(partes[2].trim());
                } catch (NumberFormatException e) {
                    System.out.println("Linha ignorada (idade/nota inválida): " + linha);
                    continue;
                }

                quantidade++;
                somaIdades += idade;

                if (nota > maiorNota) {
                    maiorNota = nota;
                    alunoMaiorNota = nome;
                }

                if (nota < menorNota) {
                    menorNota = nota;
                    alunoMenorNota = nome;
                }
            }

            if (quantidade == 0) {
                System.out.println("Nenhum aluno válido encontrado no arquivo.");
                return;
            }

            double mediaIdade = (double) somaIdades / quantidade;

            System.out.println("=== Análise de Alunos (CSV) ===");
            System.out.println("Arquivo: " + caminhoCsv);
            System.out.println("Quantidade de alunos: " + quantidade);
            System.out.printf("Média de idade: %.2f%n", mediaIdade);
            System.out.println("Maior nota: " + maiorNota + " (" + alunoMaiorNota + ")");
            System.out.println("Menor nota: " + menorNota + " (" + alunoMenorNota + ")");

        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo CSV: " + e.getMessage());
        }
    }
}
