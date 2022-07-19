# À faire


## Questions

- DaoRegistry: Est-ce que myAppCtx est utilisé pour la conversion des documents eMed ? (Méthodes getSystemDao et init)
- VersionSpecificWorkerContextWrapper: Quelles sont les méthodes utilisées ? Commenter la classe
- VersionSpecificWorkerContextWrapper: Certaines méthodes devraient pouvoir être réécrite pour retourner une valeur 
  fixe (getLocale, getResourceNames, getVersion).
- VersionSpecificWorkerContextWrapper: myFetchResourceCache contient un cache caffeine avec une durée très basse (1 
  seconde). Est-ce utile ? Lors de la conversion, est-ce que tous les appels sont miss ?
- VersionSpecificWorkerContextWrapper: Quelle utilisation de myValidationSupportContext ? Surtout, est-ce que 
  fetchResource et generateSnapshot sont utilisés ?
- Tu crées un DaoRegistry dans ConvertingWorkerContext et un DaoRegistry est injecté dans JpaPackageCache et JpaPersistedResourceValidationSupport. Peux tu regarder :
  - si le DaoRegistry dans JpaPackageCache est utilisé (à priori non).
  - si le DaoRegistry dans JpaPersistedResourceValidationSupport est utilisé (à priori oui).
  - si le DaoRegistry dans JpaPersistedResourceValidationSupport et dans ConvertingWorkerContext sont deux instances 
    différentes .