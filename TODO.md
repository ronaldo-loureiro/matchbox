# À faire

- Investiguer comment lancer une deuxième app Spring depuis une première et communiquer entre les deux.

# Fait <span style="color:green">✓</span>

- VersionSpecificWorkerContextWrapper: Quelles sont les méthodes utilisées ? Commenter la classe
- VersionSpecificWorkerContextWrapper: Certaines méthodes devraient pouvoir être réécrite pour retourner une valeur 
  fixe (getLocale, getResourceNames, getVersion).
- Utiliser un H2 en mémoire à la place de Postgres
- Nettoyer à fond.
- Installer les IGs depuis un paquet local 

# Questions ?

<details>
  <summary>DaoRegistry: Est-ce que myAppCtx est utilisé pour la conversion des documents eMed ? (Méthodes getSystemDao et init)</summary>

  - La méthode init() est appellé durant la conversion par la méthode doFetchResource contenue dans ConvertingWorkerContext. myAppCtx est utilisée.
  - La méthode getSystemDao() n'est jamais appelée.

</details>

<details>
  <summary>VersionSpecificWorkerContextWrapper: myFetchResourceCache contient un cache caffeine avec une durée très basse (1 seconde). Est-ce utile ? Lors de la conversion, est-ce que tous les appels sont miss ?</summary>
 
  - En testant de changer la durée de validité du cache (1ms, 1000ms, 100000ms) et en mesurant le temps d'exécution de la fonction, il n'y a pas de différence avec des temps de validité supérieur à 1000ms. Chaque appel durant entre 0ms et 4ms. Seul le premier appel dure entre 500ms et 600ms.
</details>

<details>
  <summary>VersionSpecificWorkerContextWrapper: Quelle utilisation de myValidationSupportContext ? Surtout, est-ce que 
  fetchResource et generateSnapshot sont utilisés ?</summary>

  - myValidationSupportContext devenu myValidationSupport de type JpaValidationSupportChain permet d'accéder aux ressources de la base de données. La méthode fetchResource en a besoin pour récupérer une ressource qui ne serait pas dans le cache. generateSnapshot n'est pas utilisé car elle n'est pas utile à la conversion.
</details>

<details>
  <summary>Tu crées un DaoRegistry dans ConvertingWorkerContext et un DaoRegistry est injecté dans JpaPackageCache et JpaPersistedResourceValidationSupport. Peux tu regarder :</summary>

  - si le DaoRegistry dans JpaPackageCache est utilisé (à priori non).
    - Oui, il est utilisé lors du téléchargement et installation des IGs

  - si le DaoRegistry dans JpaPersistedResourceValidationSupport est utilisé (à priori oui).
    - Oui, il est uniquement utilisé dans la méthode doFetchResource(@Nullable Class\<T\> theClass, String theUri)

  - si le DaoRegistry dans JpaPersistedResourceValidationSupport et dans ConvertingWorkerContext sont deux instances 
    différentes .
    - Oui, ce sont deux instances différentes

</details>