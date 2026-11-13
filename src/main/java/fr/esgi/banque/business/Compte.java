package fr.esgi.banque.business;

import java.math.BigDecimal;

/**
 * Compte bancaire avec découvert autorisé.
 *
 * <p>Règles métier (voir README) :
 * <ul>
 *   <li>C1 : le numéro et le titulaire sont obligatoires (ni null, ni vides).</li>
 *   <li>C2 : le solde initial et le découvert autorisé sont obligatoires, le découvert est positif ou nul.</li>
 *   <li>C3 : on ne crédite qu'un montant strictement positif.</li>
 *   <li>C4 : on ne débite qu'un montant strictement positif.</li>
 *   <li>C5 : un débit est accepté tant que le solde ne descend pas sous -découvert autorisé
 *       (atteindre exactement la limite est autorisé). Sinon SoldeInsuffisantException et le solde ne change pas.</li>
 *   <li>C6 : un compte est vide quand son solde vaut zéro, quelle que soit l'écriture (0, 0.00...).</li>
 *   <li>C7 : un compte est à découvert quand son solde est strictement négatif.</li>
 * </ul>
 */
public class Compte {

	private final String numero;
	private final String titulaire;
	private final BigDecimal decouvertAutorise;
	private BigDecimal solde;

	public Compte(String numero, String titulaire, BigDecimal soldeInitial, BigDecimal decouvertAutorise) {
		if (numero == null || numero.isBlank()) {
			throw new IllegalArgumentException("Le numéro de compte est obligatoire");
		}
		if (titulaire == null || titulaire.isBlank()) {
			throw new IllegalArgumentException("Le titulaire est obligatoire");
		}
		if (soldeInitial == null || decouvertAutorise == null) {
			throw new IllegalArgumentException("Le solde initial et le découvert autorisé sont obligatoires");
		}
		if (decouvertAutorise.signum() < 0) {
			throw new IllegalArgumentException("Le découvert autorisé doit être positif ou nul");
		}
		this.numero = numero;
		this.titulaire = titulaire;
		this.solde = soldeInitial;
		this.decouvertAutorise = decouvertAutorise;
	}

	public void crediter(BigDecimal montant) {
		if (montant == null || montant.signum() < 0) {
			throw new IllegalArgumentException("Le montant à créditer doit être strictement positif");
		}
		solde = solde.add(montant);
	}

	public void debiter(BigDecimal montant) {
		if (montant == null || montant.signum() <= 0) {
			throw new IllegalArgumentException("Le montant à débiter doit être strictement positif");
		}
		BigDecimal nouveauSolde = solde.subtract(montant);
		if (nouveauSolde.compareTo(decouvertAutorise.negate()) <= 0) {
			throw new SoldeInsuffisantException(numero, montant);
		}
		solde = nouveauSolde;
	}

	public boolean estVide() {
		return solde.equals(BigDecimal.ZERO);
	}

	public boolean estADecouvert() {
		return solde.signum() < 0;
	}

	public String getNumero() {
		return numero;
	}

	public String getTitulaire() {
		return titulaire;
	}

	public BigDecimal getSolde() {
		return solde;
	}

	public BigDecimal getDecouvertAutorise() {
		return decouvertAutorise;
	}

}
