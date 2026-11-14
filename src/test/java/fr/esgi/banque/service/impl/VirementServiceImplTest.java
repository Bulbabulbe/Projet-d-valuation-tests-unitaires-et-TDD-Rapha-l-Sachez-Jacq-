package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.SoldeInsuffisantException;
import fr.esgi.banque.business.Virement;
import fr.esgi.banque.service.CompteIntrouvableException;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import fr.esgi.banque.service.VirementRefuseException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
		verify(repository, never()).sauvegarder(any());
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
		verify(repository, never()).sauvegarder(any());
		assertThat(source.getSolde()).isEqualByComparingTo("100.00");
	}

	@Test
	void debiteCrediteEtSauvegardeLesComptesAvecLaDateDuJour() {
		// Arrange
		Compte source = compte("FR001", "Alice", "100.00");
		Compte destination = compte("FR002", "Bob", "20.00");
		enregistrerComptesDansLeBouchon(source, destination);
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

	@Test
	void propageLeSoldeInsuffisantSansSauvegarderNiCrediter() {
		// Arrange
		Compte source = compte("FR001", "Alice", "100.00");
		Compte destination = compte("FR002", "Bob", "20.00");
		enregistrerComptesDansLeBouchon(source, destination);
		BigDecimal montant = new BigDecimal("100.01");

		// Act / Assert
		assertThatExceptionOfType(SoldeInsuffisantException.class)
				.isThrownBy(() -> service.virer("FR001", "FR002", montant));
		verify(repository, never()).sauvegarder(any());
		assertThat(source.getSolde()).isEqualByComparingTo("100.00");
		assertThat(destination.getSolde()).isEqualByComparingTo("20.00");
	}

	@Test
	void alerteLeTitulaireSourceAuDelaDeDixMilleEuros() {
		// Arrange
		enregistrerComptesDansLeBouchon(compte("FR001", "Alice", "20000"), compte("FR002", "Bob", "0"));

		// Act
		service.virer("FR001", "FR002", new BigDecimal("10000.01"));

		// Assert
		verify(notifications).alerter(eq("Alice"), anyString());
	}

	@Test
	void neDeclenchePasDAlerteADixMilleEuros() {
		// Arrange
		enregistrerComptesDansLeBouchon(compte("FR001", "Alice", "20000"), compte("FR002", "Bob", "0"));

		// Act
		service.virer("FR001", "FR002", new BigDecimal("10000.00"));

		// Assert
		verifyNoInteractions(notifications);
	}

	@Test
	void refuseUnVirementLeDimancheSansSauvegarder() {
		// Arrange
		Clock dimanche = Clock.fixed(Instant.parse("2025-01-05T12:00:00Z"), ZoneOffset.UTC);
		VirementServiceImpl serviceDuDimanche = new VirementServiceImpl(repository, notifications, dimanche);

		// Act / Assert
		assertThatExceptionOfType(VirementRefuseException.class)
				.isThrownBy(() -> serviceDuDimanche.virer("FR001", "FR002", BigDecimal.ONE));
		verify(repository, never()).sauvegarder(any());
		verifyNoInteractions(notifications);
	}

	@Test
	void accepteUnVirementLeSamedi() {
		// Arrange
		Clock samedi = Clock.fixed(Instant.parse("2025-01-04T12:00:00Z"), ZoneOffset.UTC);
		VirementServiceImpl serviceDuSamedi = new VirementServiceImpl(repository, notifications, samedi);
		Compte source = compte("FR001", "Alice", "100.00");
		Compte destination = compte("FR002", "Bob", "0");
		enregistrerComptesDansLeBouchon(source, destination);

		// Act
		Virement virement = serviceDuSamedi.virer("FR001", "FR002", BigDecimal.TEN);

		// Assert
		assertThat(virement.date()).isEqualTo(LocalDate.of(2025, 1, 4));
		verify(repository).sauvegarder(source);
		verify(repository).sauvegarder(destination);
	}

	private void enregistrerComptesDansLeBouchon(Compte source, Compte destination) {
		when(repository.trouver(source.getNumero())).thenReturn(Optional.of(source));
		when(repository.trouver(destination.getNumero())).thenReturn(Optional.of(destination));
	}

	private Compte compte(String numero, String titulaire, String solde) {
		return new Compte(numero, titulaire, new BigDecimal(solde), BigDecimal.ZERO);
	}
}
