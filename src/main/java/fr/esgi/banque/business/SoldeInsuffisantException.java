package fr.esgi.banque.business;

import java.math.BigDecimal;

public class SoldeInsuffisantException extends RuntimeException {

	public SoldeInsuffisantException(String numeroCompte, BigDecimal montant) {
		super("Solde insuffisant sur le compte " + numeroCompte + " pour débiter " + montant + " €");
	}

}
