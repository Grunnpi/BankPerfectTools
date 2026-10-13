function importOUTPUT(sheet) {
  log_debug("importOUTPUT[" + sheet.getName() + "]")
  var data = sheet.getDataRange().getValues();
  var rowNumber = 0
  var tableIsOver = false

  // clé = nom de l'onglet cible, valeur = tableau de lignes
  var account_rows = {}

  var accountSheet = SpreadsheetApp.getActive().getSheetByName("Comptes")
  var accountDefinition = accountSheet.getDataRange().getDisplayValues();

  // Index banque|agence|compte -> onglet (la première occurrence gagne, comme la recherche linéaire d'avant)
  // Normalisation numérique pour garder la tolérance de l'ancien "==" ("00123" == 123)
  var norm = function (v) {
    var s = String(v).trim()
    return (s !== "" && !isNaN(s)) ? String(Number(s)) : s
  }
  var accountIndex = {}
  for (let i=1;i<accountDefinition.length;i++){
    var k = norm(accountDefinition[i][7]) + "|" + norm(accountDefinition[i][8]) + "|" + norm(accountDefinition[i][9])
    if ( !(k in accountIndex) ) {
      accountIndex[k] = accountDefinition[i][0]
    }
  }

  data.forEach(function (row) {
    rowNumber++
    if ( !tableIsOver ) {
      if (rowNumber > 1 ) {
        if ( row[1] == "" ) {
          tableIsOver = true
        } else {
          //log_debug("row " + rowNumber + " = " + row + " (with row[0]=[" + row[4] + "])")
          var new_date = row[4]
          if ( !(new_date instanceof Date) ) {
            log_error("ligne ignorée ligne " + rowNumber + " : [" + new_date + "] pas une date : " + row)
            return
          }
          new_date.setHours(0,0,0,0)

          var row_bank    = row[1]
          var row_branch  = row[2]
          var row_account = row[3]

          // search for account
          var targetSheetName = accountIndex[norm(row_bank) + "|" + norm(row_branch) + "|" + norm(row_account)]
          if ( targetSheetName === undefined ) {
            targetSheetName = "Import"
            log_debug("Account not found : don't know what to do with those lines !!! [" + row_bank + "|" + row_branch + "|" + row_account + "]")
          }

          // create array in array
          if ( account_rows[targetSheetName] === undefined ) {
            log_debug("First time account : create entry for " + targetSheetName)
            account_rows[targetSheetName] = []
          }

          // le montant (nombre ou texte) est normalisé par addAccountRow -> parseAmount
          addAccountRow(account_rows[targetSheetName],new_date,targetSheetName,row[5],row[6],"","",row[7],"")
        }
      }
    }
  });

  for (var key in account_rows) {
    var value = account_rows[key];
    log_debug("[" + key + "] account " + value.length + " rows to import");
    importRowsToSheet(value,key,true)
  }
  log_info("Fin import output.csv");
}
