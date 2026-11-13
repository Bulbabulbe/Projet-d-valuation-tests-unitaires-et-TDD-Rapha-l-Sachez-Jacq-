package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Virement;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;

import java.math.BigDecimal;
import java.time.Clock;

public class VirementServiceImpl {

	public VirementServiceImpl(CompteRepository repository, NotificationService notifications, Clock horloge) {
	}

	public Virement virer(String numeroSource, String numeroDestination, BigDecimal montant) {
		if (montant == null || montant.signum() <= 0) {
			throw new IllegalArgumentException("Le montant du virement doit être strictement positif");
		}
		throw new UnsupportedOperationException();
	}
}
