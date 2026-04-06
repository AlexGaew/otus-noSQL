## 1. Что такое etcd

etcd — это распределенное key-value NoSQL хранилище с открытым исходным кодом, ориентированное на надежное хранение конфигурации, сервис-дискавери и координацию распределенных систем. Основное преимущество etcd — строгая консистентность данных в кластере за счет алгоритма консенсуса Raft.

etcd относится к типу key-value store и предоставляет простой API для чтения/записи значений, подписки на изменения (watch) и примитивы координации (lease, lock, election). Это делает его базовым компонентом инфраструктурных платформ, включая Kubernetes.

Проект был создан компанией CoreOS и позже передан в CNCF. Сегодня etcd широко применяется как отказоустойчивое хранилище состояния control-plane в облачных и микросервисных средах.

### Ключевые характеристики

- **Тип**: Distributed key-value NoSQL
- **Модель данных**: Ключ-значение + версии ключей
- **Скорость**: Высокая, с акцентом на консистентность
- **Персистентность**: WAL + snapshots
- **Масштабируемость**: Кластер с Raft (обычно нечетное число узлов)
- **Основные кейсы**: Конфигурация, service discovery, coordination, state store

### Основные концепции

**Raft и quorum**. Запись подтверждается большинством узлов, что обеспечивает согласованное состояние кластера.

**Линейризуемые чтения**. etcd поддерживает строгие чтения, когда клиент получает актуальное согласованное состояние.

**MVCC (многоверсионность)**. Каждое изменение получает ревизию, что позволяет безопасно смотреть историю изменений и использовать watch.

**Watch API**. Клиенты могут подписываться на изменения ключей и в реальном времени реагировать на обновления.

**Lease и TTL**. Временные ключи и heartbeat-механика удобны для регистрации сервисов и leader election.

### CAP-теорема

etcd в сетевых разделениях делает выбор в пользу **CP** (Consistency + Partition tolerance): при проблемах связи часть узлов может стать недоступной для записи, но согласованность данных сохраняется.

---

## 2. Для чего использовать etcd

### Идеальные сценарии

**Хранение конфигурации инфраструктуры**. Централизованное и консистентное хранение параметров сервисов.

**Service discovery**. Регистрация сервисов через ключи с lease/TTL и автоматическое удаление устаревших записей.

**Координация распределенных приложений**. Distributed lock, election и другие примитивы синхронизации.

**State store для control-plane**. Классический пример — хранение состояния Kubernetes.

**Реактивные сценарии**. Watch-модель позволяет строить системы, мгновенно реагирующие на изменения конфигурации.

### Когда НЕ стоит использовать etcd

**Большие объемы бизнес-данных**. etcd не предназначен как основная OLTP/OLAP база для тяжелых нагрузок на данные.

**Сложные JOIN и аналитика**. Это не реляционная и не колонночная аналитическая СУБД.

**Кэш с максимальной скоростью и минимальной латентностью любой ценой**. Для этого чаще выбирают in-memory решения вроде Redis.

**Хранение файлов и крупных бинарных объектов**. etcd лучше использовать для метаданных и состояния, а не для blob-данных.

### Реальные примеры использования

- Хранение состояния и конфигурации Kubernetes control-plane
- Service discovery в микросервисной инфраструктуре
- Distributed lock для фоновых воркеров
- Leader election для отказоустойчивых сервисов
- Хранение feature flags и runtime-конфигурации

---

## Краткое резюме

etcd — это надежное распределенное key-value NoSQL хранилище для конфигурации и координации в distributed-системах. Оно особенно полезно там, где критична строгая консистентность, но не подходит как универсальная база для больших объемов прикладных данных.

## 3. Выполнение домашнего задания
## 3.1 Деплой в Docker
###  при помощи docker-compose развернем redis в docker. 
```yml
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
### Проверим, что кластеры живы.
```bash
docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  endpoint health
```
```
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  endpoint health

http://etcd1:2379 is healthy: successfully committed proposal: took = 2.0805ms
http://etcd2:2379 is healthy: successfully committed proposal: took = 2.159083ms
http://etcd3:2379 is healthy: successfully committed proposal: took = 2.659958ms
```
### Выведем основную информацию о кластерах.
```bash
docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  endpoint status -w table

```
```bash
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  endpoint status -w table

+-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+
|     ENDPOINT      |        ID        | VERSION | DB SIZE | IS LEADER | IS LEARNER | RAFT TERM | RAFT INDEX | RAFT APPLIED INDEX | ERRORS |
+-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+
| http://etcd1:2379 | 99eab3685d8363a1 |  3.5.14 |   29 kB |      true |      false |         2 |         14 |                 14 |        |
| http://etcd2:2379 | dcb68c82481661be |  3.5.14 |   20 kB |     false |      false |         2 |         14 |                 14 |        |
| http://etcd3:2379 | 876043ef79ada1ea |  3.5.14 |   20 kB |     false |      false |         2 |         14 |                 14 |        |
+-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+
```
### Проверим возможность записи/чтения.
```bash
docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  put hw/key v1

