# ClickHouse — Общие сведения

## 1. Что такое ClickHouse

ClickHouse — это колоночная аналитическая СУБД (OLAP) с открытым исходным кодом, оптимизированная для быстрых запросов по большим объёмам данных. Она спроектирована для сценариев, где важны высокая скорость чтения, агрегации и работа с миллиардами строк.

В отличие от классических OLTP-баз, ClickHouse не ориентирован на частые точечные обновления и сложные транзакционные процессы. Основной фокус — аналитика: `GROUP BY`, фильтрация, оконные функции, расчёт метрик и построение отчётов в реальном времени.

Проект был создан в Яндексе для задач Яндекс.Метрики и позже открыт как open-source. Сейчас ClickHouse развивается как самостоятельный проект и широко используется в аналитических платформах, observability-решениях и продуктовой аналитике.

### Ключевые характеристики

- **Тип**: Колоночная аналитическая БД (OLAP)
- **Модель данных**: Таблицы с движками хранения (чаще всего семейство `MergeTree`)
- **Запросы**: SQL-совместимый язык, мощные агрегации и функции аналитики
- **Производительность**: Очень высокая скорость чтения и сканирования больших наборов данных
- **Масштабируемость**: Горизонтальная через шардинг и распределённые таблицы
- **Репликация**: Поддержка репликации и отказоустойчивости на уровне таблиц/кластера
- **Транзакции**: Не поддержтвает транзакции, если нужны транзакции ClickHouse не подходит!!!


### Основные концепции

**Колоночное хранение**. Данные хранятся по столбцам, а не по строкам. Это позволяет читать только нужные колонки, эффективнее сжимать данные и значительно ускорять аналитические запросы.

**Движки таблиц**. В ClickHouse важна не только схема таблицы, но и движок (`ENGINE`). Базовый выбор для большинства задач — `MergeTree` и его варианты (`ReplacingMergeTree`, `SummingMergeTree`, `AggregatingMergeTree` и другие), которые определяют поведение хранения, обновлений и агрегаций.

**Партиционирование и сортировка**. Таблицы обычно проектируются с `PARTITION BY` и `ORDER BY`, чтобы сократить объём чтения и ускорить фильтрацию по ключевым измерениям (например, по дате, клиенту, сервису).

**Индексы и пропуск данных**. В ClickHouse используются sparse primary index и data skipping indexes. Они не работают как B-tree в OLTP-базах, но позволяют быстро отбрасывать ненужные куски данных при сканировании.

**Слияния (merge) и части данных**. Записи попадают в immutable parts, которые затем асинхронно объединяются фоновыми merge-процессами. Это влияет на стратегию загрузки данных, удалений и дедупликации.

**Распределённая архитектура**. Для масштабирования применяется шардинг и распределённые таблицы (`Distributed`). Это позволяет параллельно выполнять запросы по нескольким узлам и агрегировать результат на лету.

### CAP-теорема

В чистом виде ClickHouse нельзя однозначно назвать только `AP` или только `CP`, потому что поведение зависит от режима работы кластера:

- Для реплицируемых таблиц (`ReplicatedMergeTree`) система обычно ближе к **CP**: важнее согласованность метаданных и реплик, а при сетевых проблемах часть операций может быть ограничена.
- Для распределённых чтений и аналитических запросов по шардам система может вести себя более **AP-подобно**: при отказе части узлов возможны частичные результаты или чтение с доступных реплик (в зависимости от настроек).

Практический вывод: ClickHouse чаще рассматривают как аналитическую систему с приоритетом корректности данных и контролируемой деградацией доступности, то есть ближе к **CP/гибридному** поведению, а не к классическому AP-подходу Cassandra.

---

## 2. Для чего использовать ClickHouse

### Идеальные сценарии

**Аналитика в реальном времени**. ClickHouse отлично подходит для построения дашбордов и отчётов с задержкой в секунды или минуты после поступления данных.

Примеры: продуктовая аналитика, KPI-дашборды, BI-отчёты.

**Логи и observability**. Высокая скорость вставки и агрегаций делает ClickHouse удобным для логов, метрик и событий.

Примеры: анализ application logs, инфраструктурные метрики, APM/trace-аналитика.

**Time-series и событийные данные**. Модель с партиционированием по времени и эффективной компрессией хорошо подходит для временных рядов.

Примеры: телеметрия IoT, clickstream, аудит, история действий пользователей.

**Ad-hoc аналитика на больших объёмах**. ClickHouse позволяет быстро выполнять сложные исследовательские SQL-запросы по очень крупным таблицам.

Примеры: анализ маркетинговых кампаний, финансовая аналитика, исследование поведения пользователей.

**Снижение стоимости аналитики**. За счёт компрессии и эффективного выполнения запросов ClickHouse часто позволяет снизить инфраструктурные затраты на хранение и вычисления.

### Когда НЕ стоит использовать ClickHouse

**Классические OLTP-нагрузки**. Если система требует большого количества коротких транзакций, частых `UPDATE/DELETE` по отдельным строкам и строгой транзакционной семантики, лучше использовать PostgreSQL/MySQL.

**Сложные межтабличные транзакции**. ClickHouse не предназначен как основная транзакционная база для бизнес-операций с жёсткими ACID-требованиями.

**Частые точечные изменения данных**. Модель хранения в `MergeTree` эффективна для batch-вставок и аналитики, но не оптимальна для постоянных row-by-row обновлений.

**Небольшой проект без аналитической нагрузки**. Если объём данных и требования к аналитике умеренные, внедрение ClickHouse может быть избыточным по сложности эксплуатации.

### Реальные примеры использования

- **Яндекс.Метрика**: веб-аналитика и обработка пользовательских событий
- **Cloudflare**: анализ сетевых и security-событий
- **eBay**: аналитические витрины и внутренние отчёты
- **Lyft**: операционная и продуктовая аналитика
- **PostHog**: продуктовая event-аналитика

---

## Краткое резюме

ClickHouse — это мощная OLAP-база для быстрой аналитики по большим объёмам данных. Он особенно эффективен для логов, метрик, событий и BI-сценариев, но не заменяет классические OLTP-СУБД там, где критичны транзакции и частые точечные обновления.

# Выполнение ДЗ:
- **Для развертывания ClickHouse на локальной машине используем Docker compose**
```yml
services:
  clickhouse:
    image: clickhouse/clickhouse-server:24.8
    container_name: clickhouse-single
    hostname: clickhouse-single
    restart: unless-stopped
    ports:
      - "8123:8123"
      - "9000:9000"
    environment:
      CLICKHOUSE_DB: default
      CLICKHOUSE_USER: default
      CLICKHOUSE_PASSWORD: ""
      CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT: 1
    ulimits:
      nofile:
        soft: 262144
        hard: 262144
    volumes:
      - clickhouse_data:/var/lib/clickhouse
      - clickhouse_logs:/var/log/clickhouse-server
      - ./init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "clickhouse-client --query 'SELECT 1'"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s

volumes:
  clickhouse_data:
  clickhouse_logs:
```

1. Подключимся к клиенту кликхауса.

```bash
    docker exec -it clickhouse-single clickhouse-client 
```
2. Создадим тестовую БД.
```sql
CREATE DATABASE IF NOT EXISTS otus_ch
```
3. Проверим создание тестовой бд.
```sql
    SHOW DATABASES;
```
<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/d1b0088f-3d49-48e0-8b73-d94daee9cfc5" />


