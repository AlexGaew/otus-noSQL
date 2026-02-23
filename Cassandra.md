# Apache Cassandra — Общие сведения

## 1. Что такое Cassandra

Apache Cassandra — это распределённая NoSQL база данных с открытым исходным кодом. Она разработана для обработки больших объёмов данных на множестве серверов без единой точки отказа.

Cassandra относится к типу wide-column store (колоночные базы данных). В отличие от реляционных баз, здесь нет поддержки JOIN-операций и строгих ACID-транзакций. Вместо этого Cassandra жертвует консистентностью ради доступности и масштабируемости.

Проект был создан в 2008 году в компании Facebook инженерами Avinash Lakshman и Prashant Malik. В 2009 году Cassandra стала открытым Apache-проектом, а с 2010 года получила статус Top-Level проекта. Текущая стабильная версия — 5.0.

### Ключевые характеристики

- **Тип**: Wide-column store (колоночная)
- **Модель данных**: Denormalized tables с partition key
- **Консистентность**: Eventual с настраиваемым уровнем (tunable consistency)
- **Доступность**: Высокая, 99.99% uptime
- **Масштабируемость**: Горизонтальная, линейная
- **Архитектура**: Peer-to-peer, все узлы равны

### Основные концепции

**Распределённая архитектура**. Cassandra использует peer-to-peer модель без master/slave иерархии. Все узлы кластера равноправны. Узлы обмениваются метаданными через Gossip Protocol каждую секунду. Отказ любого узла не влияет на общую доступность системы.

**Модель данных**. Данные организуются в keyspace (аналог database в RDBMS), внутри которого находятся таблицы. Каждая таблица имеет partition key, который определяет распределение данных по узлам кластера, и clustering columns, которые определяют сортировку внутри партиции.

**Consistency Levels**. Cassandra позволяет настраивать уровень консистентности для каждого запроса отдельно. Основные уровни: ONE (достаточно одного узла), QUORUM (большинство узлов), LOCAL_QUORUM (большинство в локальном датацентре), ALL (все узлы). Для сильной консистентности работает формула: R + W > N, где N — фактор репликации.

**Репликация**. Данные автоматически реплицируются между узлами. Существуют две стратегии: SimpleStrategy (все реплики в одном датацентре, подходит для разработки) и NetworkTopologyStrategy (реплики распределены по датацентрам, для production).

### CAP-теорема

Согласно CAP-теореме, распределённая система может гарантировать только два из трёх свойств: Consistency (консистентность), Availability (доступность), Partition Tolerance (устойчивость к разделению). Cassandra выбирает AP — доступность и устойчивость к разделению. Это означает, что система остаётся доступной даже при потере связи между узлами, но данные могут быть не полностью согласованы в момент чтения.

---

## 2. Для чего использовать Cassandra

### Идеальные сценарии

**Высокая нагрузка на запись**. Cassandra оптимизирована для записи данных. Она использует append-only модель: данные сначала записываются в commit log для гарантии сохранности, затем в memtable (оперативная память), и периодически сбрасываются в SSTable на диск. Это делает Cassandra идеальным выбором для систем с интенсивной записью.

Примеры: IoT телеметрия, логирование событий, сбор метрик производительности, аудит действий пользователей.

**Time-series данные**. Благодаря модели данных с clustering columns, Cassandra эффективно хранит и запрашивает временные ряды. Данные внутри партиции автоматически сортируются, что позволяет быстро получать последние записи.

Примеры: финансовые транзакции, показания сенсоров, история изменений состояния, метрики мониторинга.

**Глобальная распределённость**. Cassandra поддерживает multi-region развертывания с активными узлами во всех датацентрах (active-active). Запросы могут обслуживаться локально, без обращения к удалённым узлам.

Примеры: международные сервисы, географически распределённые приложения, системы с требованиями к локализации данных.

**Требование к доступности**. Благодаря отсутствию единой точки отказа и автоматической репликации, Cassandra обеспечивает доступность на уровне 99.99%. Система продолжает работать даже при отказе нескольких узлов.

Примеры: платежные системы, онлайн-сервисы, критичные к простою приложения.

