package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.Virement;
import fr.esgi.banque.service.CompteIntrouvableException;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import fr.esgi.banque.service.VirementRefuseException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public class VirementServiceImpl {

	private static final BigDecimal SEUIL_ALERTE = new BigDecimal("10000");

	private final CompteRepository repository;
	private final Clock horloge;
	private final NotificationService notifications;

	public VirementServiceImpl(CompteRepository repository, NotificationService notifications, Clock horloge) {
		this.repository = repository;
		this.horloge = horloge;
		this.notifications = notifications;
	}

	public Virement virer(String numeroSource, String numeroDestination, BigDecimal montant) {
		verifierMontant(montant);
		if (Objects.equals(numeroSource, numeroDestination)) {
			throw new IllegalArgumentException("Les deux comptes doivent être différents");
		}
		LocalDate date = LocalDate.now(horloge);
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			throw new VirementRefuseException("Les virements ne sont pas acceptés le dimanche");
		}
		Compte source = trouverCompte(numeroSource);
		Compte destination = trouverCompte(numeroDestination);
		source.debiter(montant);
		destination.crediter(montant);
		repository.sauvegarder(source);
		repository.sauvegarder(destination);
		if (montant.compareTo(SEUIL_ALERTE) > 0) {
			notifications.alerter(source.getTitulaire(), "Virement de " + montant.toPlainString() + " €");
		}
		return new Virement(numeroSource, numeroDestination, montant, date);
	}

	private Compte trouverCompte(String numero) {
		return repository.trouver(numero).orElseThrow(() -> new CompteIntrouvableException(numero));
	}

	private void verifierMontant(BigDecimal montant) {
		if (montant == null || montant.signum() <= 0) {
			throw new IllegalArgumentException("Le montant du virement doit être strictement positif");
		}
	}
}
