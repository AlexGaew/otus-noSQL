## 1. Что такое Neo4j

Neo4j — это графовая NoSQL база данных с открытым исходным кодом, предназначенная для хранения и обработки связей между объектами. Основное преимущество Neo4j — эффективная работа со сложными взаимосвязями, которые в реляционных БД часто требуют множества JOIN.

Neo4j относится к типу property graph database: данные представлены узлами (nodes), связями (relationships) и свойствами (properties). Это делает систему особенно удобной для задач, где важна не только сущность, но и структура ее связей.

Проект был создан компанией Neo4j (ранее Neo Technology). Сегодня Neo4j широко применяется для рекомендательных систем, графов знаний, антифрода, IAM и анализа сетевых связей.

### Ключевые характеристики

- **Тип**: Graph NoSQL (Property Graph)
- **Модель данных**: Узлы, связи, свойства
- **Скорость**: Высокая для запросов по связям и графовых обходов
- **Персистентность**: Дисковое хранилище с журналом транзакций
- **Масштабируемость**: Репликация и кластерные конфигурации
- **Основные кейсы**: Рекомендации, fraud detection, knowledge graph, network analysis

### Основные концепции

**Property Graph модель**. В Neo4j объекты и связи являются сущностями первого класса, и обе стороны могут иметь атрибуты.

**Cypher**. Декларативный язык запросов, ориентированный на шаблоны графа (`MATCH`, `CREATE`, `MERGE`), что упрощает работу со связями.

**Индексирование и constraints**. Индексы и ограничения уникальности помогают ускорять поиск и поддерживать целостность данных.

**Traversal и pattern matching**. Neo4j оптимизирован для обходов графа на несколько шагов, где реляционные JOIN обычно дороги.

**ACID-транзакции**. База поддерживает транзакционную целостность при изменениях графа.

### CAP-теорема

В кластерных сценариях Neo4j ориентируется на консистентность данных и устойчивость к разделениям сети с учетом роли узлов и настроек кластера. На практике поведение зависит от конфигурации кворума и режима чтения/записи.

---

## 2. Для чего использовать Neo4j

### Идеальные сценарии

**Рекомендательные системы**. Быстрый поиск релевантных объектов на основе соседей и похожих связей.

**Антифрод**. Выявление подозрительных паттернов через цепочки транзакций, устройств и аккаунтов.

**Граф знаний**. Хранение семантических связей между сущностями в единой модели.

**Identity and Access Management (IAM)**. Моделирование ролей, прав и наследования доступа через графовые связи.

**Сетевой анализ**. Анализ маршрутов, зависимостей и влияния в IT/телеком/социальных графах.

### Когда НЕ стоит использовать Neo4j

**Простые табличные данные без связей**. Если графовых запросов нет, реляционная БД может быть проще и дешевле.

**Тяжелая OLAP-аналитика по большим фактам**. Для этого чаще используют колоночные аналитические СУБД.

**Ключ-значение кэш с минимальной латентностью**. Для таких задач обычно подходит Redis.

**Хранение больших blob-файлов**. Neo4j лучше использовать для связной модели данных, а не для медиа-объектов.

### Реальные примеры использования

- Рекомендации товаров, контента и контактов
- Выявление мошеннических схем в платежах
- Построение корпоративных и предметных knowledge graph
- Анализ связей пользователей и устройств
- Моделирование зависимостей сервисов и инфраструктуры

---

## Краткое резюме

Neo4j — это графовая NoSQL БД, которая особенно эффективна для задач со сложными связями между сущностями. Она снижает сложность графовых запросов и ускоряет анализ связей, но не является универсальной заменой реляционных и аналитических СУБД для всех типов нагрузок.

## 3. Выполнение ДЗ
 Neo4j (Cypher): одна команда, сразу сущности + связи
```C
CREATE
  (joel:Director {name:'Joel Coen'}),
  (blood:Movie {title:'Blood Simple', year:1983}),
  (frances:Actor {name:'Frances McDormand'}),
  (joel)-[:CREATED]->(blood),
  (frances)-[:PLAYED_IN {character:'Abby'}]->(blood),
  (ethan:Director {name:'Ethan Coen', born:1957}),
  (ethan)-[:CREATED]->(blood);
```

PostgreSQL: 
  ```sql
  WITH
ins_movie AS (
  INSERT INTO movies(title, year)
  VALUES ('Blood Simple', 1983)
  RETURNING id
),
ins_joel AS (
  INSERT INTO directors(name)
  VALUES ('Joel Coen')
  RETURNING id
),
ins_ethan AS (
  INSERT INTO directors(name, born)
  VALUES ('Ethan Coen', 1957)
  RETURNING id
),
ins_frances AS (
  INSERT INTO actors(name)
  VALUES ('Frances McDormand')
  RETURNING id
),
link_joel AS (
  INSERT INTO director_movie(director_id, movie_id)
  SELECT j.id, m.id FROM ins_joel j, ins_movie m
),
link_ethan AS (
  INSERT INTO director_movie(director_id, movie_id)
  SELECT e.id, m.id FROM ins_ethan e, ins_movie m
),
link_frances AS (
  INSERT INTO actor_movie(actor_id, movie_id, character_name)
  SELECT a.id, m.id, 'Abby' FROM ins_frances a, ins_movie m
)
SELECT 'ok';
  ```
 ### Вывод: 

 Для связей между сущностями проще и понятнее Neo4j/Cypher: связи видны прямо в запросе (-[:CREATED]->, -[:PLAYED_IN {...}]->).
В PostgreSQL запись длиннее: нужны отдельные таблицы связей, FK, получение id, затем отдельные вставки в link-таблицы.
Для этой задачи (граф связей) Cypher обычно читается быстрее и пишется проще.
SQL удобнее, когда данные в основном табличные и важны строгие реляционные ограничения/классическая отчетность.

и еще команда 
```C
MATCH (venom:Movie {title:'Venom'})-[*1..3]-(d:Director)
RETURN d
```

Она же в PostgreSQL

```sql
WITH RECURSIVE walk AS (
  -- старт: нода Movie 'Venom'
  SELECT
    n.id AS start_id,
    n.id AS current_id,
    0    AS depth,
    ARRAY[n.id] AS path
  FROM nodes n
  WHERE n.label = 'Movie'
    AND n.props->>'title' = 'Venom'

  UNION ALL

  -- шаги по графу в обе стороны, максимум до 3
  SELECT
    w.start_id,
    CASE WHEN e.from_id = w.current_id THEN e.to_id ELSE e.from_id END AS current_id,
    w.depth + 1,
    w.path || CASE WHEN e.from_id = w.current_id THEN e.to_id ELSE e.from_id END
  FROM walk w
  JOIN edges e
    ON e.from_id = w.current_id OR e.to_id = w.current_id
  WHERE w.depth < 3
    AND NOT (CASE WHEN e.from_id = w.current_id THEN e.to_id ELSE e.from_id END = ANY(w.path))
)
SELECT DISTINCT d.*
FROM walk w
JOIN nodes d ON d.id = w.current_id
WHERE w.depth BETWEEN 1 AND 3
  AND d.label = 'Director';
```
## Вывод
Cypher для графовых запросов обычно короче и понятнее: связи между объектами и глубина поиска задаются прямо в одной команде.
В SQL похожий запрос получается длиннее, потому что нужно явно описывать каждый шаг поиска по связям, объединения таблиц и ограничения глубины поиска.
Поэтому для задач со сложными связями удобнее Cypher, а SQL лучше подходит для привычных табличных операций.