docker exec -e ETCDCTL_API=3 etcd2 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  get hw/key
```
```bash
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  put hw/key v1
OK
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd2 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  get hw/key
hw/key
v1
```
### Останавливаем лидера.
docker stop etcd1
### Проверяем, что на оставшихся 2 нодах есть quorum и запись работает:
```bash
docker exec -e ETCDCTL_API=3 etcd2 /usr/local/bin/etcdctl \
  --endpoints=http://etcd2:2379,http://etcd3:2379 \
  endpoint health

http://etcd2:2379 is healthy: successfully committed proposal: took = 1.758666ms
http://etcd3:2379 is healthy: successfully committed proposal: took = 1.849583ms

docker exec -e ETCDCTL_API=3 etcd2 /usr/local/bin/etcdctl \
  --endpoints=http://etcd2:2379,http://etcd3:2379 \
  endpoint status -w table

  +-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+
|     ENDPOINT      |        ID        | VERSION | DB SIZE | IS LEADER | IS LEARNER | RAFT TERM | RAFT INDEX | RAFT APPLIED INDEX | ERRORS |
+-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+
| http://etcd2:2379 | dcb68c82481661be |  3.5.14 |   20 kB |      true |      false |         3 |         18 |                 18 |        |
| http://etcd3:2379 | 876043ef79ada1ea |  3.5.14 |   20 kB |     false |      false |         3 |         18 |                 18 |        |
+-------------------+------------------+---------+---------+-----------+------------+-----------+------------+--------------------+--------+

docker exec -e ETCDCTL_API=3 etcd2 /usr/local/bin/etcdctl \
  --endpoints=http://etcd2:2379,http://etcd3:2379 \
  put hw/key v2-after-etcd1-down

  OK

  docker exec -e ETCDCTL_API=3 etcd3 /usr/local/bin/etcdctl \
  --endpoints=http://etcd2:2379,http://etcd3:2379 \
  get hw/key

  v2-after-etcd1-down
  ```
  ### Для демонстрации границы отказоустойчивости остановим еще одну ноду (etcd2):
  
  docker stop etcd2
  
```bash
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd3 /usr/local/bin/etcdctl \
  --endpoints=http://etcd3:2379 \
  put hw/key v3-should-fail

{"level":"warn","ts":"2026-04-06T10:56:32.799963Z","logger":"etcd-client","caller":"v3@v3.5.14/retry_interceptor.go:63","msg":"retrying of unary invoker failed","target":"etcd-endpoints://0x400037c000/etcd3:2379","attempt":0,"error":"rpc error: code = DeadlineExceeded desc = context deadline exceeded"}

Error: context deadline exceeded
```
### Восстанавливаем остановленные ноды

docker start etcd1 etcd2

```bash
aleksandrgaev@MacBook-Pro-Aleksandr etcd % docker exec -e ETCDCTL_API=3 etcd1 /usr/local/bin/etcdctl \
  --endpoints=http://etcd1:2379,http://etcd2:2379,http://etcd3:2379 \
  endpoint health
http://etcd2:2379 is healthy: successfully committed proposal: took = 4.174417ms
http://etcd1:2379 is healthy: successfully committed proposal: took = 4.258375ms
http://etcd3:2379 is healthy: successfully committed proposal: took = 4.136792ms
```
## 4. Результаты выполнения домашнего задания

### 4.1 Что было сделано
- Развернут кластер `etcd` из 3 нод: `etcd1`, `etcd2`, `etcd3` (через `docker-compose`).
- Проверено состояние кластера командами `endpoint status` и `endpoint health`.
- Выполнена проверка записи и чтения ключа в штатном режиме.

### 4.2 Проверка отказоустойчивости
1. Определен лидер кластера (`etcd1`).
2. Остановлен лидер (`etcd1`).
3. Проверены чтение/запись на оставшихся 2 нодах (`etcd2`, `etcd3`) — операции выполнялись успешно.
4. Остановлена еще одна нода (в работе осталась 1 из 3).
5. Выполнена попытка записи на одной ноде — операция не выполнена из-за отсутствия quorum.
6. Ноды восстановлены, проверка `endpoint health` показала, что все 3 ноды снова в рабочем состоянии.

### 4.3 Результаты
- При отказе 1 ноды из 3 кластер продолжает работать.
- При отказе 2 нод из 3 запись недоступна (потерян quorum).
- После восстановления нод кластер возвращается в штатный режим работы.

### 4.4 Вывод
Отказоустойчивость кластера `etcd` подтверждена. Поведение соответствует модели **CP**: при наличии quorum сохраняется корректная работа и консистентность данных, при потере quorum запись невозможна, после восстановления узлов работоспособность кластера полностью возвращается.
