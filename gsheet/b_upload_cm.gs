function importCreditMutuel(spreadsheet) {

  var allCMSheet = spreadsheet.getSheets()
  log_debug("importCreditMutuel [" + allCMSheet.length + " sheets]")

  var accountSheet = SpreadsheetApp.getActive().getSheetByName("Comptes")
  var accountDefinition = accountSheet.getDataRange().getDisplayValues();

  var importDetail = true

  if (importDetail) {
    for (idx_sheet=1;idx_sheet<allCMSheet.length;idx_sheet++){
      // sheet name
      var sheet = allCMSheet[idx_sheet]
      var sheetName = sheet.getName()
      var targetSheetName = "Import"
      
      // search for account  
      var foundIt = false
      var skipImport = false
      for (i=1;i<accountDefinition.length;i++){
        if ( accountDefinition[i][2] == "Crédit Mutuel" ) { // CM & actif
          var sheetNameToSearch = "Cpt " + accountDefinition[i][8] + " " + accountDefinition[i][9]
          if ( sheetNameToSearch == sheetName ) {
            targetSheetName = accountDefinition[i][0]
            if ( accountDefinition[i][4] == "oui") {
              log_debug("Compte cible trouvé [" + targetSheetName + "] pour [" + sheetName + "]")
              foundIt = true
            } else {
              log_warn("Compte cible trouvé [" + targetSheetName + "] pour [" + sheetName + "] non actif dans setup ! skip")
              skipImport = true
              foundIt = true
            }
            break
          }
        } 
      }
      if ( !foundIt) {
        if ( sheetName == "hidden_data" ) {
          log_debug("Onglet [hidden_data] - skip")
          skipImport = true
        } else if ( sheetName == "hidden" ) {
          log_debug("Onglet [hidden] - skip")
          skipImport = true
        } else {
          targetSheetName = "Import"
          log_warn("Onglet non reconnue [" + sheetName + "] : on place les lignes dans [" + targetSheetName + "]")
        }
      } 
    
      if ( !skipImport ) {
        var data = sheet.getDataRange().getValues();
        var rowNumber = 0
        var tableIsOver = false

        var account_rows = []
        data.forEach(function (row) {
          rowNumber++
          if ( !tableIsOver ) {
            if (rowNumber >= 6 ) {
              if ( row[1] == "" ) {
                //log_debug("row ends here " + rowNumber + " = " + row)
                tableIsOver = true
              } else {
                var new_date = row[0]
                if ( new_date instanceof Date && new_date != undefined) {
                  new_date.setHours(0,0,0,0)
                }
                if ( row[3] != "") {
                  addAccountRow(account_rows,new_date,targetSheetName,"",row[2],"","",row[3],"")
                } else {
                  addAccountRow(account_rows,new_date,targetSheetName,"",row[2],"","",row[4],"")
                }
              }
            }
          }
        });

        if ( account_rows.length > 0) {
          log_debug("[" + account_rows.length + "] rows to import")
          importRowsToSheet(account_rows,targetSheetName,false,true)
        } 
      }
    }
  }


  /* ********************************************************************************************************* */
  // add summary lines for xcheck
  var sheetSummary = allCMSheet[0]
  var dataAccountSummary = sheetSummary.getDataRange().getValues();
  log_debug("Récupération des soldes des comptes dans [" + sheetSummary.getName() + "]")
  var targetSheet = SpreadsheetApp.getActive().getSheetByName("Comptes")
  
  var rowNumber = 0
  var tableIsOver = false
  var account_rows = []
  var dateAujourdhui = new Date();
  dataAccountSummary.forEach(function (row) {
    rowNumber++
    if ( !tableIsOver ) {
      if (rowNumber >= 5 ) {
        if ( row[1] == "" ) {
          log_debug("recherche de compte résumé ligne fin " + rowNumber + " = " + row)
          tableIsOver = true
        } else {
          //log_debug("Row [" + row[1] + "]")
          sheetName = row[1]

          // search account in 
          fountIt = false
          for (i=1;i<accountDefinition.length;i++){
            if ( accountDefinition[i][2] == "Crédit Mutuel" ) { // CM & actif
              var sheetNameToSearch = accountDefinition[i][7] + " " + accountDefinition[i][8] + " " + accountDefinition[i][9]
              if ( sheetNameToSearch == sheetName ) {
                if ( accountDefinition[i][4] == "oui") {
                  targetSheet.getRange("N" + (i + 1)).setValue(row[2]) // solde
                  targetSheet.getRange("Q" + (i + 1)).setValue(dateAujourdhui) // date de maj
                } else {
                  log_warn("Compte résumé [" + accountDefinition[i][0] + "] pour [" + sheetName + "] non actif dans setup ! skip")
                }
                fountIt = true
                break
              }
            } 
          }
          if ( !fountIt){
            log_warn("Pas de compte résumé pour [" + sheetName + "] WTF ?! ")
          }
        }
      }
    }
  })
  displayToastAlert("Fin import CM")
  log_info("Fin import CM")
}