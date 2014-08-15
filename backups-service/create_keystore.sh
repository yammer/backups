#!/bin/bash

if [ $# -ne 4 ]; then
    echo "Usage: $0 <certificate> <key> <alias> <output file>"
    exit 1
fi

ALIAS=${3}
IN_CRT=${1}
IN_KEY=${2}
OUTPUT_FILE=${4}

TMP_FILE="keystore.p12.tmp"

openssl pkcs12 -export -name ${ALIAS} -in ${IN_CRT} -inkey ${IN_KEY} -out ${TMP_FILE}
keytool -importkeystore -trustcacerts -destkeystore ${OUTPUT_FILE} -srckeystore ${TMP_FILE} -srcstoretype pkcs12 -alias ${ALIAS}
rm ${TMP_FILE}
