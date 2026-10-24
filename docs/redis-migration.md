# Redis migration

Moves Redis data from the old VPS to the new k8s cluster through an RDB snapshot. Redis runs as `redis-master-0` in the `databases` namespace, the password is in the `rediscreds` secret.

```sh
redis_exec() {
  kubectl exec -n databases redis-master-0 -- redis-cli -a "$(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' | base64 -d)" "$@"
}
```

## 1. Create the RDB backup on the VPS

In `redis-cli`:

```
BGSAVE
LASTSAVE          # changes when the save is complete
CONFIG GET dir
CONFIG GET dbfilename
```

## 2. Copy the file

`scp` the RDB file from the VPS to your machine, as `./redis-backup.rdb`.

## 3. Load it into the new cluster

Check the data directory and file name in the new Redis:

```sh
redis_exec CONFIG GET dir
redis_exec CONFIG GET dbfilename
```

Stop Redis without saving (otherwise it overwrites the file on shutdown):

```sh
redis_exec SHUTDOWN NOSAVE
```

Copy the file, **using the location and name returned above** (`/data/dump.rdb` here), fix ownership and restart the pod:

```sh
kubectl cp ./redis-backup.rdb databases/redis-master-0:/data/dump.rdb
kubectl exec -n databases redis-master-0 -- chown 1001:1001 /data/dump.rdb
kubectl delete pod redis-master-0 -n databases
```

## 4. Verify

```sh
kubectl get pods -n databases -w    # wait for ready
redis_exec DBSIZE
redis_exec KEYS "*" | head -10
```
