### 1. Домашнее задание Кластер Couchbase
## 1.2 Развертывание кластера Couchbase
Для развертывания кластера Couchbase сначала необходимо поднять Couchbase в Docker используя docker-compose файл, тип файла может меняться но примерно муть сохраняется...
``` yaml 
version: '3.9'
services:
  cb-1:
    image: couchbase:latest
    container_name: cb-1
    hostname: cb-1
    domainname: otus.local
    restart: unless-stopped
    ports: 
      - '8091-8096:8091-8096'
      - '11210:11210'
    environment:
      COUCHBASE_ADMIN_USERNAME: "Administrator"
      COUCHBASE_ADMIN_PASSWORD: "password"
    networks:
      cbnet:
        aliases: [cb-1.otus.local]
  
  cb-2:
    image: couchbase:latest
    container_name: cb-2
    hostname: cb-2
    domainname: otus.local
    restart: unless-stopped
    environment:
      COUCHBASE_ADMIN_USERNAME: "Administrator"
      COUCHBASE_ADMIN_PASSWORD: "password"
    networks:
      cbnet:
        aliases: [cb-2.otus.local]

  cb-3:
    image: couchbase:latest
    container_name: cb-3
    hostname: cb-3
    domainname: otus.local
    restart: unless-stopped
    environment:
      COUCHBASE_ADMIN_USERNAME: "Administrator"
      COUCHBASE_ADMIN_PASSWORD: "password"
    networks:
      cbnet:
        aliases: [cb-3.otus.local]

networks:
  cbnet:
    driver: bridge
```
используя команду docker ``` compose exec couchbase1 bash -lc '  ``` - подключаемся к bash couchbase1 и начинаем настраивать кластер 
(также кластер можно настроить и через  UI Couchbase port 8094)
используя команду 
```bash 
 % docker compose exec couchbase1 bash -lc '
/opt/couchbase/bin/couchbase-cli cluster-init -c couchbase1 \
--cluster-username Administrator \
--cluster-password password \
--services data,index,query \
--cluster-ramsize 1024 \
--cluster-index-ramsize 256 \
--index-storage-setting default
```
настраивам Перввичный кластер *(важно команды вводьть в деритории где при развертывании  couchbase создал свои Ноды)*
после правильной настройки кластера появляется окно "админки" ---> 

<img width="486" height="346" alt="image" src="https://github.com/user-attachments/assets/b60fec91-b77a-4e79-8210-fbeafee5fc9e" />

вводим наши учетные данные и попадаем в окно со множеством вкладок, где мы можем более детально и точно настроить кластер и добавить кластеры а также писать запросы...

<img width="2024" height="599" alt="image" src="https://github.com/user-attachments/assets/d185b985-8148-4e5f-88ec-6a27255a72e2" />

 далее необходимо добавить дополнительные сервера при помощи команды: 
 ```bash
docker compose exec cb-1 bash -lc '
/opt/couchbase/bin/couchbase-cli server-add -c cb-1.otus.local -u Administrator -p password \
--server-add=cb-3.otus.local --server-add-username Administrator --server-add-password password \
--services data,index
'
```
<img width="3594" height="1056" alt="image" src="https://github.com/user-attachments/assets/bc0bbb0f-d664-4051-8e26-64c9b5101182" />

 * Теперь, когда сервера добавлены, необходимо провести ребалансировку.
   
 ```bash
docker compose exec cb-1 bash -lc '
quote> /opt/couchbase/bin/couchbase-cli rebalance -c cb-1.otus.local -u Administrator -p password
quote> '
```
<img width="3594" height="1056" alt="image" src="https://github.com/user-attachments/assets/ca9132e1-2e12-4068-bb8a-f5a0375ce524" />
<img width="2548" height="528" alt="image" src="https://github.com/user-attachments/assets/396b0c4b-a224-418f-a797-d6dd713a4c56" />

  * далее добавляем и настраиваем бакеты.
  
<img width="1110" height="1942" alt="image" src="https://github.com/user-attachments/assets/2bc48be2-2e40-499e-a377-ef135bfbd2a1" />

 * добавляем свой скоуп, добавляем свои коллекции: Заказы, продукты и пользователи.
 
<img width="3482" height="1150" alt="image" src="https://github.com/user-attachments/assets/d1a91c1f-42bd-4d69-99f8-d9fa9107206e" />

*  для того, чтобы начать писать запросы, необходимо для начала создать первичный primary index  в разделе query для каждой таблицы users, orders, products.
  ```sql
CREATE PRIMARY INDEX ON test.app.users;
CREATE PRIMARY INDEX ON test.app.orders;
CREATE PRIMARY INDEX ON test.app.products;
```
<img width="3436" height="736" alt="image" src="https://github.com/user-attachments/assets/6b67eee4-c75c-4716-bfac-814942d8ad46" />









 






















