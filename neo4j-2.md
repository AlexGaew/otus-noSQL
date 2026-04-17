## Выполнение домашнего задания

### Для работы с Neo4j необходимо развернуть его в docker. Для этого используем docker-compose.
```yaml
version: "3.9"

services:
  etcd1:
    image: quay.io/coreos/etcd:v3.5.14
    container_name: etcd1
    command: >
      /usr/local/bin/etcd
      --name etcd1
      --data-dir /etcd-data
      --listen-client-urls http://0.0.0.0:2379
      --advertise-client-urls http://etcd1:2379
      --listen-peer-urls http://0.0.0.0:2380
      --initial-advertise-peer-urls http://etcd1:2380
      --initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380
      --initial-cluster-state new
      --initial-cluster-token etcd-cluster-1
    ports:
      - "12379:2379"

  etcd2:
    image: quay.io/coreos/etcd:v3.5.14
    container_name: etcd2
    command: >
      /usr/local/bin/etcd
      --name etcd2
      --data-dir /etcd-data
      --listen-client-urls http://0.0.0.0:2379
      --advertise-client-urls http://etcd2:2379
      --listen-peer-urls http://0.0.0.0:2380
      --initial-advertise-peer-urls http://etcd2:2380
      --initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380
      --initial-cluster-state new
      --initial-cluster-token etcd-cluster-1
    ports:
      - "22379:2379"

  etcd3:
    image: quay.io/coreos/etcd:v3.5.14
    container_name: etcd3
    command: >
      /usr/local/bin/etcd
      --name etcd3
      --data-dir /etcd-data
      --listen-client-urls http://0.0.0.0:2379
      --advertise-client-urls http://etcd3:2379
      --listen-peer-urls http://0.0.0.0:2380
      --initial-advertise-peer-urls http://etcd3:2380
      --initial-cluster etcd1=http://etcd1:2380,etcd2=http://etcd2:2380,etcd3=http://etcd3:2380
      --initial-cluster-state new
      --initial-cluster-token etcd-cluster-1
    ports:
      - "32379:2379"
```
### Подготавливаем тестовые данные, создаем синтетический набор сущности, туроператоры страны, туристические локации, транспортные города.
```cypher
MATCH (n) DETACH DELETE n;

UNWIND ['OrbitLine','SkyBridge','TerraWay','NorthWind','SunTrail'] AS opName
MERGE (:TourOperator {name: opName});

UNWIND ['Asteria','Borealis','Cyrenia','Demeria','Eldoria','Fjordland'] AS countryName
MERGE (:Country {name: countryName});

UNWIND [
  {place:'Azure Bay', country:'Asteria'},
  {place:'Coral Coast', country:'Asteria'},
  {place:'Snow Peak', country:'Borealis'},
  {place:'Pine Valley', country:'Borealis'},
  {place:'Old Harbor', country:'Cyrenia'},
  {place:'Amber Cliffs', country:'Cyrenia'},
  {place:'Sun Dunes', country:'Demeria'},
  {place:'Mirage Oasis', country:'Demeria'},
  {place:'Moon Lake', country:'Eldoria'},
  {place:'Green Canyon', country:'Eldoria'},
  {place:'Ice Fjord', country:'Fjordland'},
  {place:'North Cape', country:'Fjordland'}
] AS row
MATCH (c:Country {name: row.country})
MERGE (p:Place {name: row.place})
MERGE (p)-[:IN_COUNTRY]->(c);

UNWIND [
  {name:'Metrograd', hasAirport:true, hasStation:true},
  {name:'Riverport', hasAirport:false, hasStation:true},
  {name:'Hillford', hasAirport:false, hasStation:true},
  {name:'Coastline', hasAirport:true, hasStation:false},
  {name:'DesertGate', hasAirport:false, hasStation:true},
  {name:'Lakeside', hasAirport:false, hasStation:true},
  {name:'HarborCity', hasAirport:true, hasStation:true},
  {name:'Nordhaven', hasAirport:true, hasStation:true}
] AS row
MERGE (c:City {name: row.name})
SET c.hasAirport = row.hasAirport,
    c.hasStation = row.hasStation;

UNWIND [
  {place:'Azure Bay', city:'Coastline'},
  {place:'Coral Coast', city:'Coastline'},
  {place:'Snow Peak', city:'Nordhaven'},
  {place:'Pine Valley', city:'Hillford'},
  {place:'Old Harbor', city:'HarborCity'},
  {place:'Amber Cliffs', city:'HarborCity'},
  {place:'Sun Dunes', city:'DesertGate'},
  {place:'Mirage Oasis', city:'DesertGate'},
  {place:'Moon Lake', city:'Lakeside'},
  {place:'Green Canyon', city:'Lakeside'},
  {place:'Ice Fjord', city:'Nordhaven'},
  {place:'North Cape', city:'Nordhaven'}
] AS row
MATCH (p:Place {name: row.place})
MATCH (c:City {name: row.city})
MERGE (p)-[:NEAR_CITY]->(c);

UNWIND [
  {op:'OrbitLine', place:'Azure Bay'},
  {op:'OrbitLine', place:'Moon Lake'},
  {op:'OrbitLine', place:'Old Harbor'},
  {op:'SkyBridge', place:'Coral Coast'},
  {op:'SkyBridge', place:'Snow Peak'},
  {op:'SkyBridge', place:'Green Canyon'},
  {op:'TerraWay', place:'Sun Dunes'},
  {op:'TerraWay', place:'Mirage Oasis'},
  {op:'TerraWay', place:'Pine Valley'},
  {op:'NorthWind', place:'Ice Fjord'},
  {op:'NorthWind', place:'North Cape'},
  {op:'NorthWind', place:'Amber Cliffs'},
  {op:'SunTrail', place:'Moon Lake'},
  {op:'SunTrail', place:'Azure Bay'},
  {op:'SunTrail', place:'Sun Dunes'}
] AS row
MATCH (o:TourOperator {name: row.op})
MATCH (p:Place {name: row.place})
MERGE (o)-[:OFFERS]->(p);

UNWIND [
  {from:'Metrograd', to:'Riverport', transport:'TRAIN', hours:2},
  {from:'Riverport', to:'Hillford', transport:'BUS', hours:2},
  {from:'Hillford', to:'Lakeside', transport:'TRAIN', hours:3},
  {from:'Lakeside', to:'DesertGate', transport:'BUS', hours:4},
  {from:'Riverport', to:'HarborCity', transport:'TRAIN', hours:3},
  {from:'HarborCity', to:'Nordhaven', transport:'TRAIN', hours:5},
  {from:'Metrograd', to:'Coastline', transport:'AIR', hours:1},
  {from:'Metrograd', to:'Nordhaven', transport:'AIR', hours:2},
  {from:'Coastline', to:'HarborCity', transport:'AIR', hours:1}
] AS row
MATCH (a:City {name: row.from})
MATCH (b:City {name: row.to})
MERGE (a)-[r:ROUTE {transport: row.transport}]->(b)
SET r.durationHours = row.hours;

```

