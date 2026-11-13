package fr.esgi.banque.service;

public class CompteIntrouvableException extends RuntimeException {

	public CompteIntrouvableException(String numero) {
		super("Compte introuvable : " + numero);
	}
}