**Большие объёмы данных**. Cassandra масштабируется линейно: добавление новых узлов пропорционально увеличивает производительность. Кластеры могут хранить петабайты данных и обрабатывать миллиарды операций в день.

Примеры: большие данные, аналитические платформы, архивные хранилища.

### Когда НЕ стоит использовать Cassandra

**Сложные JOIN-операции**. Cassandra не поддерживает JOIN между таблицами. Если приложению требуется соединять данные из разных таблиц, придётся денормализовать данные или выполнять соединения на стороне приложения.

**ACID-транзакции**. Cassandra поддерживает только lightweight transactions (LWT) с ограниченной функциональностью. Если требуются полноценные транзакции с изоляцией и атомарностью, лучше выбрать реляционную базу данных.

**Агрегации по всему датасету**. Cassandra неэффективна для запросов, требующих агрегации данных across partitions (например, COUNT, SUM по всей таблице). Такие запросы требуют чтения всех партиций, что противоречит распределённой природе Cassandra.

**Частые обновления по ключу**. Cassandra оптимизирована для записи (append), а не для обновлений. Обновление записи фактически является вставкой новой версии с последующим удалением старой во время compaction.

**Малые объёмы данных**. Если данные помещаются на одном сервере и не требуют горизонтального масштабирования, overhead на распределённость Cassandra не оправдан. Проще использовать PostgreSQL или MySQL.

### Реальные примеры использования

- **Netflix**: хранит профили пользователей, историю просмотров, метрики производительности
- **Apple**: iCloud данные, метаданные сервисов
- **Instagram**: direct messages, activity feed
- **Uber**: поездки, история местоположений, трипы
- **Spotify**: плейлисты, рекомендации, история прослушиваний
- **Twitter**: timeline, direct messages, analytics

---

## Краткое резюме

Cassandra — это распределённая NoSQL база данных для сценариев с высокой нагрузкой на запись, большими объёмами данных и требованиями к доступности. Она не подходит для сложных JOIN и ACID-транзакций, но незаменима для time-series данных, IoT, логирования и географически распределённых систем.

## 3. Выполнение ДЗ
- для запуска cassandra состоящего из трех нод в Docker на MacBook был применен docker-compose.yaml слудующей конфигурации: 

```yaml
services:
  cassandra-1:
    image: cassandra:4.1
    container_name: cassandra-node-1
    hostname: cassandra-1
    environment:
      - CASSANDRA_CLUSTER_NAME=OtusCluster
      - CASSANDRA_DC=dc1
      - CASSANDRA_RACK=rack1
      - CASSANDRA_SEEDS=cassandra-1
      - CASSANDRA_LISTEN_ADDRESS=cassandra-1
      - CASSANDRA_RPC_ADDRESS=0.0.0.0
      - CASSANDRA_BROADCAST_ADDRESS=cassandra-1
      - CASSANDRA_BROADCAST_RPC_ADDRESS=cassandra-1
      - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch
      - CASSANDRA_NUM_TOKENS=16
      - MAX_HEAP_SIZE=1536M
      - HEAP_NEWSIZE=256M
    ports:
      - "9042:9042"
    volumes:
      - cassandra_data_1:/var/lib/cassandra
    networks:
      - cassandra_network
    mem_limit: 2g
    healthcheck:
      test: ["CMD-SHELL", "nodetool netstats | grep -q 'Mode: NORMAL'"]
      interval: 15s
      timeout: 10s
      retries: 15
      start_period: 60s

  cassandra-2:
    image: cassandra:4.1
    container_name: cassandra-node-2
    hostname: cassandra-2
    environment:
      - CASSANDRA_CLUSTER_NAME=OtusCluster
      - CASSANDRA_DC=dc1
      - CASSANDRA_RACK=rack1
      - CASSANDRA_SEEDS=cassandra-1
      - CASSANDRA_LISTEN_ADDRESS=cassandra-2
      - CASSANDRA_RPC_ADDRESS=0.0.0.0
      - CASSANDRA_BROADCAST_ADDRESS=cassandra-2
      - CASSANDRA_BROADCAST_RPC_ADDRESS=cassandra-2
      - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch
      - CASSANDRA_NUM_TOKENS=16
      - MAX_HEAP_SIZE=1536M
      - HEAP_NEWSIZE=256M
    ports:
      - "9043:9042"
    volumes:
      - cassandra_data_2:/var/lib/cassandra
    networks:
      - cassandra_network
    depends_on:
      cassandra-1:
        condition: service_healthy
    mem_limit: 2g
    healthcheck:
      test: ["CMD-SHELL", "nodetool netstats | grep -q 'Mode: NORMAL'"]
      interval: 15s
      timeout: 10s
      retries: 15
      start_period: 60s

  cassandra-3:
    image: cassandra:4.1
    container_name: cassandra-node-3
    hostname: cassandra-3
    environment:
      - CASSANDRA_CLUSTER_NAME=OtusCluster
      - CASSANDRA_DC=dc1
      - CASSANDRA_RACK=rack1
      - CASSANDRA_SEEDS=cassandra-1
      - CASSANDRA_LISTEN_ADDRESS=cassandra-3
      - CASSANDRA_RPC_ADDRESS=0.0.0.0
      - CASSANDRA_BROADCAST_ADDRESS=cassandra-3
      - CASSANDRA_BROADCAST_RPC_ADDRESS=cassandra-3
      - CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch
      - CASSANDRA_NUM_TOKENS=16
      - MAX_HEAP_SIZE=1536M
      - HEAP_NEWSIZE=256M
    ports:
      - "9044:9042"
    volumes:
      - cassandra_data_3:/var/lib/cassandra
    networks:
      - cassandra_network
    depends_on:
      cassandra-2:
        condition: service_healthy
    mem_limit: 2g
    healthcheck:
      test: ["CMD-SHELL", "nodetool netstats | grep -q 'Mode: NORMAL'"]
      interval: 15s
      timeout: 10s
      retries: 15
      start_period: 60s

volumes:
  cassandra_data_1:
  cassandra_data_2:
  cassandra_data_3:

networks:
  cassandra_network:
    driver: bridge

```
- **проверка статуса работы нод подтвердила работоспособность**

