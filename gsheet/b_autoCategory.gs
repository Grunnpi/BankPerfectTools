const IDX_RULE_MOT_CLEF                 = 0
const IDX_RULE_TIER                     = 1
const IDX_RULE_LOGIQUE_MOT_CLEF         = 2
const IDX_RULE_MOT_CLEF_CASE_SENSITIVE  = 3
const IDX_RULE_LOGIQUE_TIER             = 4
const IDX_RULE_STATUS                   = 5
const IDX_RULE_CATEGORIE_ID             = 6
const IDX_RULE_CATEGORIE_VAL            = 7
const IDX_RULE_CATEGORIE_SOUS_VAL       = 8

const MOT_CLEF_CASE_SENSITIVE           = "sensitive" // else "insensitive" = default

function getRulesValues() {
  var rulesSheet = SpreadsheetApp.getActive().getSheetByName("Rules")
  var rulesValues = rulesSheet.getDataRange().getDisplayValues();
  return rulesValues
}

function rowMatchRule(rulesValues, the_row) {
  var the_rule_row_number = -1
  for (var i=1;i<rulesValues.length;i++){
    if ( isRowMatchRule(the_row,rulesValues[i]) )  {
      the_rule_row_number = i
      break
    }
  }
  return the_rule_row_number
}

/*
  Rules matcher :
  - label contains rule flag
  - if rule flag contains "+", label contains all "+" separated values
*/
function isRowMatchRule(the_row, the_rule) {
  var row_label = the_row[3].toString()
  var returnValue = false

  if ( the_rule[IDX_RULE_STATUS] == "active" ) {
    if ( the_rule[IDX_RULE_LOGIQUE_MOT_CLEF] == "libellé") {
      if ( the_rule[IDX_RULE_MOT_CLEF_CASE_SENSITIVE] == MOT_CLEF_CASE_SENSITIVE ) {
        if ( row_label.indexOf(the_rule[IDX_RULE_MOT_CLEF]) >= 0 )  {
          returnValue = true
        }
      } else {
        if ( row_label.toUpperCase().indexOf(the_rule[IDX_RULE_MOT_CLEF].toUpperCase()) >= 0 )  {
          returnValue = true
        }
      }
    }
    else if ( the_rule[IDX_RULE_LOGIQUE_MOT_CLEF] == "et+") {
      var all_labels = the_rule[IDX_RULE_MOT_CLEF].split("+")
      var match_count = 0
      for (var i=0;i<all_labels.length;i++){
        if ( the_rule[IDX_RULE_MOT_CLEF_CASE_SENSITIVE] == MOT_CLEF_CASE_SENSITIVE ) {
          if (row_label.indexOf(all_labels[i]) >= 0) {
            match_count++
          } 
        }
        else {
          if (row_label.toUpperCase().indexOf(all_labels[i].toUpperCase()) >= 0) {
            match_count++
          } 
        }
      }
      returnValue = ( match_count === all_labels.length)
    }
    else if ( the_rule[IDX_RULE_LOGIQUE_MOT_CLEF] == "ou+") {
      var all_labels = the_rule[IDX_RULE_MOT_CLEF].split("+")
      for (var i=0;i<all_labels.length;i++){
        if ( the_rule[IDX_RULE_MOT_CLEF_CASE_SENSITIVE] == MOT_CLEF_CASE_SENSITIVE ) {
          if (row_label.indexOf(all_labels[i]) >= 0) {
            returnValue = true
            break
          } 
        } else {
          if (row_label.toUpperCase().indexOf(all_labels[i].toUpperCase()) >= 0) {
            returnValue = true
            break
          } 
        }
      }
    } else {
      log_error("(" + row_label + ") ? Rules[" + the_rule[IDX_RULE_MOT_CLEF] + "] type[" + the_rule[IDX_RULE_LOGIQUE_MOT_CLEF] + "] unknown")
    }
  }
  return returnValue
}


function assignRuleToRow(currentSheet, row_line, the_rule, batchBuffer) {
  // Mode batch : on stocke dans le buffer sans toucher l'API
  if (batchBuffer) {
    batchBuffer.push({ row_line, the_rule });
    return;
  }
  // Mode direct (fallback) : comportement original
  _applyRuleToSheet(currentSheet, row_line, the_rule);
}

function _applyRuleToSheet(currentSheet, row_line, the_rule) {
  if (the_rule[IDX_RULE_LOGIQUE_TIER] !== "fait_rien") {
    if (the_rule[IDX_RULE_LOGIQUE_TIER] === "si_vide") {
      const prev = currentSheet.getRange("C" + row_line).getValue();
      if (prev === "") currentSheet.getRange("C" + row_line).setValue(the_rule[IDX_RULE_TIER]);
    } else {
      currentSheet.getRange("C" + row_line).setValue(the_rule[IDX_RULE_TIER]);
    }
  }
  currentSheet.getRange("E" + row_line).setValue(the_rule[IDX_RULE_CATEGORIE_VAL]);
  currentSheet.getRange("F" + row_line).setValue(the_rule[IDX_RULE_CATEGORIE_SOUS_VAL]);
  currentSheet.getRange("H" + row_line).setValue("-");
}

/**
 * Vide le buffer batch en un minimum d'appels API
 */
function flushRuleBatch(currentSheet, batchBuffer, allSheetValues) {
  if (batchBuffer.length === 0) return;

  // On travaille sur une copie du tableau 2D existant
  // allSheetValues : résultat de getDataRange().getValues() (0-based)
  batchBuffer.forEach(({ row_line, the_rule }) => {
    const idx = row_line - 1; // convertit en index 0-based

    // Colonne C (index 2) : tier
    if (the_rule[IDX_RULE_LOGIQUE_TIER] === "fait_rien") {
      // rien
    } else if (the_rule[IDX_RULE_LOGIQUE_TIER] === "si_vide") {
      if (allSheetValues[idx][2] === "") {
        allSheetValues[idx][2] = the_rule[IDX_RULE_TIER];
      }
    } else {
      allSheetValues[idx][2] = the_rule[IDX_RULE_TIER];
    }

    allSheetValues[idx][4] = the_rule[IDX_RULE_CATEGORIE_VAL];     // E
    allSheetValues[idx][5] = the_rule[IDX_RULE_CATEGORIE_SOUS_VAL]; // F
    allSheetValues[idx][7] = "-";                                    // H
  });

  // Un seul setValues pour tout réécrire
  currentSheet
    .getRange(1, 1, allSheetValues.length, allSheetValues[0].length)
    .setValues(allSheetValues);
}

// ─── autoCategory optimisée ────────────────────────────────────────────────

function autoCategory() {
  log_debug("AutoCategory - début batch");

  var currentSheet = SpreadsheetApp.getActiveSheet();

  // 1 lecture
  var allValues = currentSheet.getDataRange().getValues();
  var rulesValues = getRulesValues();

  var batchBuffer = [];
  var nbAutoMatchLines = 0;

  for (let i = 1; i < allValues.length; i++) {
    const row = allValues[i];
    if (row[7] !== "N") continue;

    for (let r = 1; r < rulesValues.length; r++) {
      if (isRowMatchRule(row, rulesValues[r])) {
        assignRuleToRow(null, i + 1, rulesValues[r], batchBuffer);
        nbAutoMatchLines++;
        break;
      }
    }
  }

  log_debug(" Lignes matchées [" + nbAutoMatchLines + "]");

  // 1 écriture pour tout
  flushRuleBatch(currentSheet, batchBuffer, allValues);

  log_debug(" AutoCategory terminée");
}