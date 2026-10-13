function sheetName() {
  console.log("sheetname appelé dans [" + SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getName() + "]")
  return SpreadsheetApp.getActiveSpreadsheet().getActiveSheet().getName();
}

// Mettre à true pour réactiver les logs DEBUG dans l'onglet "Logs"
const LOG_DEBUG_ENABLED = false;

// Buffer en mémoire : les logs sont écrits en 1 seul appel par flushLogs()
const _logBuffer = [];

function log(type, message) {
  if (type === "DEBUG" && !LOG_DEBUG_ENABLED) return;
  _logBuffer.push([new Date(), type, String(message)]);
}

/**
 * Écrit tous les logs bufferisés dans l'onglet "Logs" en un seul appel.
 * À appeler en fin de chaque point d'entrée (try/finally).
 */
function flushLogs() {
  if (_logBuffer.length === 0) return;
  var sheetLogs = SpreadsheetApp.getActive().getSheetByName("Logs");
  if (sheetLogs) {
    sheetLogs.getRange(sheetLogs.getLastRow() + 1, 1, _logBuffer.length, 3).setValues(_logBuffer);
  }
  _logBuffer.length = 0;
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
