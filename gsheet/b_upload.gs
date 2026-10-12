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
    let folder = DriveApp.getFolderById("1Qn8LXy6b12xIslJGoFd7DbTfovWPipVp");
    const csvFile = folder.createFile(blob); 

    const today = new Date();
    const formattedDate = Utilities.formatDate(today, Session.getScriptTimeZone(), "yyyy-MM-dd_HH-mm-ss");

    const sheetName = formattedDate + "-importedSheetFrom-" + fileName

    let imported_file_id = "??"

    if ( fileName.endsWith(".csv")) {
      log_info("csv : on tente un cast supplémentaire du fichier [" + fileName + "]")

      // Spécifiez le séparateur (par exemple, ";" pour des CSV français)
      //const separator = ",";

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


      // Découper le fichier en lignes
      const rows = csvContent.split("\n").map(line => line.split(separator));
      // Créer un fichier Google Sheet
      const spreadsheet = SpreadsheetApp.create(sheetName);
      const sheet = spreadsheet.getActiveSheet();
      sheet.setName("ConversionTabCSV")
      

      // Supprimer les guillemets des colonnes de type chaîne de caractères
      const cleanedRows = rows.map(row => row.map(cell => cell.replace(/^"|"$/g, '')));
      
      cleanedRows.forEach((row, index) => {
        sheet.getRange(index + 1, 1, 1, row.length).setValues([row]);
      });

      // Déplacer le fichier dans le dossier cible
      const folder = DriveApp.getFolderById("1Qn8LXy6b12xIslJGoFd7DbTfovWPipVp"); // Accéder au dossier
      const file = DriveApp.getFileById(spreadsheet.getId()); // Obtenir le fichier créé
      const newFile = folder.addFile(file); // Ajouter le fichier au dossier
      DriveApp.getRootFolder().removeFile(file); // Supprimer le fichier du dossier racine

      imported_file_id = spreadsheet.getId()
      log_info("conversion par code et move du fichier [" + sheetName + "]")

      // Supprimer le fichier CSV original (optionnel)
      // csvFile.setTrashed(true);
    } else {
      log_info("import et conversion auto du fichier [" + sheetName + "]")

      // Enable the advanced Drive Service to use this function that converts the excel  file to a Google Sheets file
      let imported_file = Drive.Files.insert({title:sheetName, mimeType: MimeType.GOOGLE_SHEETS, parents: [{ id: "1Qn8LXy6b12xIslJGoFd7DbTfovWPipVp" }] }, blob, {convert: true, supportsAllDrives: true});
      imported_file_id = imported_file.id
    }

    var cache = CacheService.getScriptCache();
    cache.put("gsheet_imported_id",imported_file_id)
    cache.put("accountType",accountType)
    
    log_info("importe & convertie [" + fileName + "]")

    return "Success";
  }catch(ex)
  {
    Logger.log(ex);
    throw ex;
  }
}

function displayChoice(choice) {
  displayToastAlert("Importation ok !")
  return;
}

function importFile() {
  var cache = CacheService.getScriptCache();
  const imported_id = cache.get("gsheet_imported_id")
  const accountType = cache.get("accountType")

  if ( imported_id != "" && accountType != "" ) {
    cache.put("gsheet_imported_id","")
    cache.put("accountType","")

    var spreadsheet_imported = SpreadsheetApp.openById(imported_id)
    var sheet = spreadsheet_imported.getSheets()[0]
    //displayToastAlert("Sheet cible [" + sheet.getName() + "]")
    log_debug("importFile> sheet[0].name=[" + sheet.getName() + "]")
  
    switch (accountType) {
      case "CreditMutuel":
        importCreditMutuel(spreadsheet_imported) // multiple tab = multiple account
        break
      case "BIL":
        importBIL(spreadsheet_imported.getSheets()[0]) // 1 tab for 1 account
        break
      case "BOURSORAMA":
        importBOURSORAMA(spreadsheet_imported.getSheets()[0]) // 1 tab for 1 account
        break
      case "output":
        importOUTPUT(spreadsheet_imported.getSheets()[0]) // 1 tab for multiple account
        break
      default:
        break;
    }
    log_info("importation finie [" + spreadsheet_imported.getName() + "]")
  } else {
    log_warn("Rien a importer ou déjà en cours - stop")
  }
}


function addAccountRow(account_rows,row_date,row_mode,row_tier,row_label,row_category,row_sub_catetory,row_amount,row_status) {
  if ( row_status == "" ) {
    row_status = "N"
  }
  if ( row_mode == "" ) {
    row_mode = "=sheetName()" // should copy paste value then
  }

  if ( typeof(row_amount) == "string" && row_amount.indexOf(".") >= 0 ) {
    row_amount = row_amount.replace(".", ",")
  }
  if ( typeof(row_amount) == "string" && row_amount.indexOf(",") >= 0 ) {
    row_amount = row_amount.replace(",", ".")
  }
  row_amount_float = parseFloat(row_amount)

  account_rows.push( [ row_date,row_mode,row_tier,row_label,row_category,row_sub_catetory,row_amount_float,row_status ] )
  return account_rows
}

function importRowsToSheet(account_rows, targetSheetName, focus_after_insert, automatch_on_import) {
  var targetSheet = SpreadsheetApp.getActive().getSheetByName(targetSheetName);

  // 1. Une seule lecture
  const existingValues = targetSheet.getDataRange().getValues();
  var rulesValues = automatch_on_import ? getRulesValues() : null;

  // 2. Dédoublonnage en mémoire
  const newRows = account_rows.filter(new_row => {
    const duplicate = existingValues.some(row => {
      const row_date = new Date(row[0]);
      row_date.setHours(0, 0, 0, 0);
      const new_row_date = new_row[0];
      return (
        row_date.getFullYear() === new_row_date.getFullYear() &&
        row_date.getMonth()    === new_row_date.getMonth()    &&
        row_date.getDay()      === new_row_date.getDay()      &&
        row[6]                 === new_row[6]
      );
    });
    if (duplicate) log_debug("ignore ligne dupliquée [" + new_row + "] pour [" + targetSheetName + "]");
    return !duplicate;
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