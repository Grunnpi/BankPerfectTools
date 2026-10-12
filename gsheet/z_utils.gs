function sheetName() {
  console.log("sheetname appelé dans [" + SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getName() + "]")
  return SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getName();
}

function log(type, message) {
  var sheetLogs = SpreadsheetApp.getActive().getSheetByName("Logs"); 

  var currentDate = new Date();

  sheetLogs.appendRow([currentDate, type, message])
}

function clearLogs() {
  var sheetLogs = SpreadsheetApp.getActive().getSheetByName("Logs"); 
  sheetLogs.getRange("A2:E").clearContent()
}

function log_debug(message) {
  log("DEBUG",message)
}
function log_info(message) {
  log("INFO",message)
}
function log_warn(message) {
  log("WARN",message)
}
function log_error(message) {
  log("ERROR",message)
}

//Displays an alert as a Toast message
function displayToastAlert(message) {
  SpreadsheetApp.getActive().toast(message, "⚠️ GrunnAlert", 5); 
}

