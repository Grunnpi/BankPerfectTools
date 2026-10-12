const CACHE_KEY_CATEGORIES = 'categories_data';
const CACHE_EXPIRATION_SECONDS = 3000; // 50 minutes (3000 secondes)

// Note: Assurez-vous que la constante TECHNICAL_SHEETS est définie ailleurs si elle est utilisée.
// ex: const TECHNICAL_SHEETS = ['FeuilleTechnique', 'AutreFeuille'];

/**
 * Lit les données de la feuille "Categories" en utilisant le cache si possible.
 *
 * @returns {Array<Array<any>>} Les valeurs de la feuille "Categories".
 */
function getCategoryDataFromCache() {
  const cache = CacheService.getScriptCache();
  const cachedData = cache.get(CACHE_KEY_CATEGORIES);

  if (cachedData) {
    // Les données sont dans le cache (JSON.parse est rapide)
    return JSON.parse(cachedData);
  }

  // Si non trouvé dans le cache, lire depuis la feuille
  const ss = SpreadsheetApp.getActive();
  const catSheet = ss.getSheetByName("Categories");

  if (!catSheet) {
    throw new Error("Sheet 'Categories' not found.");
  }
  
  // Lecture coûteuse
  const values = catSheet.getDataRange().getValues();

  // Stockage dans le cache pour les futures exécutions (JSON.stringify est rapide)
  cache.put(CACHE_KEY_CATEGORIES, JSON.stringify(values), CACHE_EXPIRATION_SECONDS);

  return values;
}

/**
 * Crée une validation de données dans la colonne F en fonction de la valeur entrée en colonne E.
 * @param {GoogleAppsScript.Events.SheetsOnEdit} e
 */
function onEdit(e) {
  if (!e) {
    // Évitez l'exécution manuelle
    throw new Error('Please do not run the script in the script editor window.');
  }

  const range = e.range;
  const sheet = range.getSheet();
  const column = range.getColumn();
  const row = range.getRow();
  
  // Sortir tôt si ce n'est pas la colonne E (colonne 5)
  if (column !== 5) {
    return;
  }
  
  const sheetName = sheet.getName();

  // Gérer l'ignorance des feuilles techniques (si la constante existe)
  if (typeof TECHNICAL_SHEETS !== 'undefined' && TECHNICAL_SHEETS.includes(sheetName)) {
    console.log("ignore sheet " + sheetName);
    return;
  }
  
  const targetCell = sheet.getRange(row, 6); // Cellule cible dans la colonne F (6)
  
  // Traitement de l'effacement
  if (e.value === undefined) {
    targetCell.clearDataValidations();
    return;
  }
  
  // === Appel optimisé avec cache pour les valeurs ===
  try {
    const values = getCategoryDataFromCache();
    // ===============================================

    const editedValue = range.getValue();

    let startIndex = -1;
    let endIndex = -1;
    let foundStart = false; // Pour marquer le début de la plage

    // Recherche de la plage de catégories dans le tableau en mémoire
    // On commence à l'index 1 (ligne 2) pour ignorer l'entête
    for (let i = 1; i < values.length; i++) {
      // La colonne B est l'index 1 dans le tableau JavaScript (base zéro)
      const categoryValue = values[i][1]; 
      
      if (categoryValue === editedValue) {
        if (!foundStart) {
          startIndex = i + 1; // Le numéro de ligne est l'index + 1
          foundStart = true;
        }
        endIndex = i + 1; // Toujours mettre à jour le dernier élément
      } else {
        if (foundStart) {
          break; // Sortie anticipée : la plage s'est terminée
        }
      }
    }

    if (startIndex > 0) {
      // **Optimisation Mineure** : Récupérer la feuille de calcul active et la feuille "Categories" une seule fois.
      const ss = SpreadsheetApp.getActive();
      const catSheet = ss.getSheetByName("Categories"); 
      
      if (!catSheet) {
        throw new Error("Sheet 'Categories' not found during validation creation.");
      }
      
      // Construire la plage A1 pour la colonne C (Index 2)
      const categoryRangeA1 = 'C' + startIndex + ':C' + endIndex; 
      
      // **Appel au service coûteux mais nécessaire pour la validation de données par plage**
      const validationRange = catSheet.getRange(categoryRangeA1);
      
      const rule = SpreadsheetApp.newDataValidation().requireValueInRange(validationRange).build();
      targetCell.setDataValidation(rule);
      
    } else {
      // Si aucune catégorie n'est trouvée pour la valeur entrée
      targetCell.clearDataValidations();
    }
  } catch(error) {
    console.error(error.message);
    // Optionnel : Effacer la validation pour éviter une erreur visible à l'utilisateur
    targetCell.clearDataValidations();
  }
}

// Fonction de formatage standard (peut être lancée manuellement)
function applyStandardFormat() {
  const ss = SpreadsheetApp.getActive();
  
  // Mise en place de la validation pour la colonne F entière (défaut)
  // Note: Cette fonction est pour un formatage initial/manuel et n'est pas critique pour la performance onEdit.
  var cell = ss.getRange('F2:F');

  // La plage de validation ici est 'B1:B10', assurez-vous que c'est bien la plage voulue
  var range = ss.getRange('B1:B10'); 
  var rule = SpreadsheetApp.newDataValidation().requireValueInRange(range).build();
  cell.setDataValidation(rule);
}