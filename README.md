# Projet d'évaluation : tests unitaires et TDD

Projet **individuel**. Durée : environ 3h30 en séance.

Vous reprenez le code d'une petite application bancaire. Il est livré avec un seul test et contient des défauts.

- **Partie A** (environ 1h30) : tester le code existant, trouver les défauts et les corriger.
- **Partie B** (environ 2h) : développer un service de virement **en TDD**, avec Mockito.

Outils : JUnit (avec les tests paramétrés), AssertJ et Mockito. Le `pom.xml` est fourni et ne doit pas être modifié (sauf écart de version avec votre JDK local).

## 0. Démarrage (5 min)

1. Faite un fork de ce dépot sur votre compte github ou une copie local
2. Ouvrez le projet dans IntelliJ et lancez `CompteTest` : il doit passer.
3. Si vous voulez travailler uniquement le local, initialisez le dépôt git **avant toute modification** :
   ```bash
   git init
   git add .
   git commit -m "init: code fourni"
   ```

## Règles de commit

Votre historique git fait partie de la note. On doit pouvoir y lire votre démarche.

| Préfixe | Quand |
|---|---|
| `test:` | Ajout de tests sur le code existant (partie A). |
| `fix:` | Correction d'un défaut. Le test qui le prouve est commité **juste avant**, avec le préfixe `test:`. |
| `red:` | Partie B : un nouveau test qui échoue (le code peut ne pas compiler s'il manque la classe). |
| `green:` | Partie B : le code minimal qui fait passer le test. |
| `refactor:` | Amélioration sans changement de comportement, tous les tests au vert. |

Exemple : `red: V1 refuse un montant nul`, puis `green: V1 refuse un montant nul`.

## Partie A : tester et corriger l'existant

Les règles métier font référence. **Si le code ne les respecte pas, c'est le code qui a tort.**

### Classe `Compte`

| Règle | Comportement attendu |
|---|---|
| C1 | Le numéro et le titulaire sont obligatoires (ni `null`, ni vides). Sinon `IllegalArgumentException`. |
| C2 | Le solde initial et le découvert autorisé sont obligatoires. Le découvert autorisé est positif ou nul. Sinon `IllegalArgumentException`. |
| C3 | `crediter` n'accepte qu'un montant strictement positif. Sinon `IllegalArgumentException`. |
| C4 | `debiter` n'accepte qu'un montant strictement positif. Sinon `IllegalArgumentException`. |
| C5 | Un débit est accepté tant que le solde ne passe pas sous `-découvertAutorisé`. **Atteindre exactement la limite est autorisé.** Au-delà : `SoldeInsuffisantException`, et le solde ne change pas. |
| C6 | `estVide()` est vrai quand le solde vaut zéro, quelle que soit son écriture (`0`, `0.00`...). |
| C7 | `estADecouvert()` est vrai quand le solde est strictement négatif. |

Exemple pour C5 : avec un solde de 100 € et un découvert autorisé de 200 €, débiter 300 € est accepté (solde -200 €, la limite exacte). Débiter 300,01 € est refusé.

### Classe `CalculFrais`

`fraisMensuels(int ageTitulaire, BigDecimal soldeMoyen, boolean cartePremium)` renvoie les frais du mois.

| Règle | Comportement attendu |
|---|---|
| F1 | Le titulaire doit avoir 18 ans ou plus, et le solde moyen est obligatoire. Sinon `IllegalArgumentException`. |
| F2 | Les frais de tenue de compte sont de 2,00 € par mois. |
| F3 | Les frais de tenue sont offerts aux titulaires de **moins de 26 ans**. |
| F4 | Les frais de tenue sont offerts si le solde moyen atteint **1 000 € ou plus**. |
| F5 | Une carte premium ajoute 5,00 € par mois. Ce supplément est **toujours dû**, même quand la tenue est offerte. |

### Travail demandé

1. Écrivez les tests de `Compte` et de `CalculFrais`, règle par règle, au format Arrange / Act / Assert.
2. Utilisez des **tests paramétrés** (`@ParameterizedTest`, `@CsvSource`) quand plusieurs cas suivent la même logique, en particulier pour les **valeurs limites**.
3. Pour chaque défaut trouvé :
   - commitez le test qui échoue (`test: ...`) ;
   - corrigez le code et commitez (`fix: ...`).
4. Listez les défauts trouvés dans le fichier `RENDU.md` (voir plus bas).

Astuce : pour comparer des `BigDecimal`, utilisez `isEqualByComparingTo("2.00")` et non `isEqualTo`.

## Partie B : service de virement en TDD

Vous développez la classe `fr.esgi.banque.service.impl.VirementServiceImpl` **en partant des tests**. Aucun code de production ne doit être écrit sans un test rouge qui le justifie.

### Contrat

```java
public VirementServiceImpl(CompteRepository repository, NotificationService notifications, Clock horloge)

public Virement virer(String numeroSource, String numeroDestination, BigDecimal montant)
```

- `CompteRepository` et `NotificationService` sont des interfaces **sans implémentation** : utilisez des doublures Mockito.
- `Clock` (`java.time`) donne la date du jour. Dans les tests, utilisez `Clock.fixed(...)`.
- `Virement` est un `record` à créer, avec les champs `numeroSource`, `numeroDestination`, `montant` et `date` (`LocalDate`).
- Les exceptions `CompteIntrouvableException` et `VirementRefuseException` sont à créer (`RuntimeException`).

### Règles, à traiter dans l'ordre

| Règle | Comportement attendu |
|---|---|
| V1 | Un montant `null`, nul ou négatif est refusé : `IllegalArgumentException`. |
| V2 | Un virement vers le même compte est refusé : `IllegalArgumentException`. |
| V3 | Si l'un des deux comptes n'existe pas (`trouver` renvoie `Optional.empty()`) : `CompteIntrouvableException`. |
| V4 | Cas nominal : le compte source est débité, le compte destination est crédité, **les deux comptes sont sauvegardés**. Le `Virement` renvoyé contient la date du jour. |
| V5 | Si le solde de la source est insuffisant, la `SoldeInsuffisantException` remonte et **rien n'est sauvegardé**. |
| V6 | Un virement **strictement supérieur à 10 000 €** déclenche une alerte au titulaire du compte source (`NotificationService.alerter`). Pas d'alerte sinon. |
| V7 | Aucun virement n'est accepté le **dimanche** : `VirementRefuseException`, et aucun compte n'est sauvegardé. |

### Ce qui est attendu

- Un cycle rouge, vert, refactor visible dans l'historique git pour chaque règle.
- Des doublures adaptées : bouchon (`when ... thenReturn`) pour fournir des comptes, vérification (`verify`, `never()`) pour les sauvegardes et les alertes.
- Les valeurs limites de V6 testées (10 000 € et 10 000,01 €).

## Rendu

En fin de séance :

1. Créez un fichier `RENDU.md` à la racine qui contient :
   - vos nom, prénom et Campus;
   - la liste des défauts trouvés en partie A (règle concernée, description, nom du test qui le prouve) ;
   - une capture de la couverture de code vue dans IntelliJ (*Run with Coverage*) ;
   - les règles de la partie B que vous n'avez pas terminées, s'il y en a.
2. Commitez, puis :
   - **Soit** déposez l'archive du dossier en zip (**avec le dossier `.git`**) auprès du formateur (message teams ou mail) 
   - **Ou** communiquez l'adresse de votre repo (en public) au formateur.

Le dernier commit doit compiler et tous ses tests doivent passer. Une règle non terminée vaut mieux qu'un projet cassé : commitez souvent.
