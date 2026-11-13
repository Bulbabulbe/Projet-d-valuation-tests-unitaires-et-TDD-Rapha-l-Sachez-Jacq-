package fr.esgi.banque.service;

import fr.esgi.banque.business.Compte;

import java.util.Optional;

/**
 * Accès aux comptes (base de données). Aucune implémentation n'est fournie :
 * dans les tests, on utilise une doublure.
 */
public interface CompteRepository {

	Optional<Compte> trouver(String numero);

	void sauvegarder(Compte compte);

}