```cypher
MATCH (o:TourOperator) RETURN count(o) AS operators;
MATCH (p:Place) RETURN count(p) AS places;
MATCH (c:Country) RETURN count(c) AS countries;
MATCH (c:City) RETURN count(c) AS cities;
MATCH ()-[r:ROUTE]->() RETURN count(r) AS routes;

```
### Выполнил команды проверки у нас получилось 5 операторов 12 мест 6 стран 8 городов и 9 маршрутов

### Сформируем и выполним запрос, который находит туристические направления, достижимые исключительно наземным транспортом, и отображает полный путь с промежуточными точками.
```cypher
MATCH (o:TourOperator)-[:OFFERS]->(p:Place)-[:NEAR_CITY]->(dest:City)
MATCH path = (start:City {name:'Metrograd'})-[:ROUTE*1..6]->(dest)
WHERE all(r IN relationships(path) WHERE r.transport IN ['TRAIN','BUS'])
RETURN
  o.name AS operator,
  p.name AS place,
  [n IN nodes(path) | n.name] AS route_points,
  [r IN relationships(path) | r.transport] AS transport_chain,
  reduce(total = 0, r IN relationships(path) | total + r.durationHours) AS total_hours
ORDER BY total_hours, operator, place;
```

### Проверка результатов показала, что в найденных маршрутах используются только TRAIN и BUS, без участия авиаперелетов.

