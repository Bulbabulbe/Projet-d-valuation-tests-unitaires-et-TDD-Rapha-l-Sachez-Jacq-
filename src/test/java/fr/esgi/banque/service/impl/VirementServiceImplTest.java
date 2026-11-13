package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.Virement;
import fr.esgi.banque.service.CompteIntrouvableException;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VirementServiceImplTest {

	private final Clock horloge = Clock.fixed(Instant.parse("2025-01-06T12:00:00Z"), ZoneOffset.UTC);

	@Mock
	private CompteRepository repository;

	@Mock
	private NotificationService notifications;

	private VirementServiceImpl service;

	@BeforeEach
	void preparerService() {
		service = new VirementServiceImpl(repository, notifications, horloge);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"0.00", "-0.01"})
	void refuseUnMontantNonPositifOuAbsent(String valeur) {
		// Arrange
		BigDecimal montant = valeur == null ? null : new BigDecimal(valeur);

		// Act / Assert
		assertThatIllegalArgumentException().isThrownBy(() -> service.virer("FR001", "FR002", montant));
	}

	@Test
	void refuseUnVirementVersLeMemeCompte() {
		// Arrange
		BigDecimal montant = BigDecimal.ONE;

		// Act / Assert
		assertThatIllegalArgumentException().isThrownBy(() -> service.virer("FR001", "FR001", montant));
	}

	@Test
	void refuseUnVirementSiLeCompteSourceEstIntrouvable() {
		// Arrange
		when(repository.trouver("FR001")).thenReturn(Optional.empty());

		// Act / Assert
		assertThatExceptionOfType(CompteIntrouvableException.class)
				.isThrownBy(() -> service.virer("FR001", "FR002", BigDecimal.ONE));
	}

	@Test
	void refuseUnVirementSiLeCompteDestinationEstIntrouvable() {
		// Arrange
		Compte source = new Compte("FR001", "Alice", new BigDecimal("100.00"), BigDecimal.ZERO);
		when(repository.trouver("FR001")).thenReturn(Optional.of(source));
		when(repository.trouver("FR002")).thenReturn(Optional.empty());

		// Act / Assert
		assertThatExceptionOfType(CompteIntrouvableException.class)
				.isThrownBy(() -> service.virer("FR001", "FR002", BigDecimal.ONE));
	}

	@Test
	void debiteCrediteEtSauvegardeLesComptesAvecLaDateDuJour() {
		// Arrange
		Compte source = new Compte("FR001", "Alice", new BigDecimal("100.00"), BigDecimal.ZERO);
		Compte destination = new Compte("FR002", "Bob", new BigDecimal("20.00"), BigDecimal.ZERO);
		when(repository.trouver("FR001")).thenReturn(Optional.of(source));
		when(repository.trouver("FR002")).thenReturn(Optional.of(destination));
		BigDecimal montant = new BigDecimal("30.00");

		// Act
		Virement virement = service.virer("FR001", "FR002", montant);

		// Assert
		assertThat(source.getSolde()).isEqualByComparingTo("70.00");
		assertThat(destination.getSolde()).isEqualByComparingTo("50.00");
		verify(repository).sauvegarder(source);
		verify(repository).sauvegarder(destination);
		assertThat(virement.numeroSource()).isEqualTo("FR001");
		assertThat(virement.numeroDestination()).isEqualTo("FR002");
		assertThat(virement.montant()).isEqualByComparingTo(montant);
		assertThat(virement.date()).isEqualTo(LocalDate.of(2025, 1, 6));
	}
}
