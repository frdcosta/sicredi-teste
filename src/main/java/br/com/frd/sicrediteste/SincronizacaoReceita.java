/*
Cenário de Negócio:
Todo dia útil por volta das 6 horas da manhã um colaborador da retaguarda do Sicredi recebe e organiza as informações de contas para enviar ao Banco Central. Todas agencias e cooperativas enviam arquivos Excel à Retaguarda. Hoje o Sicredi já possiu mais de 4 milhões de contas ativas.
Esse usuário da retaguarda exporta manualmente os dados em um arquivo CSV para ser enviada para a Receita Federal, antes as 10:00 da manhã na abertura das agências.

Requisito:
Usar o "serviço da receita" (fake) para processamento automático do arquivo.

Funcionalidade:
0. Criar uma aplicação SprintBoot standalone. Exemplo: java -jar SincronizacaoReceita <input-file>
1. Processa um arquivo CSV de entrada com o formato abaixo.
2. Envia a atualização para a Receita através do serviço (SIMULADO pela classe ReceitaService).
3. Retorna um arquivo com o resultado do envio da atualização da Receita. Mesmo formato adicionando o resultado em uma nova coluna.


Formato CSV:
agencia;conta;saldo;status
0101;12225-6;100,00;A
0101;12226-8;3200,50;A
3202;40011-1;-35,12;I
3202;54001-2;0,00;P
3202;00321-2;34500,00;B
...

*/
package br.com.frd.sicrediteste;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
public class SincronizacaoReceita {

    private static final char CSV_LIMITER = ';';

    public static void main(String[] args) {
        SpringApplication.run(SincronizacaoReceita.class, args);
        final var fileUserDir = System.getProperty("user.dir");
        var filePath = fileUserDir + "/" + args[0];
        var data = readRecords(filePath);
        sendDataToReceita(data);
        printData(data);
    }

    private static List<DadosConta> readRecords(String file) {
        var csvFormatReader = CSVFormat.Builder.create()
                .setHeader()
                .setDelimiter(CSV_LIMITER)
                .build();
        try (var reader = Files.newBufferedReader(Paths.get(file));
             var parser = csvFormatReader.parse(reader)) {

            if(!parser.getHeaderNames().containsAll(List.of("agencia", "conta", "saldo", "status"))
            && parser.getHeaderNames().size() != 4){
                log.error("Cabecalho invalido!");
                throw new RuntimeException();
            }

            return parser.getRecords().stream()
                    .map(record -> {
                        try {
                            return DadosConta.of(record);
                        } catch (ParseException e) {
                            log.error("Saldo da conta " + record.get("conta") + " invalido!");
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new RuntimeException("Arquivo nao encontrado!");
        }
    }

    private static void sendDataToReceita(List<DadosConta> data) {
        var receitaService = new ReceitaService();
        data.forEach(conta ->{
            boolean atualizado = false;
            try {
                atualizado = receitaService.atualizarConta(conta.getAgencia(), conta.getConta().replace("-", ""), conta.getSaldo(), conta.getStatus());
            } catch (InterruptedException e) {
                log.error("Erro no servico da Receita!");
            }
            conta.setAtualizado(atualizado ? "atualizado" : "nao atualizado");
        });
    }

    private static void printData(List<DadosConta> data) {
        var csvFormatWriter = CSVFormat.Builder.create()
                .setHeader("agencia", "conta", "saldo", "status", "resultado")
                .setDelimiter(CSV_LIMITER)
                .build();

        try (var writer = Files.newBufferedWriter(Paths.get("./resultado.csv"));
             var printer = new CSVPrinter(writer, csvFormatWriter)) {

            data.forEach(conta -> {
                try {
                    printer.printRecord(conta.getAgencia(), conta.getConta(), conta.getSaldo(), conta.getStatus(), conta.getAtualizado());
                } catch (IOException e) {
                    log.error("Nao foi possivel imprimir as informacoes da conta:" + conta.getConta());
                }
            });
            printer.flush();
        } catch (IOException e) {
            log.error("Erro ao criar arquivo de resultados!");
        }
    }

}