### Строим план выполнения запроса до создания индексов, чтобы увидеть, как Neo4j ищет данные и какие операции использует.
```cypher
EXPLAIN
MATCH (o:TourOperator)-[:OFFERS]->(p:Place)-[:NEAR_CITY]->(dest:City)
MATCH path = (start:City {name:'Metrograd'})-[:ROUTE*1..6]->(dest)
WHERE all(r IN relationships(path) WHERE r.transport IN ['TRAIN','BUS'])
RETURN
  o.name AS operator,
  p.name AS place,
  [n IN nodes(path) | n.name] AS route_points,
  [r IN relationships(path) | r.transport] AS transport_chain,
  reduce(total = 0, r IN relationships(path) | total + r.durationHours) AS total_hours
ORDER BY total_hours, operator, place;
```
<img width="543" height="2931" alt="neo4j_query_plan_2026-4-17" src="https://github.com/user-attachments/assets/c78d5132-9c7e-47e0-89f6-4fed3df41b88" />

### «План запроса успешно построен (EXPLAIN). Оптимизатор выполняет последовательный поиск по меткам и фильтрацию, после чего выполняет графовый обход маршрутов и сортировку результата».

**Следующий шаг: запустить этот же запрос через PROFILE, потом добавить индексы и сравнить план до/после.**
```cypher
PROFILE
MATCH (o:TourOperator)-[:OFFERS]->(p:Place)-[:NEAR_CITY]->(dest:City)
MATCH path = (start:City {name:'Metrograd'})-[:ROUTE*1..6]->(dest)
WHERE all(r IN relationships(path) WHERE r.transport IN ['TRAIN','BUS'])
RETURN
  o.name AS operator,
  p.name AS place,
  [n IN nodes(path) | n.name] AS route_points,
  [r IN relationships(path) | r.transport] AS transport_chain,
  reduce(total = 0, r IN relationships(path) | total + r.durationHours) AS total_hours
ORDER BY total_hours, operator, place;

```
<img width="637" height="4399" alt="neo4j_query_plan_2026-4-17 (1)" src="https://github.com/user-attachments/assets/1d695d7c-1154-4be5-a987-6259389c7a88" />

Время выполнения: 43 ms
Общее число обращений к БД: 261.

### Создаем индексы на часто используемых в фильтрах свойствах, чтобы оптимизатор мог использовать индексный поиск вместо полного сканирования по метке.
```cypher
CREATE INDEX city_name_idx IF NOT EXISTS FOR (c:City) ON (c.name);
CREATE INDEX place_name_idx IF NOT EXISTS FOR (p:Place) ON (p.name);
CREATE INDEX operator_name_idx IF NOT EXISTS FOR (o:TourOperator) ON (o.name);
CREATE INDEX country_name_idx IF NOT EXISTS FOR (c:Country) ON (c.name);
```
### Проверим мы, что наши индексы создались корректно.
```cypher
SHOW INDEXES
YIELD name, state, type, entityType, labelsOrTypes, properties
RETURN name, state, type, entityType, labelsOrTypes, properties
ORDER BY name;
```
Время выполнения: 78 ms
Общее число обращений к БД: 266.

### Вывод
План запроса проанализирован до и после создания индексов: после индексации стартовый доступ к ноде изменился с полного сканирования на индексный поиск.
На текущем небольшом синтетическом объеме данных это не дало ускорения по времени, так как основная стоимость запроса связана с обходом графа переменной длины, а не с поиском стартовой ноды.
