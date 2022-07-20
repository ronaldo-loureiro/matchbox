# À faire


## Questions

- DaoRegistry: Est-ce que myAppCtx est utilisé pour la conversion des documents eMed ? (Méthodes getSystemDao et init)

  - La méthode init() est appellé durant la conversion par la méthode doFetchResource contenue dans ConvertingWorkerContext. myAppCtx est utilisée.
  - La méthode getSystemDao() n'est jamais appelée.
- VersionSpecificWorkerContextWrapper: Quelles sont les méthodes utilisées ? Commenter la classe
- VersionSpecificWorkerContextWrapper: Certaines méthodes devraient pouvoir être réécrite pour retourner une valeur 
  fixe (getLocale, getResourceNames, getVersion). ✓
- VersionSpecificWorkerContextWrapper: myFetchResourceCache contient un cache caffeine avec une durée très basse (1 seconde). Est-ce utile ? Lors de la conversion, est-ce que tous les appels sont miss ?
  - Les appels au cache qui accèdent à la même ressource sont effectués à la suite. De plus, en testant de changer la durée de validité du cache (1ms, 1000ms, 100000ms) et en mesurant le temps d'exécution de la fonction, il ne semble pas y avoir de différence importante entre les différents temps de validité. Chaque appel durant entre 0ms et 4ms. Seul le premier appel dure entre 500ms et 600ms. Les durées d'exécution ne semblent pas être affectées par le temps de validité.
- VersionSpecificWorkerContextWrapper: Quelle utilisation de myValidationSupportContext ? Surtout, est-ce que 
  fetchResource et generateSnapshot sont utilisés ?
- Tu crées un DaoRegistry dans ConvertingWorkerContext et un DaoRegistry est injecté dans JpaPackageCache et JpaPersistedResourceValidationSupport. Peux tu regarder :
  - si le DaoRegistry dans JpaPackageCache est utilisé (à priori non).
  - si le DaoRegistry dans JpaPersistedResourceValidationSupport est utilisé (à priori oui).
  - si le DaoRegistry dans JpaPersistedResourceValidationSupport et dans ConvertingWorkerContext sont deux instances 
    différentes .