```bash
  aleksandrgaev@MacBook-Pro-Aleksandr Cassandra % docker exec cassandra-node-1 nodetool status
  Datacenter: dc1
  ===============
  Status=Up/Down
  |/ State=Normal/Leaving/Joining/Moving
  --  Address     Load        Tokens  Owns (effective)  Host ID                               Rack 
  UN  172.19.0.2  109.39 KiB  16      64.7%             b1c42b76-eda1-4e1b-a56e-a05105121c2c  rack1
  UN  172.19.0.4  104.25 KiB  16      76.0%             26542ae1-83b6-4977-b213-6454f45c0988  rack1
  UN  172.19.0.3  70.21 KiB   16      59.3% 
```
## Шаг 1: Подключение к Cassandra
```bash
docker exec -it cassandra-node-1 cqlsh
```
## Шаг 2: Создание Keyspace
```cql
CREATE KEYSPACE IF NOT EXISTS otus 
WITH replication = {
    'class': 'SimpleStrategy',
    'replication_factor': '3'
};

### Проверка создания keyspace

```cql
DESCRIBE KEYSPACES;
```
```bash
otus    system_auth         system_schema  system_views         
system  system_distributed  system_traces  system_virtual_schema
```
## Шаг 3: Выбор keyspace для работы
```cql
USE otus;
```
## Шаг 4: Создание первой таблицы (простая)
```cql
CREATE TABLE IF NOT EXISTS users_by_email (
    email TEXT,
    user_id UUID,
    name TEXT,
    created_at TIMESTAMP,
    PRIMARY KEY (email)
);
```
### Проверка структуры таблицы

```cql
DESCRIBE TABLE users_by_email;

вывод --->
CREATE TABLE otus.users_by_email (
    email text PRIMARY KEY,
    created_at timestamp,
    name text,
    usr_id uuid
) WITH additional_write_policy = '99p'
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND cdc = false
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND memtable = 'default'
    AND crc_check_chance = 1.0
    AND default_time_to_live = 0
    AND extensions = {}
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair = 'BLOCKING'
    AND speculative_retry = '99p';
