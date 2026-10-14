const IMPORT_FOLDER_ID = "1Qn8LXy6b12xIslJGoFd7DbTfovWPipVp";

/**
 * Origines d'import disponibles dans la fenêtre unique (dialog_upload.html).
 * Chaque origine garde son propre parsing : `run` reçoit le classeur converti (Spreadsheet)
 * et appelle l'import spécifique (b_upload_cm.gs, b_upload_bil.gs, ...).
 * Pour ajouter une origine : ajouter une entrée ici + sa fonction d'import.
 * (`run` est une fonction enveloppe pour ne pas dépendre de l'ordre de chargement des fichiers.)
 */
const IMPORT_ORIGINS = [
  { id: "CreditMutuel", label: "Crédit Mutuel", hint: "comptes.xlsm",                accept: ".xls,.xlsx,.xlsm",
    run: function (ss) { importCreditMutuel(ss) } },                 // 1 onglet = 1 compte
  { id: "BIL",          label: "BIL",           hint: "list_movements.csv",          accept: ".csv",
    run: function (ss) { importBIL(ss.getSheets()[0]) } },            // 1 onglet pour 1 compte
  { id: "output",       label: "Output",        hint: "output.csv",                  accept: ".csv",
    run: function (ss) { importOUTPUT(ss.getSheets()[0]) } },         // 1 onglet pour plusieurs comptes
  { id: "BOURSORAMA",   label: "Boursorama",    hint: "export-operations-xxx.csv",   accept: ".csv",
    run: function (ss) { importBOURSORAMA(ss.getSheets()[0]) } }      // 1 onglet pour 1 compte
];

// Version sérialisable pour la fenêtre (sans les fonctions)
function getImportOrigins() {
  return IMPORT_ORIGINS.map(function (o) {
    return { id: o.id, label: o.label, hint: o.hint, accept: o.accept };
  });
}