4. Вставляем тестовые данные.
```sql
    CREATE TABLE otus_ch.trips (
    trip_id             UInt32,
    pickup_datetime     DateTime,
    dropoff_datetime    DateTime,
    pickup_longitude    Nullable(Float64),
    pickup_latitude     Nullable(Float64),
    dropoff_longitude   Nullable(Float64),
    dropoff_latitude    Nullable(Float64),
    passenger_count     UInt8,
    trip_distance       Float32,
    fare_amount         Float32,
    extra               Float32,
    tip_amount          Float32,
    tolls_amount        Float32,
    total_amount        Float32,
    payment_type        Enum('CSH' = 1, 'CRE' = 2, 'NOC' = 3, 'DIS' = 4, 'UNK' = 5),
    pickup_ntaname      LowCardinality(String),
    dropoff_ntaname     LowCardinality(String)
)
ENGINE = MergeTree
PRIMARY KEY (pickup_datetime, dropoff_datetime);

INSERT INTO otus_ch.trips
SELECT
    trip_id,
    pickup_datetime,
    dropoff_datetime,
    pickup_longitude,
    pickup_latitude,
    dropoff_longitude,
    dropoff_latitude,
    passenger_count,
    trip_distance,
    fare_amount,
    extra,
    tip_amount,
    tolls_amount,
    total_amount,
    payment_type,
    pickup_ntaname,
    dropoff_ntaname
FROM s3(
    'https://datasets-documentation.s3.eu-west-3.amazonaws.com/nyc-taxi/trips_{0..2}.gz',
    'TabSeparatedWithNames'
);

```
- 4.1 Проверяем наличие тестовых данных.

<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/f782dc90-fc62-4602-a1bf-e55ebc678342" />

<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/d94f046c-adab-4d95-8540-d3a52207ec31" />


5. Теперь необходимо выполнить несколько запросов и оценить скорость выполнения.

```sql
SELECT payment_type, count() AS rides
FROM trips
GROUP BY payment_type
ORDER BY rides DESC;
```
<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/f1592caf-729e-495c-8b4a-7bd297fa5d3f" />

```sql
SELECT pickup_ntaname, count() AS rides
FROM trips
GROUP BY pickup_ntaname
ORDER BY rides DESC
LIMIT 10;
```
<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/64371c2e-1b60-456c-a5fc-ce6442a2dcb1" />

```sql
    SELECT toDate(pickup_datetime) AS day, count() AS rides
FROM trips
GROUP BY day
ORDER BY day
LIMIT 31;
```
<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/4820c5ed-74d5-4a9a-a3aa-f86890afb7fd" />


5.1 Снимаем время выполнения запросов From.
```sql
SYSTEM FLUSH LOGS;

SELECT
    round(query_duration_ms / 1000, 3) AS sec,
    read_rows,
    formatReadableSize(read_bytes) AS read_size,
    query
FROM system.query_log
WHERND query LIKE '%FROM trips%'
ORDE type = 'QueryFinish'
  AER BY event_time DESC
LIMIT 10;
```
<img width="343" height="279" alt="image" src="https://github.com/user-attachments/assets/ca9f4a7f-65a5-428e-9b01-a9dc697da794" />

-- Все 3 агрегационных запроса на 3 млн строк выполнились быстро (~0.009–0.012 sec).
-- Самый “тяжёлый” из трёх — агрегация по дням (toDate(...)), так как читает больше данных (11.45 MiB против 2.86 MiB).

