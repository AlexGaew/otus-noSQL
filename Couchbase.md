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

 * добавляем пользователей.
```sql
 INSERT INTO test.app.users (KEY, VALUE) VALUES
("user::1", {"type":"user","id":1,"email":"ivan.petrov@mail.com","firstName":"Ivan","lastName":"Petrov","createdAt":NOW_STR()}),
("user::2", {"type":"user","id":2,"email":"anna.sidorova@mail.com","firstName":"Anna","lastName":"Sidorova","createdAt":NOW_STR()}),
("user::3", {"type":"user","id":3,"email":"alex.ivanov@mail.com","firstName":"Alex","lastName":"Ivanov","createdAt":NOW_STR()}),
("user::4", {"type":"user","id":4,"email":"maria.kuznetsova@mail.com","firstName":"Maria","lastName":"Kuznetsova","createdAt":NOW_STR()}),
("user::5", {"type":"user","id":5,"email":"sergey.orlov@mail.com","firstName":"Sergey","lastName":"Orlov","createdAt":NOW_STR()});
```
*  добавляем продукты
```sql
INSERT INTO test.app.products (KEY, VALUE) VALUES
("product::1", {"type":"product","id":1,"name":"Coaching Session 60 min","price":100.00,"stockQty":50}),
("product::2", {"type":"product","id":2,"name":"Online Course: Leadership","price":299.00,"stockQty":100}),
("product::3", {"type":"product","id":3,"name":"Personal Development Book","price":25.50,"stockQty":200}),
("product::4", {"type":"product","id":4,"name":"Team Workshop","price":800.00,"stockQty":10}),
("product::5", {"type":"product","id":5,"name":"Mindfulness Webinar","price":49.99,"stockQty":150}),
("product::6", {"type":"product","id":6,"name":"Career Strategy Session","price":150.00,"stockQty":40});
```
*  добавляем заказы
```sql
INSERT INTO test.app.orders (KEY, VALUE) VALUES
("order::1", {
  "type":"order",
  "id":1,
  "userId":1,
  "status":"NEW",
  "totalAmount":125.50,
  "createdAt":NOW_STR(),
  "items":[
    {"productId":1,"quantity":1,"price":100.00},
    {"productId":3,"quantity":1,"price":25.50}
  ]
}),
("order::2", {
  "type":"order",
  "id":2,
  "userId":2,
  "status":"PAID",
  "totalAmount":299.00,
  "createdAt":NOW_STR(),
  "items":[
    {"productId":2,"quantity":1,"price":299.00}
  ]
}),
("order::3", {
  "type":"order",
  "id":3,
  "userId":3,
  "status":"PAID",
  "totalAmount":199.99,
  "createdAt":NOW_STR(),
  "items":[
    {"productId":6,"quantity":1,"price":150.00},
    {"productId":5,"quantity":1,"price":49.99}
  ]
});
```
*  далее при помощи селектов мы можем выбирать, допустим, пользователей.
```sql
SELECT META().id, u.*
FROM test.app.users AS u
WHERE u.type = "user"
LIMIT 2;

SELECT 
    u.*,
    o.id AS orderId,
    o.status,
    o.totalAmount,
    o.items
FROM test.app.users AS u
JOIN test.app.orders AS o
    ON o.userId = u.id
WHERE u.type = "user"
  AND o.type = "order"
  AND u.id = 1;
```
<img width="1174" height="962" alt="image" src="https://github.com/user-attachments/assets/f7e826f1-2df3-40e8-abae-ffd8e9c5087d" />

### начинаем проверять отказоустойчивость
* отключаем одну ноду, проверяем статус
<img width="1784" height="354" alt="image" src="https://github.com/user-attachments/assets/8e83fe63-1846-4502-9841-0f76edec304d" />

*  проверяем прохождение запросов

<img width="1600" height="1264" alt="image" src="https://github.com/user-attachments/assets/33ed17fd-6ea7-4f55-aebb-48120f70619b" />

*  при отключенной одной ноде запросы продолжают выполняться.
*   теперь отключены две ноды. 
  <img width="3332" height="572" alt="image" src="https://github.com/user-attachments/assets/29510dd2-869e-491d-a944-ac7545c2d634" />
  
* Запросы не проходят, потому что нет majority quorum. Без кворума кластер не может поддерживать консистентную конфигурацию (cluster map), поэтому операции блокируются.
* запустил все три ноды. Ситуация следующая:
* Кластер работает за счёт 2 healthy-нод (есть кворум). Третья нода остаётся inactiveFailed — она исключена из vBucket map и данные на неё не перераспределены. Кластер не полностью восстановлен. Нужно rebalance. 
*  после проведения рибаланса, нода, которая была не активна, была удалена. Осталось две рабочие ноды.
*  При помощи команды server-add добавил еще одну третью ноду, перепровел револанс, все заработало как и было.
TL;DR: Кластер Couchbase корректно продемонстрировал работу majority quorum и механизма failover. При потере 1 ноды кластер остаётся доступным, при потере 2 из 3 — недоступен из-за отсутствия кворума. Rebalance после failover удаляет failed-ноду из topology. Данные сохраняются при наличии replica.
⸻
Краткий итог исследования

1️⃣ Развёртывание кластера (3 ноды)
Кластер успешно собран, bucket и коллекции созданы, primary index настроены. CRUD-запросы работают.

2️⃣ Отказ 1 ноды
	•	Кворум сохранён (2 из 3).
	•	Запросы продолжают выполняться.
	•	Replica обеспечивает доступность данных.

3️⃣ Отказ 2 нод
	•	Потерян majority quorum.
	•	Кластер блокирует операции (защита от split-brain).
	•	Это штатное поведение.

4️⃣ Восстановление нод
	•	При возврате 2 нод кворум восстановился.
	•	Одна нода перешла в inactiveFailed.
	•	Rebalance финализировал failover и удалил её из кластера.

5️⃣ Повторное добавление ноды
	•	Через server-add + rebalance topology восстановлена.
	•	Кластер снова полностью отказоустойчив (при replica ≥ 1).

⸻

Архитектурный вывод
	•	Couchbase использует majority quorum для metadata.
	•	Replica отвечает за сохранность данных.
	•	Failover + rebalance изменяют topology, а не «лечат» ноду.
	•	Для устойчивости к 2 падениям требуется минимум 5 нод и replica ≥ 2.

⸻

Общий вывод

Эксперимент подтвердил корректную работу:
	•	quorum-механизма
	•	failover
	•	rebalance
	•	механизма распределения vBucket

Поведение кластера соответствует архитектуре Couchbase и ожиданиям production-сценариев.