```
## Шаг 5: Создание второй таблицы

Создадим таблицу событий пользователя с составным первичным ключом.

```cql
CREATE TABLE IF NOT EXISTS user_events (
    user_id UUID,
    event_date DATE,
    event_time TIMESTAMP,
    event_type TEXT,
    event_data TEXT,
    ip_address INET,
    PRIMARY KEY ((user_id, event_date), event_time)
) WITH CLUSTERING ORDER BY (event_time DESC);
```
### Проверка структуры таблицы
```cql
cqlsh:otus> DESCRIBE TABLE user_events;

CREATE TABLE otus.user_events (
    user_id uuid,
    event_date date,
    event_time timestamp,
    event_data text,
    event_type text,
    ip_address inet,
    PRIMARY KEY ((user_id, event_date), event_time)
) WITH CLUSTERING ORDER BY (event_time DESC)
    AND additional_write_policy = '99p'
    AND bloom_filter_fp_chance = 0.01
    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}
    AND cdc = false
    AND comment = ''
    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}
    AND compression = {'chunk_length_in_kb': '16', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}
    AND memtable = 'default'
    AND crc_check_chance = 1.0
    AND default_time_to_live = 0
    AND extensions = {}
    AND gc_grace_seconds = 864000
    AND max_index_interval = 2048
    AND memtable_flush_period_in_ms = 0
    AND min_index_interval = 128
    AND read_repair = 'BLOCKING'
    AND speculative_retry = '99p';
```
**Пояснение структуры:**

| Компонент | Поля | Назначение |
|-----------|------|------------|
| **Partition Key** | `(user_id, event_date)` | Составной ключ — данные распределяются по узлам по комбинации user_id + дата |
| **Clustering Key** | `event_time` | Сортировка событий внутри партиции по времени (по убыванию) |
| **Обычные поля** | `event_type`, `event_data`, `ip_address` | Данные, не входящие в первичный ключ |

## Шаг 6: Вставка данных
### Вставка в таблицу users_by_email

```cql
INSERT INTO users_by_email (email, user_id, name, created_at)
VALUES (
    'john.doe@example.com',
    uuid(),
    'John Doe',
    toTimestamp(now())
);
```

```cql
INSERT INTO users_by_email (email, user_id, name, created_at)
VALUES (
    'jane.smith@example.com',
    uuid(),
    'Jane Smith',
    toTimestamp(now())
);
```
### Вставка в таблицу user_events

```cql
INSERT INTO user_events (user_id, event_date, event_time, event_type, event_data, ip_address)
VALUES (
    uuid(),
    '2026-02-22',
    toTimestamp(now()),
    'LOGIN',
    'User logged in from mobile app',
    '192.168.1.100'
);
```
**Для вставки нескольких событий с одним user_id:**
-- Сначала создадим переменную с user_id (в cqlsh это делается через INSERT с тем же UUID)
```cql
INSERT INTO user_events (user_id, event_date, event_time, event_type, event_data, ip_address)
VALUES (
    123e4567-e89b-12d3-a456-426614174000,
    '2026-02-22',
    dateOf(now()),
    'LOGIN',
    'User logged in',
    '192.168.1.100'
);
INSERT INTO user_events (user_id, event_date, event_time, event_type, event_data, ip_address)
VALUES (
    123e4567-e89b-12d3-a456-426614174000,
    '2026-02-22',
    dateOf(now()),
    'VIEW_PAGE',
    'Viewed product catalog',
    '192.168.1.100'
);
INSERT INTO user_events (user_id, event_date, event_time, event_type, event_data, ip_address)
VALUES (
    123e4567-e89b-12d3-a456-426614174000,
    '2026-02-22',
    dateOf(now()),
    'ADD_TO_CART',
    'Added item to cart',
    '192.168.1.100'
);
```

## Шаг 7: Выполнение запросов

### Запрос к таблице users_by_email

```cql
SELECT * FROM users_by_email WHERE email = 'john.doe@example.com';
```
 email                | created_at                      | name     | usr_id
----------------------+---------------------------------+----------+--------------------------------------
 john.doe@example.com | 2026-02-22 18:12:03.849000+0000 | John Doe | c7b849d3-f549-43c7-967a-ae86a64a3317


 ### Запрос к таблице user_events с partition key

```cql
SELECT * FROM user_events 
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000 
  AND event_date = '2026-02-22';
