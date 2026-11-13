package fr.esgi.banque.service.impl;

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
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
}
