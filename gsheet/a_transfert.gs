function createTransfert(type) {

  const ss = SpreadsheetApp.getActive();
  const sh = ss.getActiveSheet();
  let a = sh.getActiveRangeList().getRanges().map(r => sh.getName() + ";" + r.getRow() + ";" + r.getLastRow() );

  var cache = CacheService.getScriptCache();
  cache.put("selected_stuff",a)

  var html = HtmlService.createTemplateFromFile('dialog_transfert')
  html.data = a

  
  SpreadsheetApp.getUi() // Or DocumentApp or SlidesApp or FormApp.
      .showModalDialog(html.evaluate().setWidth(400).setHeight(200), 'Transfert');
}

function appendRowFromFormSubmit(compte_cible) {
  var cache = CacheService.getScriptCache();
  const selected_data = cache.get("selected_stuff")

  log_debug(compte_cible)
  log_debug(JSON.stringify(selected_data))

  // récupére les lignes du compte sources
  var liste_ranges = selected_data.split(",")
  account_rows = []
  var compte_source = ""
  for ( var idx_range=0;idx_range<liste_ranges.length;idx_range++) {
    var un_range_infos = liste_ranges[idx_range].split(";")
    log_debug("Onglet[" + un_range_infos[0] + "] lignes [" + un_range_infos[1] +"]à[" + un_range_infos[2] + "]")
    compte_source = un_range_infos[0]
    let range_A1notation = "A" + un_range_infos[1] + ":H" + un_range_infos[2]
    var rows = SpreadsheetApp.getActive().getSheetByName(un_range_infos[0]).getRange(range_A1notation).getValues()
    log_debug("rows[" + range_A1notation + "] " + rows.length)
    var index_row_range = parseInt(un_range_infos[1])
    rows.forEach(function(oneRow) {
      oneRow[8] = index_row_range++
      log_debug("Row index = " + oneRow[8])
      account_rows.push(oneRow)
    })
    
  }

  transfert_rows = []
  var sourceSheet = SpreadsheetApp.getActive().getSheetByName(compte_source)

  account_rows.forEach(function(oneRow){
    var label_transfert_source = sourceSheet.getRange("D" + (oneRow[8])).getValue()

    var mot_liaison_source = "vers "
    var mot_liaison_cible = "depuis "
    if (oneRow[6] > 0){
      mot_liaison_source = "depuis "
      mot_liaison_cible = "vers "
    }
    label_transfert_cible = "[transfert " + mot_liaison_cible +  compte_source + "] " + oneRow[3]
    label_transfert_source = "[transfert " + mot_liaison_source +  compte_cible + "] " + label_transfert_source

    sourceSheet.getRange("D" + (oneRow[8])).setValue(label_transfert_source)

    montant_inverse = oneRow[6] * -1
    addAccountRow(transfert_rows,oneRow[0],compte_cible,oneRow[2],label_transfert_cible,oneRow[4],oneRow[5],montant_inverse,oneRow[7])
  })
  log_debug("transfert_rows = " + transfert_rows.length)
  if ( transfert_rows.length > 0 ) {
    importRowsToSheet(transfert_rows,compte_cible,true)
  }


// var row = [select_choice];
// SpreadsheetApp.getActiveSheet().appendRow(row);
}