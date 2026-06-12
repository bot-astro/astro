#!/usr/bin/env bash

input=$1

shard_id=$(( (input >> 22) % 176 ))
shards_per_pod=$((176 / 11))

echo $(( shard_id / shards_per_pod ))