```

 user_id                              | event_date | event_time                      | event_data             | event_type  | ip_address
--------------------------------------+------------+---------------------------------+------------------------+-------------+---------------
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:24.888000+0000 |     Added item to cart | ADD_TO_CART | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:17.706000+0000 | Viewed product catalog |   VIEW_PAGE | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:18:21.868000+0000 |         User logged in |       LOGIN | 192.168.1.100

### Запрос с фильтром по clustering key

```cql
SELECT * FROM user_events
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
  AND event_date = '2026-02-22';
```
 event_time                      | event_type
---------------------------------+-------------
 2026-02-22 18:25:24.888000+0000 | ADD_TO_CART
 2026-02-22 18:25:17.706000+0000 |   VIEW_PAGE
 2026-02-22 18:18:21.868000+0000 |       LOGIN

```cql cqlsh:otus> SELECT * FROM user_events
        ... WHERE user_id = 123e4567-e89b-12d3-a456-426614174000
        ...   AND event_date = '2026-02-22'
        ...   AND event_time > '2026-02-22 18:18:21.868000+0000';
```
 user_id                              | event_date | event_time                      | event_data             | event_type  | ip_address
--------------------------------------+------------+---------------------------------+------------------------+-------------+---------------
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:24.888000+0000 |     Added item to cart | ADD_TO_CART | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:17.706000+0000 | Viewed product catalog |   VIEW_PAGE | 192.168.1.100

 ### Запрос с LIMIT

```cql
SELECT * FROM user_events 
WHERE user_id = 123e4567-e89b-12d3-a456-426614174000 
  AND event_date = '2026-02-22'
LIMIT 5;
```
 user_id                              | event_date | event_time                      | event_data             | event_type  | ip_address
--------------------------------------+------------+---------------------------------+------------------------+-------------+---------------
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:24.888000+0000 |     Added item to cart | ADD_TO_CART | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:17.706000+0000 | Viewed product catalog |   VIEW_PAGE | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:18:21.868000+0000 |         User logged in |       LOGIN | 192.168.1.100

 ## Шаг 8: Проверка данных в таблицах

### Просмотр всех записей

```cql
SELECT * FROM users_by_email;
SELECT * FROM user_events;
```

 email                  | created_at                      | name       | usr_id
------------------------+---------------------------------+------------+--------------------------------------
   john.doe@example.com | 2026-02-22 18:12:03.849000+0000 |   John Doe | c7b849d3-f549-43c7-967a-ae86a64a3317
 inan.smith@example.com | 2026-02-22 18:12:51.159000+0000 | Ivan Smith | 8b02972f-aca3-452d-b306-9041eb49efff

  user_id                              | event_date | event_time                      | event_data                     | event_type  | ip_address
--------------------------------------+------------+---------------------------------+--------------------------------+-------------+---------------
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:24.888000+0000 |             Added item to cart | ADD_TO_CART | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:25:17.706000+0000 |         Viewed product catalog |   VIEW_PAGE | 192.168.1.100
 123e4567-e89b-12d3-a456-426614174000 | 2026-02-22 | 2026-02-22 18:18:21.868000+0000 |                 User logged in |       LOGIN | 192.168.1.100
 dea9fa18-1bda-4e5a-96af-c87ff49788c5 | 2026-02-22 | 2026-02-22 18:16:29.767000+0000 | User logged in from mobile app |       LOGIN | 192.168.1.100

 ## Шаг 9: Обновление и удаление данных

### Обновление записи

```cql
UPDATE users_by_email 
SET name = 'John Updated' 
WHERE email = 'john.doe@example.com';
```
 email                  | created_at                      | name         | usr_id
------------------------+---------------------------------+--------------+--------------------------------------
   john.doe@example.com | 2026-02-22 18:12:03.849000+0000 | **John Updated** | c7b849d3-f549-43c7-967a-ae86a64a3317
 inan.smith@example.com | 2026-02-22 18:12:51.159000+0000 |   Ivan Smith | 8b02972f-aca3-452d-b306-9041eb49efff

 ### Удаление записи
 ```cql
DELETE FROM users_by_email WHERE email = 'jane.smith@example.com';
```





