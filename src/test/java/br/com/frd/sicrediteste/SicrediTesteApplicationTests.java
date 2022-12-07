package br.com.frd.sicrediteste;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

@SpringBootTest
class SicrediTesteApplicationTests {

	@Test
	@DisplayName("Valida se as informacoes do relatorio estão sendo lidas e convertidas corretamente.")
	void readingAllRecordsFromFile() {
		List<DadosConta> testData = new SincronizacaoService().readRecords("src/test/resources/relatorio_success.csv");
		assertThat(testData.get(0)).isEqualTo(DadosConta.builder().agencia("0101").conta("12225-6").saldo(Double.valueOf(100)).status("A").build());
		assertThat(testData.get(1)).isEqualTo(DadosConta.builder().agencia("0101").conta("12226-8").saldo(Double.valueOf(3200.5)).status("A").build());
	}

	@Test
	@DisplayName("Invalida relatorio com cabecalho errado")
	void validateHeadersFromFile() {
		List<DadosConta> testData = new SincronizacaoService().readRecords("src/test/resources/relatorio_success.csv");
		assertThatRuntimeException().describedAs("Cabecalho invalido!");
	}

	@Test
	@DisplayName("Testa o envio dos dados a receita.")
	void testSendingDataToReceitaSuccess() {
		List<DadosConta> testData = new SincronizacaoService().readRecords("src/test/resources/relatorio_receita_atualizado_e_nao_atualizado.csv");
		new SincronizacaoService().sendDataToReceita(testData);
		assertThat(testData.get(0).getAtualizado()).isEqualTo("Atualizado");
		assertThat(testData.get(1).getAtualizado()).isEqualTo("Atualizado");
		assertThat(testData.get(2).getAtualizado()).isEqualTo("Nao Atualizado");
		assertThat(testData.get(3).getAtualizado()).isEqualTo("Nao Atualizado");
		assertThat(testData.get(4).getAtualizado()).isEqualTo("Atualizado");
	}


}
