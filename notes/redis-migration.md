1. Create RDB backup on your VPS  
first enter Redis cli  
then run:  
`BGSAVE`  
it may take a bit, check when it's completed with:  
`LASTSAVE`

check save location  
`CONFIG GET dir`  
check file name  
`CONFIG GET dbfilename`  

2. scp file to my macbook  
3. scp to new k8s cluster  
check rdb file location  
`kubectl exec -n databases redis-master-0 -- redis-cli -a $(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' | base64 -d) CONFIG GET dir`  
check rdb file name:  
`kubectl exec -n databases redis-master-0 -- redis-cli -a $(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' | base64 -d) CONFIG GET dbfilename`  
shutdown redis:   
`kubectl exec -n databases redis-master-0 -- redis-cli -a $(kubectl get secret rediscreds -n databases -o jsonpath='{.data.password}' | base64 -d) SHUTDOWN NOSAVE`  
copy rdb file:  
VERIFY FILE LOCATION AND NAME WITH THE TWO PREVIOUS COMMANDS  
`kubectl cp ./redis-backup.rdb databases/redis-master-0:/data/dump.rdb`  
set ownership:  
`kubectl exec -n databases redis-master-0 -- chown 1001:1001 /data/dump.rdb`  
restart redis pod
`kubectl delete pod redis-master-0 -n databases`

4. Check new data  
wait for pod to be ready:  
`kubectl get pods -n databases -w`  

set pwd:
`export $REDIS_PASSWORD=pwd`  

check data:  
`kubectl exec -n databases redis-master-0 -- redis-cli -a $REDIS_PASSWORD DBSIZE`  
`kubectl exec -n databases redis-master-0 -- redis-cli -a $REDIS_PASSWORD KEYS "*" | head -10`  
