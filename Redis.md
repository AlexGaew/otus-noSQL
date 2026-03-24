## 1. Что такое Redis

Redis (Remote Dictionary Server) — это in-memory NoSQL хранилище с открытым исходным кодом, используемое как база данных, кэш и message broker. Основное преимущество Redis — очень высокая скорость чтения и записи за счет хранения данных в оперативной памяти.

Redis относится к типу key-value store, но поддерживает богатые структуры данных: строки, хэши, списки, множества, sorted sets, bitmaps, hyperloglog и streams. Это делает его универсальным инструментом для высоконагруженных приложений.

Проект был создан в 2009 году Сальваторе Санфилиппо (antirez). Сегодня Redis широко применяется для кэширования, сессий, очередей и real-time сценариев.

### Ключевые характеристики

- **Тип**: In-memory key-value NoSQL
- **Модель данных**: Ключ-значение + структуры данных
- **Скорость**: Очень высокая (обычно миллисекунды и ниже)
- **Персистентность**: RDB и/или AOF
- **Масштабируемость**: Репликация, Sentinel, Cluster
- **Основные кейсы**: Кэш, сессии, очереди, pub/sub, rate limiting

### Основные концепции

**In-memory хранение**. Данные хранятся в RAM, что дает минимальные задержки, но ограничивает объем доступной памятью.

**Структуры данных**. Redis предоставляет прикладные операции на уровне сервера (`SET`, `HSET`, `LPUSH`, `ZADD`, `XADD`), упрощая логику приложения.

**Персистентность**. Поддерживаются снимки (RDB) и журнал операций (AOF), что позволяет балансировать скорость и надежность.

**TTL и eviction**. Есть управление временем жизни ключей (`EXPIRE`) и политики вытеснения при нехватке памяти.

**Репликация и отказоустойчивость**. Поддерживаются master-replica, Redis Sentinel (failover), Redis Cluster (шардинг).

### CAP-теорема

В распределенных конфигурациях Redis чаще используют как высокодоступный и быстрый слой (кэш/очереди), где при сетевых проблемах возможны компромиссы по консистентности. Поведение зависит от режима и настроек репликации/подтверждения записи.

---

## 2. Для чего использовать Redis

### Идеальные сценарии

**Кэширование**. Ускорение API и страниц за счет хранения горячих данных.

**Сессии и токены**. Быстрое хранение с TTL для пользовательских сессий и auth-данных.

**Rate limiting**. Лимиты запросов по IP/пользователю через атомарные счетчики.

**Очереди и фоновые задачи**. Redis часто используется как брокер фоновой обработки.

**Real-time кейсы**. Pub/Sub, streams, статусы онлайн, лидерборды.

### Когда НЕ стоит использовать Redis

**Данные не помещаются в память**. Redis неэффективен как основной слой для очень больших cold-данных.

**Сложные JOIN и аналитика**. Это не реляционная аналитическая СУБД.

**Строгие ACID-требования**. Для критичных транзакций лучше реляционные БД.

**Архивное хранение**. Redis обычно быстрый operational-слой, а не долговременный архив.

### Реальные примеры использования

- Кэширование результатов API и SQL-запросов
- Хранение сессий и state для веб-приложений
- Ограничение частоты запросов
- Очереди задач
- Real-time статистика и лидерборды

---

## Краткое резюме

Redis — это быстрый in-memory NoSQL инструмент для кэша, сессий, очередей и real-time задач. Он отлично подходит для ускорения систем, но требует грамотной настройки памяти, персистентности и отказоустойчивости.
EOF

## 3. Выполнение домашнего задания
## 3.1 Деплой в Docker
###  при помощи docker-compose развернем redis в docker. 
```yaml
version: "3.9"

services:
  redis:
    image: redis:7.2-alpine
    container_name: redis-otus
    restart: unless-stopped
    ports:
      - "6379:6379"
    command: >
      redis-server
      --appendonly yes
      --save 60 1000
      --maxmemory-policy noeviction
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  redis_data:
```
** проверяем доступность redis в docker следующей командой: docker exec -it redis-otus redis-cli ping
получили PONG готовы к работе...
**  данной командой записываем comments.json = 18Mb в виде строки в Redis.

## 3.2 String
```bash
cat redis/data/comments.json | docker exec -i redis-otus redis-cli -x SET hw:string > /dev/null
docker exec -i redis-otus redis-cli STRLEN hw:string -  проверим размер нашего comments.json
он равен 18231014

cat data/comments.json  0.00s user 0.01s system 8% cpu 0.097 total
docker exec -i redis-otus redis-cli -x SET hw:string > /dev/null  0.01s user 0.03s system 18% cpu 0.215 total
```
** наблюдаем, что время записи comments.json составила 0.215 секунды

```bash
time docker exec -i redis-otus redis-cli --raw GET hw:string > /dev/null
0.01s user 0.03s system 20% cpu 0.212 total
```
** наблюдаем, что время  чтения comments.json составила 0.212 секунды
**Итог работы json - String: запись = 0.215, чтение = 0.212**

## 3.3 HSET
```bash
time cat redis/data/comments.json | docker exec -i redis-otus redis-cli -x HSET hw:hash payload > /dev/null
 время записи составило 0.219

time docker exec -i redis-otus redis-cli --raw HGET hw:hash payload > /dev/null
 время чтения составило 0.217
```
**Итог работы json - HSET: запись = 0.219, чтение = 0.217**

## 3.4 ZSET
```bash
time cat redis/data/comments.json | docker exec -i redis-otus redis-cli -x ZADD hw:zset 1 > /dev/null
время записи составило 0.228

time docker exec -i redis-otus redis-cli --raw ZRANGE hw:zset 0 0 > /dev/null
время чтения составило 0.213
```
**Итог работы json - ZSET: запись = 0.228, чтение = 0.213**

## 3.5 list
```bash
time cat redis/data/comments.json | docker exec -i redis-otus redis-cli -x RPUSH hw:list > /dev/null
время записи составило 0.221

time docker exec -i redis-otus redis-cli --raw LINDEX hw:list 0 > /dev/null
время чтения составило 0.216
```
**Итог работы json - List: запись = 0.221, чтение = 0.216**

## 4 Анализ результатов
В итоге всех манипуляций получили следующие типы данных в БД Redis


<img width="3484" height="578" alt="image" src="https://github.com/user-attachments/assets/569fb8d6-2d7b-40ba-9e2f-92562e5f7b75" />


Чтобы интерпретировать замеры корректно, важно смотреть на алгоритмическую сложность операций Redis:

**SET / GET -> O(1)**
**HSET / HGET -> O(1)**
**ZADD -> O(log N), ZRANGE -> O(log N + M)**
**RPUSH -> O(1), LINDEX -> O(N) (для индекса 0 фактически близко к O(1))**

***Фактические результаты:***

String: запись 0.215 s, чтение 0.212 s
HSET: запись 0.219 s, чтение 0.217 s
ZSET: запись 0.228 s, чтение 0.213 s
List: запись 0.221 s, чтение 0.216 s

***Интерпретация замеров:***

SET/GET и HSET/HGET дали почти одинаковые времена (0.215/0.212 и 0.219/0.217), что соответствует их константной сложности O(1).
Самая медленная запись у ZSET (0.228 s), что ожидаемо для ZADD с O(log N).
List на записи (0.221 s) и чтении (0.216 s) близок к String/HSET.
Разброс значений небольшой (0.212–0.228 s), значит результаты стабильны и согласованы между собой.









