package fr.esgi.banque.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompteTest {

	@Test
	void shouldIncreaseBalanceWhenCrediting() {
		// Arrange
		Compte compte = new Compte("FR001", "Alice", new BigDecimal("100.00"), new BigDecimal("200"));

		// Act
		compte.crediter(new BigDecimal("50.00"));

		// Assert
		assertThat(compte.getSolde()).isEqualByComparingTo("150.00");
	}

	@ParameterizedTest
	@CsvSource({" , Alice", "'', Alice", "FR001, ", "FR001, ''"})
	void refuseUnNumeroOuUnTitulaireManquant(String numero, String titulaire) {
		// Arrange
		BigDecimal solde = new BigDecimal("100.00");

		// Act / Assert
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new Compte(numero, titulaire, solde, BigDecimal.ZERO));
	}

	@ParameterizedTest
	@CsvSource({" , 0", "100, ", "100, -0.01"})
	void refuseUnSoldeOuUnDecouvertInvalide(BigDecimal solde, BigDecimal decouvert) {
		// Act / Assert
		assertThatIllegalArgumentException().isThrownBy(() -> new Compte("FR001", "Alice", solde, decouvert));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"-0.01", "0.00"})
	void refuseUnDebitSansMontantStrictementPositif(String valeur) {
		// Arrange
		Compte compte = nouveauCompte();
		BigDecimal montant = valeur == null ? null : new BigDecimal(valeur);

		// Act / Assert
		assertThatIllegalArgumentException().isThrownBy(() -> compte.debiter(montant));
	}

	@Test
	void refuseUnDebitAuDelaDuDecouvertSansChangerLeSolde() {
		// Arrange
		Compte compte = nouveauCompte();
		BigDecimal montant = new BigDecimal("300.01");

		// Act / Assert
		assertThatThrownBy(() -> compte.debiter(montant)).isInstanceOf(SoldeInsuffisantException.class);
		assertThat(compte.getSolde()).isEqualByComparingTo("100.00");
	}

	@ParameterizedTest
	@CsvSource({"-0.01, true", "0, false", "0.01, false"})
	void indiqueSiLeCompteEstADecouvert(BigDecimal solde, boolean attendu) {
		// Arrange
		Compte compte = new Compte("FR001", "Alice", solde, BigDecimal.ONE);

		// Act
		boolean aDecouvert = compte.estADecouvert();

		// Assert
		assertThat(aDecouvert).isEqualTo(attendu);
	}

	private Compte nouveauCompte() {
		return new Compte("FR001", "Alice", new BigDecimal("100.00"), new BigDecimal("200.00"));
	}
}