function uploadFile(uploadedFile, fileName, accountType)
{
  try{
    
    // decode the base64 and convert to  blob
    var decoded    
    var blob
    try {
      decoded = Utilities.base64Decode(uploadedFile);
      blob = Utilities.newBlob(decoded,MimeType.MICROSOFT_EXCEL, fileName);
    } catch(err) {
      log_error("Erreur à l'upload de fichier")
      log_error(uploadedFile)
      return
    }
    
    // Use this to save the file as excel
    const folder = DriveApp.getFolderById(IMPORT_FOLDER_ID);
    const csvFile = folder.createFile(blob); 

    const today = new Date();
    const formattedDate = Utilities.formatDate(today, Session.getScriptTimeZone(), "yyyy-MM-dd_HH-mm-ss");

    const sheetName = formattedDate + "-importedSheetFrom-" + fileName

    let imported_file_id = "??"

    if ( fileName.endsWith(".csv")) {
      log_info("csv : on tente un cast supplémentaire du fichier [" + fileName + "]")

      // Si les accents sont cassés (CSV Windows-1252), utiliser getDataAsString("windows-1252")
      const csvContent = csvFile.getBlob().getDataAsString(); // Contenu du fichier CSV

      const lines = csvContent.split("\n");

      // Choisir la ligne pour la détection : 6 si disponible, sinon 2, sinon 1
      let detectionLine;
      if (lines.length >= 6) {
        detectionLine = lines[5]; // index 5 = 6ème ligne
      } else if (lines.length >= 2) {
        detectionLine = lines[1]; // index 1 = 2ème ligne
      } else {
        detectionLine = lines[0]; // index 0 = 1ère ligne
      }

      // Compter les séparateurs
      const countComma = (detectionLine.match(/,/g) || []).length;
      const countSemicolon = (detectionLine.match(/;/g) || []).length;
      const separator = countSemicolon > countComma ? ";" : ",";

      log_info("csv : detection auto du séparateur [" + separator + "]")

      // Parsing CSV (gère les guillemets et les séparateurs dans les champs)
      let rows;
      try {
        rows = Utilities.parseCsv(csvContent, separator);
      } catch (parseErr) {
        log_warn("parseCsv a échoué [" + parseErr.message + "] : fallback split")
        rows = csvContent.split(/\r?\n/).map(line => line.split(separator).map(cell => cell.replace(/^"|"$/g, '')));
      }

      // Créer un fichier Google Sheet
      const spreadsheet = SpreadsheetApp.create(sheetName);
      const sheet = spreadsheet.getActiveSheet();
      sheet.setName("ConversionTabCSV")

      // Rendre le tableau rectangulaire puis 1 seule écriture
      if (rows.length > 0) {
        const width = Math.max.apply(null, rows.map(r => r.length));
        if (width > 0) {
          const padded = rows.map(r => r.concat(new Array(width - r.length).fill("")));
          sheet.getRange(1, 1, padded.length, width).setValues(padded);
        }
      }

      // Déplacer le fichier dans le dossier cible
      DriveApp.getFileById(spreadsheet.getId()).moveTo(folder);

      imported_file_id = spreadsheet.getId()
      log_info("conversion par code et move du fichier [" + sheetName + "]")

      // Supprimer le fichier CSV original (optionnel)
      // csvFile.setTrashed(true);
    } else {
      log_info("import et conversion auto du fichier [" + sheetName + "]")

      // Enable the advanced Drive Service to use this function that converts the excel  file to a Google Sheets file
      let imported_file = Drive.Files.insert({title:sheetName, mimeType: MimeType.GOOGLE_SHEETS, parents: [{ id: IMPORT_FOLDER_ID }] }, blob, {convert: true, supportsAllDrives: true});
      imported_file_id = imported_file.id
    }

    var cache = CacheService.getScriptCache();
    cache.put("gsheet_imported_id",imported_file_id)
    cache.put("accountType",accountType)
    PropertiesService.getUserProperties().setProperty("lastImportOrigin", accountType) // présélection à la prochaine ouverture
    
    log_info("importe & convertie [" + fileName + "]")

    return "Success";
  }catch(ex)
  {
    Logger.log(ex);
    throw ex;
  } finally {
    flushLogs();
  }
}

function displayChoice(choice) {
  displayToastAlert("Importation ok !")
  return;
}

function importFile() {
  try {
    var cache = CacheService.getScriptCache();
    const imported_id = cache.get("gsheet_imported_id")
    const accountType = cache.get("accountType")

    // cache.get renvoie null si la clé est absente/expirée : on teste la valeur, pas != ""
    if ( imported_id && accountType ) {
      cache.remove("gsheet_imported_id")
      cache.remove("accountType")

      var spreadsheet_imported = SpreadsheetApp.openById(imported_id)
      var sheet = spreadsheet_imported.getSheets()[0]
      //displayToastAlert("Sheet cible [" + sheet.getName() + "]")
      log_debug("importFile> sheet[0].name=[" + sheet.getName() + "]")
    
      const origin = IMPORT_ORIGINS.find(function (o) { return o.id === accountType })
      if ( !origin ) {
        log_error("Origine d'import inconnue [" + accountType + "]")
        throw new Error("Origine d'import inconnue : " + accountType)
      }
      origin.run(spreadsheet_imported)
      log_info("importation finie [" + spreadsheet_imported.getName() + "]")
    } else {
      log_warn("Rien a importer ou déjà en cours - stop")
      throw new Error("Rien à importer (fichier expiré ou import déjà en cours). Réessayez l'upload.")
    }
  } finally {
    flushLogs();
  }
}

/**
 * Convertit un montant (nombre ou texte) en float.
 * Gère "1 234,56", "1.234,56", "1,234.56", "12.5", "12,5", "-12,50 €".
 */
