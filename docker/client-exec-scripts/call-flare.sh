#/bin/bash 

FLARE_BASE_URL=${FLARE_BASE_URL:-"http://flare:5000"}

QUERY_INPUT=`cat`

RESP=$(curl --location -s --request POST "$FLARE_BASE_URL/query-sync" \
--header 'Content-Type: codex/json' \
--header 'Accept: internal/json' \
--header 'Cookie: JSESSIONID=node0v3dnl2dqawhlbymawm3cl7ib22.node0' \
--data-raw "$QUERY_INPUT")

echo $RESP