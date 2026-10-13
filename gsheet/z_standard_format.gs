const CACHE_KEY_CATEGORIES = 'categories_map_v2';
const CACHE_EXPIRATION_SECONDS = 21600; // 6 h, invalidé dès qu'on édite l'onglet "Categories"

// Note: TECHNICAL_SHEETS est définie dans a_main.gs

/**
 * Retourne la map { catégorie: [sous-catégories] } construite depuis l'onglet "Categories"
 * (colonne B = catégorie, colonne C = sous-catégorie), avec cache.
 *
 * @returns {Object<string, string[]>}
 */
function getCategoryMap() {
  const cache = CacheService.getScriptCache();
  const cached = cache.get(CACHE_KEY_CATEGORIES);
  if (cached) {
    return JSON.parse(cached);
  }

  const catSheet = SpreadsheetApp.getActive().getSheetByName("Categories");
  if (!catSheet) {
    throw new Error("Sheet 'Categories' not found.");
  }

  const values = catSheet.getDataRange().getValues();
  const map = {};
  for (let i = 1; i < values.length; i++) { // ignore l'entête
    const cat = values[i][1];
    const sub = values[i][2];
    if (cat === "" || sub === "") continue;
    (map[cat] = map[cat] || []).push(String(sub));
  }

  try {
    cache.put(CACHE_KEY_CATEGORIES, JSON.stringify(map), CACHE_EXPIRATION_SECONDS);
  } catch (err) {
    // ex: valeur > 100 Ko : on continue sans cache plutôt que de casser la validation
    console.warn("cache categories non stocké : " + err.message);
  }
  return map;
}

/**
 * Crée une validation de données dans la colonne F en fonction de la valeur entrée en colonne E.
 * Gère aussi les collages multi-lignes.
 * @param {GoogleAppsScript.Events.SheetsOnEdit} e
 */
function onEdit(e) {
  if (!e) {
    // Évitez l'exécution manuelle
    throw new Error('Please do not run the script in the script editor window.');
  }

  const range = e.range;
  const sheet = range.getSheet();
  const sheetName = sheet.getName();

  // Modification des catégories -> on invalide le cache
  if (sheetName === "Categories") {
    CacheService.getScriptCache().remove(CACHE_KEY_CATEGORIES);
    return;
  }

  // L'édition doit toucher la colonne E (5)
  if (range.getColumn() > 5 || range.getLastColumn() < 5) {
    return;
  }

  if (TECHNICAL_SHEETS.includes(sheetName)) {
    return;
  }

  const firstRow = Math.max(range.getRow(), 2); // ignore l'entête
  const lastRow = range.getLastRow();
  if (lastRow < firstRow) {
    return;
  }
  const nbRows = lastRow - firstRow + 1;

  // Valeurs de E : e.value pour une cellule, lecture groupée pour un collage / une suppression de plage
  const editedValues = (nbRows === 1 && range.getNumColumns() === 1 && e.value !== undefined)
    ? [e.value]
    : sheet.getRange(firstRow, 5, nbRows, 1).getValues().map(r => r[0]);

  let map;
  try {
    map = getCategoryMap();
  } catch (error) {
    console.error(error.message);
    return;
  }

  const rules = editedValues.map(v => {
    const list = map[v];
    if (list && list.length) {
      return [SpreadsheetApp.newDataValidation().requireValueInList(list, true).setAllowInvalid(false).build()];
    }
    return [null]; // null = efface la validation
  });

  // 1 seul appel pour toutes les lignes
  sheet.getRange(firstRow, 6, nbRows, 1).setDataValidations(rules);
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
