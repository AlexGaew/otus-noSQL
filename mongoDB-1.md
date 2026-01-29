# Работа с MongoDB в Docker — практический разбор

## 1.Развёртывание MongoDB
1. Скачал образ для докера и развернул контейнер монги  
2. Cоздал папку `import` и добавил туда тестовые данные `movies` and `comments`  
3. Используя команду mongoimport –db test_db –collection movies –file /import/movies.json добавил эти коллекции в тестовую БД монги  

## 2.Базовые операции поиска (`find`)

```js
Простой поиск - выборка всех документов в коллекции
db.collection.find()

Поиск по полю - выборка документов, где поле равно указанному значению
db.collection.find({ field: <value> })

Поиск по вложенному полю - выборка документов по значению вложенного поля
db.collection.find({ 'path.to.field': <value> })

Поиск с условием "больше или равно" - выборка документов, где возраст >= 18
db.collection.find({ age: { $gte: 18 } })

Поиск с условием "входит в массив" - выборка документов, где возраст равен 18 или 19
db.collection.find({ age: { $in: [18, 19] } })

Проекция - выборка только указанных полей (например, имя и возраст)
db.collection.find({}, { name: 1, age: 1 })

Сортировка - сортировка результатов по возрасту
db.collection.find().sort({ age: 1 })   // По возрастанию
db.collection.find().sort({ age: -1 })  // По убыванию

Лимит - ограничение количества возвращаемых документов до 10
db.collection.find().limit(10)

Пропуск - пропуск первых 10 документов в результатах поиска
db.collection.find().skip(10)

10. Условие $and - выборка документов, соответствующих нескольким условиям одновременно
db.collection.find({ $and: [{ age: { $gte: 18 } }, { city: "New York" }] })
```

## 3.INSERT
```js
db.movies.insertOne({plot:'Кино', genres:"кино"})
db.movies.insertMany([
  {plot:'asdf', genres:"aaaaa"},
  {plot:'zzzzz', genres:"zzzzz"}
])
```
<img width="1408" height="386" alt="image" src="https://github.com/user-attachments/assets/461ab350-9e4c-4dcf-8744-f13a63ba0b4b" />

## 4.Агрегации и подсчёты

```js
db.comments.countDocuments({ name: "Sansa Stark"})
```
<img width="2874" height="2046" alt="image" src="https://github.com/user-attachments/assets/cb6e212e-4ed7-4c22-96f8-f38468b071aa" />

## 5.Explain и анализ выполнения запросов
``` js
db.movies.find().explain("executionStats")
```
<img width="1408" height="1978" alt="image" src="https://github.com/user-attachments/assets/0e8d166a-53cb-4c63-91cd-a94d67a8d352" />

## 6.Работа с индексами
```js
db.movies.createIndex({ title: 1 })
```
<img width="1408" height="270" alt="image" src="https://github.com/user-attachments/assets/d3d657d8-7400-4167-95e3-136b37fbbc98" />

## 7.Сравнение поиска с индексом и без индекса
```js
executionStats: {
  executionSuccess: true,
  nReturned: 1,
  executionTimeMillis: 41,
  totalKeysExamined: 0,
  totalDocsExamined: 73846,
  executionStages: {
    isCached: false,
    stage: 'COLLSCAN'
```

С индексом
```js
executionStats: {
  executionSuccess: true,
  nReturned: 1,
  executionTimeMillis: 0,
  totalKeysExamined: 1,
  totalDocsExamined: 1,
  executionStages: {
    isCached: false,
    stage: 'FETCH',
    nReturned: 1,
    executionTimeMillisEstimate: 0,
    works: 2,
    advanced: 1,
    needTime: 0,
    needYield: 0,
    saveState: 0,
    restoreState: 0,
    isEOF: 1,
    docsExamined: 1,
    alreadyHasObj: 0,
    inputStage: {
      stage: 'IXSCAN'
```
Видно, что с индексом поиск уменьшился:
было 41 мс, стало 0 мс.



