package fr.esgi.banque.business;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CalculFraisTest {

	private final CalculFrais calcul = new CalculFrais();

	@ParameterizedTest
	@CsvSource({"17, 500", "30, "})
	void refuseUnMineurOuUnSoldeMoyenManquant(int age, BigDecimal soldeMoyen) {
		// Act / Assert
		assertThatIllegalArgumentException().isThrownBy(() -> calcul.fraisMensuels(age, soldeMoyen, false));
	}

	@Test
	void factureDeuxEurosDeTenueDeCompte() {
		// Arrange
		BigDecimal soldeMoyen = new BigDecimal("500.00");

		// Act
		BigDecimal frais = calcul.fraisMensuels(30, soldeMoyen, false);

		// Assert
		assertThat(frais).isEqualByComparingTo("2.00");
	}

	@ParameterizedTest
	@CsvSource({"18, 500, 0.00", "25, 500, 0.00", "26, 500, 2.00"})
	void appliqueLaGratuiteAuxMoinsDeVingtSixAns(int age, BigDecimal soldeMoyen, BigDecimal fraisAttendus) {
		// Act
		BigDecimal frais = calcul.fraisMensuels(age, soldeMoyen, false);

		// Assert
		assertThat(frais).isEqualByComparingTo(fraisAttendus);
	}
}
