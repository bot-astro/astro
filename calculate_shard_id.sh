#!/usr/bin/env bash

input=$1

shard_id=$(( (input >> 22) % 160 ))
shards_per_pod=$((160 / 10))

echo $(( shard_id / shards_per_pod ))