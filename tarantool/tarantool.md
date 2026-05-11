## 1. Что такое Tarantool

Tarantool — это высокопроизводительная in-memory NoSQL база данных с открытым исходным кодом, разработанная компанией Mail.ru Group (VK). Отличительная черта Tarantool — встроенный сервер приложений на Lua, что позволяет хранить логику прямо внутри СУБД.

Tarantool сочетает в себе черты key-value хранилища и реляционной БД: поддерживает индексы, транзакции, вторичные ключи и SQL-подобный язык запросов (IPROTO/Lua API + начиная с версии 2.x — полноценный SQL через connector). Данные хранятся в оперативной памяти с опциональной персистентностью на диск.

Проект был создан в 2010 году, активно используется в высоконагруженных продуктах VK, Авито и других крупных компаниях рунета.

### Ключевые характеристики

- **Тип**: In-memory NoSQL + встроенный сервер приложений
- **Модель данных**: Кортежи в спейсах (spaces), напоминающие строки таблиц
- **Скорость**: Очень высокая (сотни тысяч операций в секунду)
- **Персистентность**: WAL-журнал + снимки (snapshots)
- **Масштабируемость**: Репликация (master-replica, master-master), vshard для шардинга
- **Основные кейсы**: Кэш, сессии, очереди, real-time обработка, hot-data слой

### Основные концепции

**Spaces и tuples**. Данные организованы в spaces (аналог таблиц), каждый space содержит tuples (кортежи). Схема может быть строгой (format) или свободной.

**Индексы**. Каждый space обязан иметь первичный индекс. Поддерживаются HASH, TREE, BITSET, RTREE индексы. Вторичные индексы — ключевое преимущество над чистым key-value.

**Персистентность**. Write-Ahead Log (WAL) обеспечивает durability. Периодические снимки (snapshots) ускоряют восстановление после рестарта.

**Транзакции**. Поддерживаются ACID-транзакции в рамках одного узла через механизм MVCC.

**Fiber-based конкурентность**. Tarantool работает в однопоточном event loop (на базе libev) с кооперативными fiber'ами, что исключает накладные расходы на блокировки.

**Встроенный Lua**. Хранимые процедуры, триггеры, фоновые задачи — всё пишется на Lua прямо внутри сервера.

### CAP-теорема

Tarantool в конфигурации с репликацией обеспечивает CP: при сетевых разделах предпочитает консистентность доступности. В режиме master-replica читать можно с реплик (eventual consistency для чтения).

---

## 2. Для чего использовать Tarantool

### Идеальные сценарии

**Кэширование с логикой**. Tarantool позволяет хранить кэш и одновременно выполнять сложную бизнес-логику на Lua без round-trip к приложению.

**Сессии и профили пользователей**. Быстрое хранение с TTL, вторичными индексами и транзакциями.

**Очереди задач**. Встроенный модуль `queue` реализует надёжные очереди с подтверждением доставки.

**Hot-data слой**. Хранение «горячих» данных рядом с вычислениями, разгружая основную БД.

**Real-time антифрод и аналитика**. Высокая скорость + вторичные индексы позволяют делать сложные выборки в реальном времени.

### Когда НЕ стоит использовать Tarantool

**Очень большие cold-данные**. Tarantool хранит данные в памяти — объём ограничен RAM.

**Сложная OLAP-аналитика**. Для аналитических запросов по историческим данным лучше подходят ClickHouse или PostgreSQL.

**Команда без опыта Lua**. Встроенный сервер приложений требует знания Lua для максимальной эффективности.

**Строгие требования к многоузловым транзакциям**. Распределённые транзакции между шардами не поддерживаются нативно.

### Реальные примеры использования

- Хранение сессий и профилей в высоконагруженных веб-приложениях
- Очереди фоновых задач с гарантией доставки
- Real-time лидерборды и счётчики
- Hot-data кэш поверх PostgreSQL/MySQL
- Антифрод системы с быстрыми выборками по вторичным индексам

---

## Краткое резюме

Tarantool — это in-memory СУБД с встроенным сервером приложений на Lua. Сочетает скорость Redis с возможностями реляционных индексов и транзакций. Идеален для hot-data слоя, очередей и real-time задач в высоконагруженных системах.

---

## 3. Выполнение домашнего задания

## 3.1 Деплой в Docker

### При помощи docker-compose развернём Tarantool в Docker.

```yaml
version: "3.9"

services:
  tarantool:
    image: tarantool/tarantool:2.11
    container_name: tarantool-otus
    restart: unless-stopped
    ports:
      - "3301:3301"
    environment:
      TARANTOOL_USER_NAME: admin
      TARANTOOL_USER_PASSWORD: password
    volumes:
      - tarantool_data:/var/lib/tarantool

volumes:
  tarantool_data:
```

Запуск:

```bash
docker-compose up -d
```

Проверка доступности:

```bash
docker exec -it tarantool-otus tarantool
```
### Создаем спейс в Box.
```bash
box.schema.space.create('flights', {if_not_exists = true})

```
### Задаем схему полей.
```bash
box.space.flights:format({
    {name = 'id',             type = 'unsigned'},
    {name = 'airline',        type = 'string'},
    {name = 'departure_date', type = 'string'},
    {name = 'departure_city', type = 'string'},
    {name = 'arrival_city',   type = 'string'},
    {name = 'min_price',      type = 'unsigned'}
})

```
### Создадим первичный индекс.
```bash
box.space.flights:create_index('primary', {
    type = 'hash',
    parts = {'id'}
})

```
### Создадим вторичный индекс на три поля : departure_date, airline, departure_city.
```bash
box.space.flights:create_index('secondary', {
    type = 'tree',
    parts = {'departure_date', 'airline', 'departure_city'}
})

```
### Добавим записи.
```bash
box.space.flights:insert({1, 'Aeroflot',   '2025-01-01', 'Moscow',     'Saint-Petersburg', 2500})
box.space.flights:insert({2, 'S7',         '2025-01-01', 'Moscow',     'Novosibirsk',      4200})
box.space.flights:insert({3, 'Ural Airlines', '2025-01-01', 'Yekaterinburg', 'Moscow',     2800})
box.space.flights:insert({4, 'Pobeda',     '2025-02-15', 'Moscow',     'Sochi',            1900})
box.space.flights:insert({5, 'Aeroflot',   '2025-02-15', 'Saint-Petersburg', 'Moscow',     3500})
```
### запрос для выборки минимальной стоимости авиабилета на рейсы с датой вылета 01.01.2025:
```bash 
function get_min_price(date)
local min = math.huge
  for _, tuple in box.space.flights.index.secondary:pairs(date, {iterator = 'EQ'}) do
      if tuple.min_price < min then
        min = tuple.min_price
      end
  end
  return min
end
```
### функция на Lua для вывода списка рейсов с минимальной стоимостью билета менее 3000 рублей.
```bash
function get_flights(max_cost)
  for _, tuple in box.space.flights:pairs() do
    if tuple.min_price < max_cost then
            print(tuple.airline .. ' ' .. tuple.departure_city .. ' -> ' .. tuple.arrival_city .. ' : ' .. tuple.min_price)
    end
  end
end
