package fr.esgi.banque.service;

/**
 * Envoi d'alertes aux clients (SMS, mail...). Aucune implémentation n'est fournie :
 * dans les tests, on utilise une doublure.
 */
public interface NotificationService {

	void alerter(String titulaire, String message);

}