6. Разворачиваем дополнительную базу данных из ресурса Clickhouse.com
[text](https://clickhouse.com/docs/en/getting-started/example-datasets/uk-price-paid)
6.1 Проверяем заполненность тестовыми данными.

```bash
SELECT count() FROM otus_ch.uk_price_paid;
SELECT count()
FROM otus_ch.uk_price_paid
Query id: 635c82c0-10b7-4e3f-8a1e-4c62a57dd25e
   ┌──count()─┐
1. │ 31004536 │ -- 31.00 million
   └──────────┘
1 row in set. Elapsed: 0.002 sec. 
```
6.2 Выполним несколько тестовых запросов.
```bash
clickhouse-single :) SELECT
   toYear(date) AS year,
   round(avg(price)) AS price,
   bar(price, 0, 1000000, 80
)
FROM otus_ch.uk_price_paid
GROUP BY year
ORDER BY year

SELECT
    toYear(date) AS year,
    round(avg(price)) AS price,
    bar(price, 0, 1000000, 80)
FROM otus_ch.uk_price_paid
GROUP BY year
ORDER BY year ASC

Query id: 9b57be09-8a5c-4efb-b6ce-06e749484907

    ┌─year─┬──price─┬─bar(price, 0, 1000000, 80)─────────┐
 1. │ 1995 │  67947 │ █████▍                             │
 2. │ 1996 │  71522 │ █████▋                             │
 3. │ 1997 │  78553 │ ██████▎                            │
 4. │ 1998 │  85451 │ ██████▊                            │
 5. │ 1999 │  96053 │ ███████▋                           │
 6. │ 2000 │ 107501 │ ████████▌                          │
 7. │ 2001 │ 118902 │ █████████▌                         │
 8. │ 2002 │ 137967 │ ███████████                        │
 9. │ 2003 │ 155906 │ ████████████▍                      │
10. │ 2004 │ 178917 │ ██████████████▎                    │
11. │ 2005 │ 189409 │ ███████████████▏                   │
12. │ 2006 │ 203556 │ ████████████████▎                  │
13. │ 2007 │ 219389 │ █████████████████▌                 │
14. │ 2008 │ 217145 │ █████████████████▎                 │
15. │ 2009 │ 213416 │ █████████████████                  │
16. │ 2010 │ 236100 │ ██████████████████▉                │
17. │ 2011 │ 232826 │ ██████████████████▋                │
18. │ 2012 │ 238406 │ ███████████████████                │
19. │ 2013 │ 256986 │ ████████████████████▌              │
20. │ 2014 │ 280065 │ ██████████████████████▍            │
21. │ 2015 │ 297461 │ ███████████████████████▊           │
22. │ 2016 │ 313728 │ █████████████████████████          │
23. │ 2017 │ 346872 │ ███████████████████████████▋       │
24. │ 2018 │ 351522 │ ████████████████████████████       │
25. │ 2019 │ 355270 │ ████████████████████████████▍      │
26. │ 2020 │ 375360 │ ██████████████████████████████     │
27. │ 2021 │ 389418 │ ███████████████████████████████▏   │
28. │ 2022 │ 414274 │ █████████████████████████████████▏ │
29. │ 2023 │ 405849 │ ████████████████████████████████▍  │
30. │ 2024 │ 396912 │ ███████████████████████████████▊   │
31. │ 2025 │ 372632 │ █████████████████████████████▊     │
32. │ 2026 │ 353989 │ ████████████████████████████▎      │
    └──────┴────────┴────────────────────────────────────┘

32 rows in set. Elapsed: 0.036 sec. Processed 31.00 million rows, 186.03 MB (867.35 million rows/s., 5.20 GB/s.)
Peak memory usage: 4.36 MiB.
```
```bash 
SELECT
   toYear(date) AS year,
   round(avg(price)) AS price,
   bar(price, 0, 2000000, 100
)
FROM otus_ch.uk_price_paid
WHERE town = 'LONDON'
GROUP BY year
ORDER BY year

Query id: 0de89244-ab06-41a0-8242-e677ff1355d4

    ┌─year─┬───price─┬─bar(price, 0, 2000000, 100)───────────────────────────┐
 1. │ 1995 │  109156 │ █████▍                                                │
 2. │ 1996 │  118687 │ █████▉                                                │
 3. │ 1997 │  136557 │ ██████▊                                               │
 4. │ 1998 │  153001 │ ███████▋                                              │
 5. │ 1999 │  180673 │ █████████                                             │
 6. │ 2000 │  215872 │ ██████████▊                                           │
 7. │ 2001 │  232992 │ ███████████▋                                          │
 8. │ 2002 │  263678 │ █████████████▏                                        │
 9. │ 2003 │  278419 │ █████████████▉                                        │
10. │ 2004 │  304716 │ ███████████████▏                                      │
11. │ 2005 │  323002 │ ████████████████▏                                     │
12. │ 2006 │  356305 │ █████████████████▊                                    │
13. │ 2007 │  404136 │ ████████████████████▏                                 │
14. │ 2008 │  420787 │ █████████████████████                                 │
15. │ 2009 │  427830 │ █████████████████████▍                                │
16. │ 2010 │  480307 │ ████████████████████████                              │
17. │ 2011 │  496358 │ ████████████████████████▊                             │
18. │ 2012 │  519580 │ █████████████████████████▉                            │
19. │ 2013 │  616559 │ ██████████████████████████████▊                       │
20. │ 2014 │  724254 │ ████████████████████████████████████▏                 │
21. │ 2015 │  792498 │ ███████████████████████████████████████▌              │
22. │ 2016 │  843692 │ ██████████████████████████████████████████▏           │
23. │ 2017 │  983457 │ █████████████████████████████████████████████████▏    │
24. │ 2018 │ 1014015 │ ██████████████████████████████████████████████████▋   │
25. │ 2019 │ 1049836 │ ████████████████████████████████████████████████████▍ │
26. │ 2020 │ 1050794 │ ████████████████████████████████████████████████████▌ │
27. │ 2021 │  965066 │ ████████████████████████████████████████████████▎     │
28. │ 2022 │ 1013465 │ ██████████████████████████████████████████████████▋   │
29. │ 2023 │ 1020234 │ ███████████████████████████████████████████████████   │
30. │ 2024 │  963364 │ ████████████████████████████████████████████████▏     │
31. │ 2025 │  836530 │ █████████████████████████████████████████▊            │
32. │ 2026 │  732380 │ ████████████████████████████████████▌                 │
    └──────┴─────────┴───────────────────────────────────────────────────────┘

32 rows in set. Elapsed: 0.020 sec. Processed 31.00 million rows, 77.57 MB (1.53 billion rows/s., 3.82 GB/s.)
Peak memory usage: 4.25 MiB.
```

```bash
SELECT
    town,
    district,
    count() AS c,
    round(avg(price)) AS price,
    bar(price, 0, 5000000, 100)
FROM otus_ch.uk_price_paid
WHERE date >= '2020-01-01'
GROUP BY
    town,
    district
HAVING c >= 100
ORDER BY price DESC
LIMIT 100
Query id: 7487ab28-6cbe-4b76-bb8f-11b362f1d0ae

     ┌─town─────────────────┬─district───────────────┬─────c─┬───price─┬─bar(price, 0, 5000000, 100)────────────────────────────────────────────────┐
  1. │ LONDON               │ CITY OF LONDON         │  1659 │ 3675199 │ █████████████████████████████████████████████████████████████████████████▌ │
  2. │ LONDON               │ CITY OF WESTMINSTER    │ 20121 │ 2891952 │ █████████████████████████████████████████████████████████▊                 │
  3. │ LONDON               │ KENSINGTON AND CHELSEA │ 13150 │ 2401798 │ ████████████████████████████████████████████████                           │
  4. │ VIRGINIA WATER       │ RUNNYMEDE              │   663 │ 1943899 │ ██████████████████████████████████████▉                                    │
  5. │ LEATHERHEAD          │ ELMBRIDGE              │   488 │ 1919727 │ ██████████████████████████████████████▍                                    │
  6. │ HENLEY-ON-THAMES     │ BUCKINGHAMSHIRE        │   162 │ 1645932 │ ████████████████████████████████▉                                          │
  7. │ LONDON               │ CAMDEN                 │ 15437 │ 1559139 │ ███████████████████████████████▏                                           │
  8. │ NORTHWOOD            │ THREE RIVERS           │   346 │ 1359838 │ ███████████████████████████▏                                               │
  9. │ COBHAM               │ ELMBRIDGE              │  1758 │ 1278514 │ █████████████████████████▌                                                 │
 10. │ LONDON               │ RICHMOND UPON THAMES   │  3303 │ 1249538 │ ████████████████████████▉                                                  │
 11. │ BARNET               │ ENFIELD                │   708 │ 1237529 │ ████████████████████████▊                                                  │
 12. │ WINDSOR              │ BRACKNELL FOREST       │   190 │ 1237438 │ ████████████████████████▋                                                  │
 13. │ BEACONSFIELD         │ BUCKINGHAMSHIRE        │  1845 │ 1213626 │ ████████████████████████▎                                                  │
 14. │ SURBITON             │ ELMBRIDGE              │   414 │ 1144558 │ ██████████████████████▉                                                    │
 15. │ WATFORD              │ HERTSMERE              │   154 │ 1140391 │ ██████████████████████▊                                                    │
 16. │ LONDON               │ HOUNSLOW               │  3228 │ 1137333 │ ██████████████████████▋                                                    │
 17. │ LONDON               │ ISLINGTON              │ 14953 │ 1122519 │ ██████████████████████▍                                                    │
 18. │ RICHMOND             │ RICHMOND UPON THAMES   │  4106 │ 1110934 │ ██████████████████████▏                                                    │
 19. │ ASCOT                │ WINDSOR AND MAIDENHEAD │  2108 │ 1106979 │ ██████████████████████▏                                                    │
 20. │ RADLETT              │ HERTSMERE              │  1202 │ 1100669 │ ██████████████████████                                                     │
 21. │ ESHER                │ ELMBRIDGE              │  2217 │ 1100177 │ ██████████████████████                                                     │
 22. │ LONDON               │ KINGSTON UPON THAMES   │   213 │ 1096126 │ █████████████████████▉                                                     │
 23. │ WEYBRIDGE            │ ELMBRIDGE              │  2965 │ 1094797 │ █████████████████████▉                                                     │
 24. │ WINDLESHAM           │ SURREY HEATH           │   418 │ 1090530 │ █████████████████████▊                                                     │
 25. │ LONDON               │ HAMMERSMITH AND FULHAM │ 16477 │ 1049199 │ ████████████████████▉                                                      │
 26. │ LEATHERHEAD          │ GUILDFORD              │   926 │ 1038697 │ ████████████████████▊                                                      │
 27. │ BROCKENHURST         │ NEW FOREST             │   497 │ 1027352 │ ████████████████████▌                                                      │
 28. │ WELWYN               │ EAST HERTFORDSHIRE     │   159 │ 1017575 │ ████████████████████▎                                                      │
 29. │ SALCOMBE             │ SOUTH HAMS             │   498 │ 1014816 │ ████████████████████▎                                                      │
 30. │ CHALFONT ST GILES    │ BUCKINGHAMSHIRE        │   625 │ 1014524 │ ████████████████████▎                                                      │
 31. │ GERRARDS CROSS       │ BUCKINGHAMSHIRE        │  2026 │ 1006272 │ ████████████████████▏                                                      │
 32. │ FARNHAM              │ HART                   │   267 │  973532 │ ███████████████████▍                                                       │
 33. │ MAIDENHEAD           │ BUCKINGHAMSHIRE        │   658 │  951972 │ ███████████████████                                                        │
 34. │ GUILDFORD            │ WAVERLEY               │   614 │  945036 │ ██████████████████▉                                                        │
 35. │ LONDON               │ EPPING FOREST          │   101 │  941897 │ ██████████████████▊                                                        │
 36. │ BURFORD              │ WEST OXFORDSHIRE       │   508 │  934272 │ ██████████████████▋                                                        │
 37. │ OXFORD               │ SOUTH OXFORDSHIRE      │  1559 │  932126 │ ██████████████████▋                                                        │
 38. │ EAST MOLESEY         │ ELMBRIDGE              │   808 │  931655 │ ██████████████████▋                                                        │
 39. │ HARTFIELD            │ WEALDEN                │   184 │  929003 │ ██████████████████▌                                                        │
 40. │ FARNHAM              │ EAST HAMPSHIRE         │   235 │  924950 │ ██████████████████▍                                                        │
 41. │ LONDON               │ TOWER HAMLETS          │ 24568 │  924365 │ ██████████████████▍                                                        │
 42. │ HASSOCKS             │ LEWES                  │   204 │  923435 │ ██████████████████▍                                                        │
 43. │ HARPENDEN            │ ST ALBANS              │  3109 │  920384 │ ██████████████████▍                                                        │
 44. │ SUTTON COLDFIELD     │ LICHFIELD              │   257 │  920017 │ ██████████████████▍                                                        │
 45. │ COVENTRY             │ WARWICK                │   264 │  917776 │ ██████████████████▎                                                        │
 46. │ INGATESTONE          │ CHELMSFORD             │   315 │  911764 │ ██████████████████▏                                                        │
 47. │ LONDON               │ WANDSWORTH             │ 35534 │  909677 │ ██████████████████▏                                                        │
 48. │ LONDON               │ MERTON                 │ 11184 │  908775 │ ██████████████████▏                                                        │
 49. │ HENLEY-ON-THAMES     │ SOUTH OXFORDSHIRE      │  2487 │  906740 │ ██████████████████▏                                                        │
 50. │ READING              │ WINDSOR AND MAIDENHEAD │   173 │  904146 │ ██████████████████                                                         │
 51. │ POTTERS BAR          │ WELWYN HATFIELD        │   708 │  898529 │ █████████████████▉                                                         │
 52. │ SUTTON               │ EPSOM AND EWELL        │   121 │  895700 │ █████████████████▉                                                         │
 53. │ PETERSFIELD          │ CHICHESTER             │   227 │  892439 │ █████████████████▊                                                         │
 54. │ TONBRIDGE            │ SEVENOAKS              │   234 │  888699 │ █████████████████▊                                                         │
 55. │ IVER                 │ BUCKINGHAMSHIRE        │  1040 │  881090 │ █████████████████▌                                                         │
 56. │ SOLIHULL             │ WARWICK                │   249 │  879059 │ █████████████████▌                                                         │
 57. │ KESTON               │ BROMLEY                │   380 │  877376 │ █████████████████▌                                                         │
 58. │ THATCHAM             │ BASINGSTOKE AND DEANE  │   142 │  865254 │ █████████████████▎                                                         │
 59. │ THAMES DITTON        │ ELMBRIDGE              │  1115 │  860643 │ █████████████████▏                                                         │
 60. │ KINGSTON UPON THAMES │ KINGSTON UPON THAMES   │  4674 │  851814 │ █████████████████                                                          │
 61. │ LONDON               │ SOUTHWARK              │ 20922 │  848121 │ ████████████████▉                                                          │
 62. │ STOCKBRIDGE          │ TEST VALLEY            │   758 │  848071 │ ████████████████▉                                                          │
 63. │ EGHAM                │ RUNNYMEDE              │  2278 │  846615 │ ████████████████▉                                                          │
 64. │ EAST GRINSTEAD       │ TANDRIDGE              │   271 │  846455 │ ████████████████▉                                                          │
 65. │ TRING                │ BUCKINGHAMSHIRE        │   158 │  842595 │ ████████████████▊                                                          │
 66. │ TWICKENHAM           │ RICHMOND UPON THAMES   │  5414 │  841784 │ ████████████████▊                                                          │
 67. │ WEMBLEY              │ BRENT                  │  4920 │  837987 │ ████████████████▊                                                          │
 68. │ HASLEMERE            │ CHICHESTER             │   528 │  828704 │ ████████████████▌                                                          │
 69. │ BILLINGSHURST        │ CHICHESTER             │   579 │  826971 │ ████████████████▌                                                          │
 70. │ SOLIHULL             │ STRATFORD-ON-AVON      │   299 │  823634 │ ████████████████▍                                                          │
 71. │ BATH                 │ WILTSHIRE              │   116 │  818534 │ ████████████████▎                                                          │
 72. │ LECHLADE             │ COTSWOLD               │   431 │  816869 │ ████████████████▎                                                          │
 73. │ MARLOW               │ BUCKINGHAMSHIRE        │  1852 │  813250 │ ████████████████▎                                                          │
 74. │ PULBOROUGH           │ CHICHESTER             │   196 │  812801 │ ████████████████▎                                                          │
 75. │ CHIGWELL             │ EPPING FOREST          │  1067 │  810806 │ ████████████████▏                                                          │
 76. │ ALRESFORD            │ EAST HAMPSHIRE         │   162 │  808037 │ ████████████████▏                                                          │
 77. │ UPMINSTER            │ THURROCK               │   198 │  807620 │ ████████████████▏                                                          │
 78. │ KINGSTON UPON THAMES │ RICHMOND UPON THAMES   │   412 │  807557 │ ████████████████▏                                                          │
 79. │ PETWORTH             │ CHICHESTER             │   614 │  807114 │ ████████████████▏                                                          │
 80. │ TEDDINGTON           │ RICHMOND UPON THAMES   │  2631 │  806198 │ ████████████████                                                           │
 81. │ LONDON               │ BARNET                 │ 21376 │  798900 │ ███████████████▉                                                           │
 82. │ RUGBY                │ WEST NORTHAMPTONSHIRE  │   260 │  797951 │ ███████████████▉                                                           │
 83. │ CROYDON              │ SUTTON                 │   385 │  788361 │ ███████████████▊                                                           │
 84. │ WOKING               │ GUILDFORD              │   906 │  786452 │ ███████████████▋                                                           │
 85. │ LONDON               │ EALING                 │ 15794 │  785211 │ ███████████████▋                                                           │
 86. │ ASCOT                │ BRACKNELL FOREST       │   769 │  782864 │ ███████████████▋                                                           │
 87. │ HINDHEAD             │ WAVERLEY               │   522 │  781233 │ ███████████████▌                                                           │
 88. │ LONDON               │ HACKNEY                │ 17425 │  780944 │ ███████████████▌                                                           │
 89. │ PURFLEET-ON-THAMES   │ THURROCK               │   490 │  773871 │ ███████████████▍                                                           │
 90. │ LONDON               │ BRENT                  │ 10698 │  773671 │ ███████████████▍                                                           │
 91. │ BERKHAMSTED          │ DACORUM                │  2564 │  770370 │ ███████████████▍                                                           │
 92. │ BETCHWORTH           │ MOLE VALLEY            │   375 │  766179 │ ███████████████▎                                                           │
 93. │ READING              │ SOUTH OXFORDSHIRE      │  1496 │  762373 │ ███████████████▏                                                           │
 94. │ RICKMANSWORTH        │ THREE RIVERS           │  3584 │  762282 │ ███████████████▏                                                           │
 95. │ HAYWARDS HEATH       │ WEALDEN                │   133 │  761627 │ ███████████████▏                                                           │
 96. │ RADLETT              │ ST ALBANS              │   152 │  760342 │ ███████████████▏                                                           │
 97. │ MUCH HADHAM          │ EAST HERTFORDSHIRE     │   192 │  759088 │ ███████████████▏                                                           │
 98. │ NEWPORT PAGNELL      │ MILTON KEYNES          │  1854 │  754351 │ ███████████████                                                            │
 99. │ WADHURST             │ WEALDEN                │   572 │  754016 │ ███████████████                                                            │
100. │ AMERSHAM             │ BUCKINGHAMSHIRE        │  2247 │  751281 │ ███████████████                                                            │
     └─town─────────────────┴─district───────────────┴─────c─┴───price─┴─bar(price, 0, 5000000, 100)────────────────────────────────────────────────┘

100 rows in set. Elapsed: 0.060 sec. Processed 31.00 million rows, 310.05 MB (518.21 million rows/s., 5.18 GB/s.)
Peak memory usage: 34.42 MiB.
```
6.3 Выполним запрос, который выведет нам время выполнения наших тестовых команд.
```bash
SELECT
    event_time,
    round(query_duration_ms / 1000, 3) AS sec,
    read_rows,
    formatReadableSize(read_bytes) AS read_size,
    query
FROM system.query_log
WHERE type = 'QueryFinish'
  AND query LIKE '%FROM otus_ch.uk_price_paid%'
ORDER BY event_time DESC
LIMIT 10;
```
```bash
1. │ 2026-03-09 10:34:30 │ 0.059 │  31004536 │ 295.68 MiB │ SELECT
    town,
    district,
    count() AS c,
    round(avg(price)) AS price,
    bar(price, 0, 5000000, 100)
FROM otus_ch.uk_price_paid
WHERE date >= '2020-01-01'
GROUP BY
    town,
    district
HAVING c >= 100
ORDER BY price DESC
LIMIT 100 │
2. │ 2026-03-09 10:33:03 │ 0.019 │  31004536 │ 73.97 MiB  │ SELECT
   toYear(date) AS year,
   round(avg(price)) AS price,
   bar(price, 0, 2000000, 100
)
FROM otus_ch.uk_price_paid
WHERE town = 'LONDON'
GROUP BY year
ORDER BY year                                                                     │
3. │ 2026-03-09 10:30:21 │ 0.035 │  31004536 │ 177.41 MiB │ SELECT
   toYear(date) AS year,
   round(avg(price)) AS price,
   bar(price, 0, 1000000, 80
)
```
-- Обратим внимание, что дольше всего выполнялась команда, которая выводила самые дорогие районы Лондона. 0.059

## 7. Развернем Klichouse в кластерном исполнении.

```yml
services:
  keeper:
    image: clickhouse/clickhouse-keeper:24.8
    container_name: keeper
    hostname: keeper
    restart: unless-stopped
    ports:
      - "2181:2181"
    volumes:
      - ./keeper/keeper_config.xml:/etc/clickhouse-keeper/keeper_config.xml
      - keeper_data:/var/lib/clickhouse-keeper
      - keeper_logs:/var/log/clickhouse-keeper
    command: ["clickhouse-keeper", "--config-file=/etc/clickhouse-keeper/keeper_config.xml"]

  ch1:
    image: clickhouse/clickhouse-server:24.8
    container_name: ch1
    hostname: ch1
    restart: unless-stopped
    ports:
      - "8123:8123"
      - "9000:9000"
    volumes:
      - ./config.d/cluster.xml:/etc/clickhouse-server/config.d/cluster.xml
      - ./config.d/zookeeper.xml:/etc/clickhouse-server/config.d/zookeeper.xml
      - ch1_data:/var/lib/clickhouse
      - ch1_logs:/var/log/clickhouse-server
    environment:
      CLICKHOUSE_USER: default
      CLICKHOUSE_PASSWORD: ""
      CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT: 1
    depends_on:
      - keeper

  ch2:
    image: clickhouse/clickhouse-server:24.8
    container_name: ch2
    hostname: ch2
    restart: unless-stopped
    ports:
      - "8124:8123"
      - "9001:9000"
    volumes:
      - ./config.d/cluster.xml:/etc/clickhouse-server/config.d/cluster.xml
      - ./config.d/zookeeper.xml:/etc/clickhouse-server/config.d/zookeeper.xml
      - ch2_data:/var/lib/clickhouse
      - ch2_logs:/var/log/clickhouse-server
    environment:
      CLICKHOUSE_USER: default
      CLICKHOUSE_PASSWORD: ""
      CLICKHOUSE_DEFAULT_ACCESS_MANAGEMENT: 1
    depends_on:
      - keeper

volumes:
  keeper_data:
  keeper_logs:
  ch1_data:
  ch1_logs:
  ch2_data:
  ch2_logs:
```
## cluster.xml
```xml
<clickhouse>
  <remote_servers>
    <otus_cluster>
      <shard>
        <replica>
          <host>ch1</host>
          <port>9000</port>
        </replica>
      </shard>
      <shard>
        <replica>
          <host>ch2</host>
          <port>9000</port>
        </replica>
      </shard>
    </otus_cluster>
  </remote_servers>
</clickhouse>
```
## zookeeper.xml
```xml
<clickhouse>
  <zookeeper>
    <node>
      <host>keeper</host>
      <port>2181</port>
    </node>
  </zookeeper>

  <distributed_ddl>
    <path>/clickhouse/task_queue/ddl</path>
  </distributed_ddl>
</clickhouse>
```
## keeper_config.xml
```xml
<clickhouse>
  <logger>
    <level>information</level>
    <console>true</console>
  </logger>

  <listen_host>0.0.0.0</listen_host>

  <keeper_server>
    <tcp_port>2181</tcp_port>
    <server_id>1</server_id>

    <log_storage_path>/var/lib/clickhouse-keeper/coordination/log</log_storage_path>
    <snapshot_storage_path>/var/lib/clickhouse-keeper/coordination/snapshots</snapshot_storage_path>

    <coordination_settings>
      <operation_timeout_ms>10000</operation_timeout_ms>
      <session_timeout_ms>30000</session_timeout_ms>
    </coordination_settings>

    <raft_configuration>
      <server>
        <id>1</id>
        <hostname>keeper</hostname>
        <port>9234</port>
      </server>
    </raft_configuration>
  </keeper_server>
</clickhouse>
```
## 7.1  подключаемся к первому кластеру. 
```bash
docker exec -it ch1 clickhouse-client
```
## 7.2  создаем на наших otus_cluster базу данных otus_ch
```bash
CREATE DATABASE IF NOT EXISTS otus_ch ON CLUSTER otus_cluster;
SELECT hostName(), name
FROM clusterAllReplicas('otus_cluster', system.databases)
WHERE name = 'otus_ch';
CREATE DATABASE IF NOT EXISTS otus_ch ON CLUSTER otus_cluster
Query id: 32ef3e6c-e83e-4b11-ad3c-67840ab99e40
   ┌─host─┬─port─┬─status─┬─error─┬─num_hosts_remaining─┬─num_hosts_active─┐
1. │ ch1  │ 9000 │      0 │       │                   1 │                0 │
2. │ ch2  │ 9000 │      0 │       │                   0 │                0 │
   └──────┴──────┴────────┴───────┴─────────────────────┴──────────────────┘
2 rows in set. Elapsed: 0.064 sec. 
SELECT
    hostName(),
    name
FROM clusterAllReplicas('otus_cluster', system.databases)
WHERE name = 'otus_ch'
Query id: 87dee6a2-5dc3-4196-812b-2e23da3854f2
   ┌─hostName()─┬─name────┐
1. │ ch1        │ otus_ch │
   └────────────┴─────────┘
   ┌─hostName()─┬─name────┐
2. │ ch2        │ otus_ch │
   └────────────┴─────────┘
2 rows in set. Elapsed: 0.007 sec. 
```
## 7.3  начинаем заполнять наши кластерные таблицы тестовыми данными. 
```sql
CREATE TABLE otus_ch.uk_price_paid_local ON CLUSTER otus_cluster
(
    price UInt32,
    date Date,
    postcode1 LowCardinality(String),
    postcode2 LowCardinality(String),
    type Enum8('terraced' = 1, 'semi-detached' = 2, 'detached' = 3, 'flat' = 4, 'other' = 0),
    is_new UInt8,
    duration Enum8('freehold' = 1, 'leasehold' = 2, 'unknown' = 0),
    addr1 String,
    addr2 String,
    street LowCardinality(String),
    locality LowCardinality(String),
    town LowCardinality(String),
    district LowCardinality(String),
    county LowCardinality(String)
)
ENGINE = MergeTree
ORDER BY (postcode1, postcode2, addr1, addr2);

CREATE TABLE otus_ch.uk_price_paid_dist ON CLUSTER otus_cluster
AS otus_ch.uk_price_paid_local
ENGINE = Distributed('otus_cluster', 'otus_ch', 'uk_price_paid_local', cityHash64(postcode1, addr1));

INSERT INTO otus_ch.uk_price_paid_dist
SELECT *
FROM s3('https://datasets-documentation.s3.eu-west-3.amazonaws.com/uk-house-prices/parquet/house_prices.parquet');
```
 проверяем наличие тестовых данных. 
 ```sql
SELECT count() FROM otus_ch.uk_price_paid_dist;

SELECT hostName(), count()
FROM clusterAllReplicas('otus_cluster', otus_ch.uk_price_paid_local)
GROUP BY hostName()
ORDER BY hostName();
```
```bash
SELECT count() FROM otus_ch.uk_price_paid_dist;

SELECT hostName(), count()
FROM clusterAllReplicas('otus_cluster', otus_ch.uk_price_paid_local)
GROUP BY hostName()
ORDER BY hostName();


SELECT count()
FROM otus_ch.uk_price_paid_dist
Query id: 4ec222d6-7213-414e-b2bc-3f6db1bc8f94
   ┌──count()─┐
1. │ 28276228 │ -- 28.28 million
   └──────────┘
1 row in set. Elapsed: 0.011 sec. 
SELECT
    hostName(),
    count()
FROM clusterAllReplicas('otus_cluster', otus_ch.uk_price_paid_local)
GROUP BY hostName()
ORDER BY hostName() ASC
Query id: 5b716fa9-65ca-4a68-8e61-c56fe658d618

   ┌─hostName()─┬──count()─┐
1. │ ch1        │ 14079106 │
2. │ ch2        │ 14197122 │
   └────────────┴──────────┘
2 rows in set. Elapsed: 0.009 sec. 
```
## 7.4 Выполняем три запроса, точно таких же, какие выполняли в нереплицированной базе данных.
```sql
SELECT
    toYear(date) AS year,
    round(avg(price)) AS price,
    bar(price, 0, 1000000, 80)
FROM otus_ch.uk_price_paid_dist
GROUP BY year
ORDER BY year;
```
```bash
Query id: 7254b9d0-5e00-4fa0-bb05-05f263950efb

    ┌─year─┬──price─┬─bar(price, 0, 1000000, 80)───────┐
 1. │ 1995 │  67938 │ █████▍                           │
 2. │ 1996 │  71513 │ █████▋                           │
 3. │ 1997 │  78543 │ ██████▎                          │
 4. │ 1998 │  85443 │ ██████▊                          │
 5. │ 1999 │  96041 │ ███████▋                         │
 6. │ 2000 │ 107493 │ ████████▌                        │
 7. │ 2001 │ 118893 │ █████████▌                       │
 8. │ 2002 │ 137958 │ ███████████                      │
 9. │ 2003 │ 155894 │ ████████████▍                    │
10. │ 2004 │ 178891 │ ██████████████▎                  │
11. │ 2005 │ 189362 │ ███████████████▏                 │
12. │ 2006 │ 203535 │ ████████████████▎                │
13. │ 2007 │ 219376 │ █████████████████▌               │
14. │ 2008 │ 217044 │ █████████████████▎               │
15. │ 2009 │ 213424 │ █████████████████                │
16. │ 2010 │ 236115 │ ██████████████████▉              │
17. │ 2011 │ 232807 │ ██████████████████▌              │
18. │ 2012 │ 238384 │ ███████████████████              │
19. │ 2013 │ 256926 │ ████████████████████▌            │
20. │ 2014 │ 280027 │ ██████████████████████▍          │
21. │ 2015 │ 297287 │ ███████████████████████▊         │
22. │ 2016 │ 313551 │ █████████████████████████        │
23. │ 2017 │ 346516 │ ███████████████████████████▋     │
24. │ 2018 │ 351101 │ ████████████████████████████     │
25. │ 2019 │ 352923 │ ████████████████████████████▏    │
26. │ 2020 │ 377673 │ ██████████████████████████████▏  │
27. │ 2021 │ 383795 │ ██████████████████████████████▋  │
28. │ 2022 │ 397233 │ ███████████████████████████████▊ │
29. │ 2023 │ 358654 │ ████████████████████████████▋    │
    └──────┴────────┴──────────────────────────────────┘

29 rows in set. Elapsed: 0.066 sec. Processed 28.28 million rows, 169.66 MB (428.58 million rows/s., 2.57 GB/s.)
Peak memory usage: 5.96 MiB.
```
```sql
SELECT
    toYear(date) AS year,
    round(avg(price)) AS price,
    bar(price, 0, 2000000, 100)
FROM otus_ch.uk_price_paid_dist
WHERE town = 'LONDON'
GROUP BY year
ORDER BY year;
```
```bash
Query id: ae0bc99e-e956-4f3e-b1b9-3a4110192f04

    ┌─year─┬───price─┬─bar(price, 0, 2000000, 100)───────────────────────────┐
 1. │ 1995 │  109119 │ █████▍                                                │
 2. │ 1996 │  118674 │ █████▉                                                │
 3. │ 1997 │  136529 │ ██████▊                                               │
 4. │ 1998 │  153013 │ ███████▋                                              │
 5. │ 1999 │  180643 │ █████████                                             │
 6. │ 2000 │  215865 │ ██████████▊                                           │
 7. │ 2001 │  233002 │ ███████████▋                                          │
 8. │ 2002 │  263692 │ █████████████▏                                        │
 9. │ 2003 │  278411 │ █████████████▉                                        │
10. │ 2004 │  304665 │ ███████████████▏                                      │
11. │ 2005 │  322886 │ ████████████████▏                                     │
12. │ 2006 │  356187 │ █████████████████▊                                    │
13. │ 2007 │  404062 │ ████████████████████▏                                 │
14. │ 2008 │  420741 │ █████████████████████                                 │
15. │ 2009 │  427773 │ █████████████████████▍                                │
16. │ 2010 │  480329 │ ████████████████████████                              │
17. │ 2011 │  496293 │ ████████████████████████▊                             │
18. │ 2012 │  519474 │ █████████████████████████▉                            │
19. │ 2013 │  616189 │ ██████████████████████████████▊                       │
20. │ 2014 │  724092 │ ████████████████████████████████████▏                 │
21. │ 2015 │  792273 │ ███████████████████████████████████████▌              │
22. │ 2016 │  843737 │ ██████████████████████████████████████████▏           │
23. │ 2017 │  983704 │ █████████████████████████████████████████████████▏    │
24. │ 2018 │ 1016710 │ ██████████████████████████████████████████████████▊   │
25. │ 2019 │ 1041944 │ ████████████████████████████████████████████████████  │
26. │ 2020 │ 1061942 │ █████████████████████████████████████████████████████ │
27. │ 2021 │  966382 │ ████████████████████████████████████████████████▎     │
28. │ 2022 │  977961 │ ████████████████████████████████████████████████▉     │
29. │ 2023 │  814352 │ ████████████████████████████████████████▋             │
    └──────┴─────────┴───────────────────────────────────────────────────────┘

29 rows in set. Elapsed: 0.022 sec. Processed 28.28 million rows, 74.21 MB (1.28 billion rows/s., 3.36 GB/s.)
Peak memory usage: 4.94 MiB.
```
```sql
SELECT
    town,
    district,
    count() AS c,
    round(avg(price)) AS price,
    bar(price, 0, 5000000, 100)
FROM otus_ch.uk_price_paid_dist
WHERE date >= '2020-01-01'
GROUP BY
    town,
    district
HAVING c >= 100
ORDER BY price DESC
LIMIT 100;
```
```bash
Query id: 53e2f9f3-ec54-439c-a0dc-e26439436401

     ┌─town─────────────────┬─district───────────────┬─────c─┬───price─┬─bar(price, 0, 5000000, 100)───────────────────────────────────┐
  1. │ LONDON               │ CITY OF LONDON         │   778 │ 3022892 │ ████████████████████████████████████████████████████████████▍ │
  2. │ LONDON               │ CITY OF WESTMINSTER    │ 10350 │ 2841912 │ ████████████████████████████████████████████████████████▊     │
  3. │ LONDON               │ KENSINGTON AND CHELSEA │  6957 │ 2446447 │ ████████████████████████████████████████████████▉             │
  4. │ LEATHERHEAD          │ ELMBRIDGE              │   268 │ 2095954 │ █████████████████████████████████████████▉                    │
  5. │ VIRGINIA WATER       │ RUNNYMEDE              │   406 │ 2047191 │ ████████████████████████████████████████▉                     │
  6. │ LONDON               │ CAMDEN                 │  7808 │ 1612002 │ ████████████████████████████████▏                             │
  7. │ NORTHWOOD            │ THREE RIVERS           │   155 │ 1508494 │ ██████████████████████████████▏                               │
  8. │ WINDSOR              │ BRACKNELL FOREST       │   115 │ 1322279 │ ██████████████████████████▍                                   │
  9. │ WINDLESHAM           │ SURREY HEATH           │   242 │ 1308169 │ ██████████████████████████▏                                   │
 10. │ COBHAM               │ ELMBRIDGE              │   962 │ 1307455 │ ██████████████████████████▏                                   │
 11. │ LONDON               │ RICHMOND UPON THAMES   │  1818 │ 1272780 │ █████████████████████████▍                                    │
 12. │ BARNET               │ ENFIELD                │   408 │ 1237521 │ ████████████████████████▊                                     │
 13. │ BEACONSFIELD         │ BUCKINGHAMSHIRE        │   899 │ 1232007 │ ████████████████████████▋                                     │
 14. │ LONDON               │ ISLINGTON              │  7541 │ 1215804 │ ████████████████████████▎                                     │
 15. │ ESHER                │ ELMBRIDGE              │  1236 │ 1142843 │ ██████████████████████▊                                       │
 16. │ LONDON               │ HOUNSLOW               │  1750 │ 1137291 │ ██████████████████████▋                                       │
 17. │ RICHMOND             │ RICHMOND UPON THAMES   │  2241 │ 1135220 │ ██████████████████████▋                                       │
 18. │ ASCOT                │ WINDSOR AND MAIDENHEAD │  1084 │ 1123875 │ ██████████████████████▍                                       │
 19. │ BURFORD              │ WEST OXFORDSHIRE       │   237 │ 1109537 │ ██████████████████████▏                                       │
 20. │ RADLETT              │ HERTSMERE              │   684 │ 1067485 │ █████████████████████▎                                        │
 21. │ WEYBRIDGE            │ ELMBRIDGE              │  1668 │ 1061209 │ █████████████████████▏                                        │
 22. │ LONDON               │ HAMMERSMITH AND FULHAM │  8298 │ 1054860 │ █████████████████████                                         │
 23. │ IVER                 │ BUCKINGHAMSHIRE        │   564 │ 1047813 │ ████████████████████▉                                         │
 24. │ LEATHERHEAD          │ GUILDFORD              │   477 │ 1042518 │ ████████████████████▊                                         │
 25. │ GUILDFORD            │ WAVERLEY               │   349 │ 1035625 │ ████████████████████▋                                         │
 26. │ SALCOMBE             │ SOUTH HAMS             │   273 │ 1030394 │ ████████████████████▌                                         │
 27. │ CHALFONT ST GILES    │ BUCKINGHAMSHIRE        │   383 │ 1021218 │ ████████████████████▍                                         │
 28. │ LONDON               │ TOWER HAMLETS          │ 13683 │ 1020576 │ ████████████████████▍                                         │
 29. │ BROCKENHURST         │ NEW FOREST             │   288 │ 1020090 │ ████████████████████▍                                         │
 30. │ WEMBLEY              │ BRENT                  │  2327 │ 1010404 │ ████████████████████▏                                         │
 31. │ FARNHAM              │ HART                   │   148 │  997985 │ ███████████████████▉                                          │
 32. │ SURBITON             │ ELMBRIDGE              │   249 │  992706 │ ███████████████████▊                                          │
 33. │ FARNHAM              │ EAST HAMPSHIRE         │   130 │  985642 │ ███████████████████▋                                          │
 34. │ KINGSTON UPON THAMES │ RICHMOND UPON THAMES   │   214 │  984167 │ ███████████████████▋                                          │
 35. │ HARTFIELD            │ WEALDEN                │   104 │  983591 │ ███████████████████▋                                          │
 36. │ GERRARDS CROSS       │ BUCKINGHAMSHIRE        │  1116 │  982577 │ ███████████████████▋                                          │
 37. │ PETERSFIELD          │ CHICHESTER             │   118 │  979286 │ ███████████████████▌                                          │
 38. │ LONDON               │ KINGSTON UPON THAMES   │   118 │  966098 │ ███████████████████▎                                          │
 39. │ SUTTON COLDFIELD     │ LICHFIELD              │   142 │  958081 │ ███████████████████▏                                          │
 40. │ EAST MOLESEY         │ ELMBRIDGE              │   478 │  939515 │ ██████████████████▊                                           │
 41. │ LONDON               │ MERTON                 │  6045 │  922843 │ ██████████████████▍                                           │
 42. │ COVENTRY             │ WARWICK                │   114 │  917819 │ ██████████████████▎                                           │
 43. │ CROYDON              │ SUTTON                 │   206 │  911875 │ ██████████████████▏                                           │
 44. │ HARPENDEN            │ ST ALBANS              │  1706 │  902656 │ ██████████████████                                            │
 45. │ LONDON               │ WANDSWORTH             │ 17977 │  902221 │ ██████████████████                                            │
 46. │ HENLEY-ON-THAMES     │ SOUTH OXFORDSHIRE      │  1412 │  894052 │ █████████████████▉                                            │
 47. │ INGATESTONE          │ CHELMSFORD             │   164 │  887792 │ █████████████████▊                                            │
 48. │ HASSOCKS             │ LEWES                  │   115 │  881935 │ █████████████████▋                                            │
 49. │ BILLINGSHURST        │ CHICHESTER             │   334 │  879227 │ █████████████████▌                                            │
 50. │ EGHAM                │ RUNNYMEDE              │  1313 │  876746 │ █████████████████▌                                            │
 51. │ KINGSTON UPON THAMES │ KINGSTON UPON THAMES   │  2448 │  874755 │ █████████████████▍                                            │
 52. │ THAMES DITTON        │ ELMBRIDGE              │   611 │  860107 │ █████████████████▏                                            │
 53. │ LONDON               │ SOUTHWARK              │ 10688 │  858899 │ █████████████████▏                                            │
 54. │ POTTERS BAR          │ WELWYN HATFIELD        │   404 │  855273 │ █████████████████                                             │
 55. │ EAST GRINSTEAD       │ TANDRIDGE              │   152 │  848614 │ ████████████████▉                                             │
 56. │ OXFORD               │ SOUTH OXFORDSHIRE      │   870 │  839695 │ ████████████████▊                                             │
 57. │ HASLEMERE            │ CHICHESTER             │   300 │  825957 │ ████████████████▌                                             │
 58. │ SOLIHULL             │ STRATFORD-ON-AVON      │   178 │  815974 │ ████████████████▎                                             │
 59. │ CHIGWELL             │ EPPING FOREST          │   626 │  814199 │ ████████████████▎                                             │
 60. │ LONDON               │ BARNET                 │ 10923 │  812946 │ ████████████████▎                                             │
 61. │ TONBRIDGE            │ SEVENOAKS              │   137 │  811678 │ ████████████████▏                                             │
 62. │ MARLOW               │ BUCKINGHAMSHIRE        │  1017 │  810996 │ ████████████████▏                                             │
 63. │ LONDON               │ HACKNEY                │  9202 │  809918 │ ████████████████▏                                             │
 64. │ TWICKENHAM           │ RICHMOND UPON THAMES   │  2944 │  808759 │ ████████████████▏                                             │
 65. │ TEDDINGTON           │ RICHMOND UPON THAMES   │  1509 │  805394 │ ████████████████                                              │
 66. │ LONDON               │ EALING                 │  7847 │  802027 │ ████████████████                                              │
 67. │ LUTTERWORTH          │ HARBOROUGH             │  1520 │  801920 │ ████████████████                                              │
 68. │ STOCKBRIDGE          │ TEST VALLEY            │   426 │  798929 │ ███████████████▉                                              │
 69. │ PETWORTH             │ CHICHESTER             │   373 │  796990 │ ███████████████▉                                              │
 70. │ PULBOROUGH           │ CHICHESTER             │   124 │  789573 │ ███████████████▊                                              │
 71. │ WOKING               │ GUILDFORD              │   469 │  788648 │ ███████████████▊                                              │
 72. │ LONDON               │ BRENT                  │  5733 │  787517 │ ███████████████▊                                              │
 73. │ HINDHEAD             │ WAVERLEY               │   308 │  784250 │ ███████████████▋                                              │
 74. │ BERKHAMSTED          │ DACORUM                │  1391 │  784001 │ ███████████████▋                                              │
 75. │ SOLIHULL             │ WARWICK                │   168 │  783729 │ ███████████████▋                                              │
 76. │ LECHLADE             │ COTSWOLD               │   220 │  775501 │ ███████████████▌                                              │
 77. │ MUCH HADHAM          │ EAST HERTFORDSHIRE     │   119 │  773409 │ ███████████████▍                                              │
 78. │ LUTON                │ CENTRAL BEDFORDSHIRE   │   549 │  773280 │ ███████████████▍                                              │
 79. │ GREAT MISSENDEN      │ BUCKINGHAMSHIRE        │   592 │  770374 │ ███████████████▍                                              │
 80. │ UPMINSTER            │ THURROCK               │   114 │  767667 │ ███████████████▎                                              │
 81. │ BELVEDERE            │ BEXLEY                 │   905 │  763645 │ ███████████████▎                                              │
 82. │ KESTON               │ BROMLEY                │   237 │  761385 │ ███████████████▏                                              │
 83. │ RICKMANSWORTH        │ THREE RIVERS           │  1964 │  757539 │ ███████████████▏                                              │
 84. │ WALTON-ON-THAMES     │ ELMBRIDGE              │  2509 │  748582 │ ██████████████▉                                               │
 85. │ MAIDENHEAD           │ BUCKINGHAMSHIRE        │   334 │  747743 │ ██████████████▉                                               │
 86. │ MAYFIELD             │ WEALDEN                │   238 │  746935 │ ██████████████▉                                               │
 87. │ LONDON               │ LAMBETH                │ 14198 │  746337 │ ██████████████▉                                               │
 88. │ AMERSHAM             │ BUCKINGHAMSHIRE        │  1250 │  744414 │ ██████████████▉                                               │
 89. │ INGATESTONE          │ BRENTWOOD              │   392 │  744319 │ ██████████████▉                                               │
 90. │ TADWORTH             │ REIGATE AND BANSTEAD   │  1267 │  741866 │ ██████████████▊                                               │
 91. │ READING              │ SOUTH OXFORDSHIRE      │   799 │  740990 │ ██████████████▊                                               │
 92. │ BETCHWORTH           │ MOLE VALLEY            │   200 │  737782 │ ██████████████▊                                               │
 93. │ NORTHWOOD            │ HILLINGDON             │   721 │  735572 │ ██████████████▋                                               │
 94. │ LONDON               │ HARINGEY               │  8809 │  734889 │ ██████████████▋                                               │
 95. │ SHAFTESBURY          │ WILTSHIRE              │   102 │  733665 │ ██████████████▋                                               │
 96. │ CHISLEHURST          │ BROMLEY                │  1136 │  730516 │ ██████████████▌                                               │
 97. │ SLOUGH               │ BUCKINGHAMSHIRE        │  1121 │  729931 │ ██████████████▌                                               │
 98. │ BOURNE END           │ BUCKINGHAMSHIRE        │   388 │  728769 │ ██████████████▌                                               │
 99. │ WELWYN               │ WELWYN HATFIELD        │   525 │  728743 │ ██████████████▌                                               │
100. │ UXBRIDGE             │ BUCKINGHAMSHIRE        │   438 │  723766 │ ██████████████▍                                               │
     └─town─────────────────┴─district───────────────┴─────c─┴───price─┴─bar(price, 0, 5000000, 100)───────────────────────────────────┘

100 rows in set. Elapsed: 0.053 sec. Processed 28.28 million rows, 147.60 MB (529.45 million rows/s., 2.76 GB/s.)
Peak memory usage: 59.31 MiB.
```
 снимаем итоговое время. 
 ```sql
SYSTEM FLUSH LOGS;

SELECT
    event_time,
    round(query_duration_ms / 1000, 3) AS sec,
    read_rows,
    formatReadableSize(read_bytes) AS read_size,
    query
FROM system.query_log
WHERE type = 'QueryFinish'
  AND query LIKE '%FROM otus_ch.uk_price_paid_dist%'
ORDER BY event_time DESC
LIMIT 10;
```
```bash
  ┌──────────event_time─┬───sec─┬─read_rows─┬─read_size──┬─query─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
2. │ 2026-03-09 12:17:35 │ 0.052 │  28276228 │ 140.76 MiB │ SELECT
    town,
    district,
    count() AS c,
    round(avg(price)) AS price,
    bar(price, 0, 5000000, 100)
FROM otus_ch.uk_price_paid_dist
WHERE date >= '2020-01-01'
GROUP BY
    town,
    district
HAVING c >= 100
ORDER BY price DESC
LIMIT 100; │
   └─────────────────────┴───────┴───────────┴────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
   ┌──────────event_time─┬───sec─┬─read_rows─┬─read_size──┬─query──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
3. │ 2026-03-09 12:16:56 │ 0.021 │  28276228 │ 70.77 MiB  │ SELECT
    toYear(date) AS year,
    round(avg(price)) AS price,
    bar(price, 0, 2000000, 100)
FROM otus_ch.uk_price_paid_dist
WHERE town = 'LONDON'
GROUP BY year
ORDER BY year;                                                                               │
4. │ 2026-03-09 12:15:11 │ 0.065 │  28276228 │ 161.80 MiB │ SELECT
    toYear(date) AS year,
    round(avg(price)) AS price,
    bar(price, 0, 1000000, 80)
FROM otus_ch.uk_price_paid_dist
GROUP BY year
ORDER BY year;         
```
###   Выводы:
```text
Сравнение производительности выполнено на одинаковом наборе данных UK Price Paid и на одинаковых SQL-запросах для двух вариантов:
single-таблица (otus_ch..uk_price_paid) и распределенная таблица в кластере (otus_ch.uk_price_paid_dist).

Результаты:

Запрос avg(price) по годам
single: 0.035 сек, cluster: 0.065 сек
Вывод: в данном запросе single быстрее, так как накладные расходы распределенного выполнения выше, чем потенциальный выигрыш от распараллеливания.

Запрос avg(price) по годам для London
single: 0.019 сек, cluster: 0.021 сек
Вывод: результаты близкие, но single немного быстрее; для такого объема/фильтра преимущество кластера практически не проявляется.

Запрос town/district + HAVING + сортировка (более тяжелая агрегация)
single: 0.059 сек, cluster: 0.052 сек
Вывод: в более сложном запросе кластер показал лучшую скорость за счет параллельной обработки на нескольких шардах.

Итоговый вывод:
Кластерный режим не гарантирует ускорение для каждого запроса. На простых и сравнительно легких агрегатах single может
быть быстрее из-за сетевых и координационных overhead. На более тяжелых аналитических запросах распределенная таблица
в кластере дает выигрыш по времени выполнения.
```





