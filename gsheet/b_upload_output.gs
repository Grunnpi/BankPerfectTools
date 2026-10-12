function importOUTPUT(sheet) {
  log_debug("importOUTPUT[" + sheet.getName() + "]")
  var data = sheet.getDataRange().getValues();
  var rowNumber = 0
  var tableIsOver = false

  var account_rows = []

  var row_bank = ""
  var row_branch = ""
  var row_account = ""

  var accountSheet = SpreadsheetApp.getActive().getSheetByName("Comptes")
  var accountDefinition = accountSheet.getDataRange().getDisplayValues();

  data.forEach(function (row) {
    rowNumber++
    if ( !tableIsOver ) {
      if (rowNumber > 1 ) {
        if ( row[1] == "" ) {
          tableIsOver = true
        } else {
          //log_debug("row " + rowNumber + " = " + row + " (with row[0]=[" + row[4] + "])")
          var new_date = row[4]
          new_date.setHours(0,0,0,0)

          row_bank    = row[1]
          row_branch  = row[2]
          row_account = row[3]

          // search for account
          var foundIt = false
          var targetSheetName = ""
          for (i=1;i<accountDefinition.length;i++){
            if (
                accountDefinition[i][7] == row_bank &&
                accountDefinition[i][8] == row_branch &&
                accountDefinition[i][9] == row_account 
            ) {
              //log_debug("Found targeting account [" + accountDefinition[i][0] + "]")
              targetSheetName = accountDefinition[i][0]
              foundIt = true
              break
            }
          }
          if ( !foundIt) {
            targetSheetName = "Import"
            log_debug("Account not found : don't know what to do with those lines !!! [" + row_bank + "|" + row_branch + "|" + row_account + "]")
          }

          // create array in array
          if ( account_rows[targetSheetName] === undefined ) {
            log_debug("First time account : create entry for " + targetSheetName)
            account_rows[targetSheetName] = []
          }

          var new_amount = row[7]
          if ( new_amount instanceof String && new_amount.indexOf(".") >= 0 ) {
            new_amount = new_amount.replace(".", ",")
          }

          addAccountRow(account_rows[targetSheetName],new_date,targetSheetName,row[5],row[6],"","",new_amount,"")
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