function parseAmount(v) {
  if (typeof v === "number") return v;
  let s = String(v).replace(/[^\d,.\-]/g, "");
  const lastComma = s.lastIndexOf(",");
  const lastDot = s.lastIndexOf(".");
  if (lastComma > lastDot) {
    s = s.replace(/\./g, "").replace(",", ".");   // 1.234,56 -> 1234.56
  } else {
    s = s.replace(/,/g, "");                        // 1,234.56 -> 1234.56
  }
  return parseFloat(s);
}

function addAccountRow(account_rows,row_date,row_mode,row_tier,row_label,row_category,row_sub_catetory,row_amount,row_status) {
  if ( row_status == "" ) {
    row_status = "N"
  }
  if ( row_mode == "" ) {
    row_mode = "=sheetName()" // should copy paste value then
  }

  const row_amount_float = parseAmount(row_amount)

  account_rows.push( [ row_date,row_mode,row_tier,row_label,row_category,row_sub_catetory,row_amount_float,row_status ] )
  return account_rows
}

function importRowsToSheet(account_rows, targetSheetName, focus_after_insert, automatch_on_import) {
  var targetSheet = SpreadsheetApp.getActive().getSheetByName(targetSheetName);

  // 1. Une seule lecture
  const existingValues = targetSheet.getDataRange().getValues();
  var rulesValues = automatch_on_import ? getRulesValues() : null;

  // 2. Dédoublonnage en mémoire (multiset : date + montant en centimes)
  //    Deux opérations identiques le même jour restent possibles : on ne rejette
  //    une nouvelle ligne que si le même nombre existe déjà dans la feuille.
  const tz = Session.getScriptTimeZone();
  const keyOf = function (d, amount) {
    return Utilities.formatDate(new Date(d), tz, "yyyy-MM-dd") + "|" + Math.round(Number(amount) * 100);
  };

  const existingCount = {};
  for (let i = 1; i < existingValues.length; i++) {
    const row = existingValues[i];
    if (!(row[0] instanceof Date)) continue;
    const k = keyOf(row[0], row[6]);
    existingCount[k] = (existingCount[k] || 0) + 1;
  }

  const newRows = account_rows.filter(new_row => {
    const k = keyOf(new_row[0], new_row[6]);
    if (existingCount[k] > 0) {
      existingCount[k]--;
      log_debug("ignore ligne dupliquée [" + new_row + "] pour [" + targetSheetName + "]");
      return false;
    }
    return true;
  });

  if (newRows.length === 0) {
    log_debug("Pas de nouvelles lignes pour [" + targetSheetName + "]");
    return;
  }

  // 3. Insertion batch — 1 seul appel API
  const firstNewRowNumber = targetSheet.getLastRow() + 1;
  targetSheet
    .getRange(firstNewRowNumber, 1, newRows.length, newRows[0].length)
    .setValues(newRows);

  log_debug("[" + newRows.length + "] lignes ajoutées à [" + targetSheetName + "]");

  // 4. Automatch batch après insertion
  if (automatch_on_import) {
    // Relire la feuille complète (inclut les nouvelles lignes)
    var allValues = targetSheet.getDataRange().getValues();
    var batchBuffer = [];

    for (let i = firstNewRowNumber - 1; i < allValues.length; i++) {
      const ruleIdx = rowMatchRule(rulesValues, allValues[i]);
      if (ruleIdx > 0) {
        assignRuleToRow(null, i + 1, rulesValues[ruleIdx], batchBuffer);
      }
    }

    // 1 seule écriture pour toutes les règles appliquées
    flushRuleBatch(targetSheet, batchBuffer, allValues);
    log_debug("[" + batchBuffer.length + "] lignes catégorisées automatiquement dans [" + targetSheetName + "]");
  }

  // 5. Un seul tri après tout
  targetSheet.sort(1, false);
  log_debug("trie par date desc [" + targetSheetName + "]");

  if (focus_after_insert) {
    SpreadsheetApp.getActiveSpreadsheet().setActiveSheet(targetSheet);
    log_debug("focus on [" + targetSheet.getName() + "]");
  }
}
