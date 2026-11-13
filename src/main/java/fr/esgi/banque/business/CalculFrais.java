package fr.esgi.banque.business;

import java.math.BigDecimal;

/**
 * Calcul des frais bancaires mensuels.
 *
 * <p>Règles métier (voir README) :
 * <ul>
 *   <li>F1 : le titulaire doit être majeur (18 ans ou plus) et le solde moyen est obligatoire,
 *       sinon IllegalArgumentException.</li>
 *   <li>F2 : les frais de tenue de compte sont de 2,00 € par mois.</li>
 *   <li>F3 : les frais de tenue sont offerts aux titulaires de moins de 26 ans.</li>
 *   <li>F4 : les frais de tenue sont offerts si le solde moyen du mois atteint 1 000 € ou plus.</li>
 *   <li>F5 : une carte premium ajoute 5,00 € par mois, toujours dus, même quand la tenue est offerte.</li>
 * </ul>
 */
public class CalculFrais {

	static final BigDecimal FRAIS_TENUE = new BigDecimal("2.00");
	static final BigDecimal SUPPLEMENT_PREMIUM = new BigDecimal("5.00");
	static final int AGE_MAJORITE = 18;
	static final int AGE_JEUNE = 26;
	static final BigDecimal SEUIL_GRATUITE = new BigDecimal("1000");

	public BigDecimal fraisMensuels(int ageTitulaire, BigDecimal soldeMoyen, boolean cartePremium) {
		if (ageTitulaire < AGE_MAJORITE) {
			throw new IllegalArgumentException("Le titulaire doit être majeur");
		}
		if (soldeMoyen == null) {
			throw new IllegalArgumentException("Le solde moyen est obligatoire");
		}
		if (ageTitulaire < AGE_JEUNE) {
			return BigDecimal.ZERO;
		}
		if (soldeMoyen.compareTo(SEUIL_GRATUITE) >= 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal frais = FRAIS_TENUE;
		if (cartePremium) {
			frais = frais.add(SUPPLEMENT_PREMIUM);
		}
		return frais;
	}

}
