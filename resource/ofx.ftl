OFXHEADER:100
DATA:OFXSGML
VERSION:151
SECURITY:NONE
ENCODING:USASCII
CHARSET:1252
COMPRESSION:NONE
OLDFILEUID:NONE
NEWFILEUID:NONE

<OFX>
<SIGNONMSGSRSV2>
	<SONRS>
		<DTSERVER>20181129000000
		<STATUS>
			<CODE>0
			<SEVERITY>INFO
		</STATUS>
		<LANGUAGE>FR
	</SONRS>
</SIGNONMSGSRSV2>
<BANKMSGSRSV2>
    <#list myData as keyBP, myStatements>
	<STMTTRNRS>
		<TRNUID>20181129000000
		<STATUS>
			<CODE>0
			<SEVERITY>INFO
		</STATUS>
		<STMTRS>
			<CURDEF>EUR
			<BANKACCTFROM>
				<BANKID>${keyBP?split("#")[0]}
				<BRANCHID>${keyBP?split("#")[1]}
				<ACCTID>${keyBP?split("#")[2]}
				<ACCTTYPE2>CHECKING
			</BANKACCTFROM>
			<BANKTRANLIST>
				<DTSTART>20190119000000
				<DTEND>20190119000000
                <#list myStatements as oneStatements>
				<STMTTRN>
					<TRNTYPE>DEBIT
					<#assign statementDate = oneStatements.getStatementDate()>
					<DTPOSTED>${statementDate?replace("-", "")}
					<DTUSER>${statementDate?replace("-", "")}
					<TRNAMT>${oneStatements.getAmount()?string("##0.00")}
					<FITID>L9JVJ28%CF
					<NAME>${oneStatements.getTier()!""}
					<MEMO>${oneStatements.getDescription()}
				</STMTTRN>
                </#list>
			</BANKTRANLIST>
		</STMTRS>
	</STMTTRNRS>
    </#list>
</BANKMSGSRSV2>
</OFX>
