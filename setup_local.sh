#!/usr/bin/env bash

aws dynamodb create-table \
--table-name team55-sample-table \
--endpoint-url http://0.0.0.0:8000 \
--output json \
--attribute-definitions AttributeName=par,AttributeType=S AttributeName=num,AttributeType=N \
--key-schema AttributeName=par,KeyType=HASH AttributeName=num,KeyType=RANGE \
--provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10

aws dynamodb create-table \
--table-name team55-sample-snapshot-table \
--endpoint-url http://0.0.0.0:8000 \
--output json \
--attribute-definitions AttributeName=par,AttributeType=S AttributeName=seq,AttributeType=N AttributeName=ts,AttributeType=N \
--key-schema AttributeName=par,KeyType=HASH AttributeName=seq,KeyType=RANGE \
--provisioned-throughput ReadCapacityUnits=1,WriteCapacityUnits=1 \
--local-secondary-indexes 'IndexName=ts-idx,KeySchema=[{AttributeName=par,KeyType=HASH},{AttributeName=ts,KeyType=RANGE}],Projection={ProjectionType=KEYS_ONLY}'

aws --no-verify-ssl --endpoint-url https://0.0.0.0:4567 --output json kinesis create-stream --stream-name team55-test-stream --shard-count 1
