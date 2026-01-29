1. Скачал образ для докера и развернул контейнер монги
2. Cоздал папку import и добавил туда тестовые данные movies and comments
3. Используя команду mongoimport --db test_db --collection movies --file /import/movies.json - добавил эти коллекции в тестовую БД монги
4. Последовательно выполнял команды:
   // 1. Простой поиск - выборка всех документов в коллекции
      db.collection.find()
    // 2. Поиск по полю - выборка документов, где поле равно указанному значению
db.collection.find({ field: <value> })
  // 3. Поиск по вложенному полю - выборка документов по значению вложенного
поля
db.collection.find({ 'path.to.field': <value> })
  // 4. Поиск с условием "больше или равно" - выборка документов, где возраст
>= 18
  db.collection.find({ age: { $gte: 18 } })
  // 5. Поиск с условием "входит в массив" - выборка документов, где возраст
равен 18 или 19
db.collection.find({ age: { $in: [18, 19] } })
// 6. Проекция - выборка только указанных полей (например, имя и возраст)
db.collection.find({}, { name: 1, age: 1 })
// 7. Сортировка - сортировка результатов по возрасту (возрастание и
убывание)
db.collection.find().sort({ age: 1 }) // По возрастанию
db.collection.find().sort({ age: -1 }) // По убыванию
// 8. Лимит - ограничение количества возвращаемых документов до 10
db.collection.find().limit(10)
// 9. Пропуск - пропуск первых 10 документов в результатах поиска
db.collection.find().skip(10)
// 10. Условие $and - выборка документов, соответствующих нескольким
условиям одновременно
db.collection.find({ $and: [{ age: { $gte: 18 } }, { city: "New York" }] })

INSERT
db.collection.insertOne({ name: "John", age: 30, status: "active" })
db.collection.insertMany([ { name: "Jane", age: 25, status: "active" }, {
name: "Doe", age: 28, status: "inactive" } ])
<img width="2874" height="2046" alt="image" src="https://github.com/user-attachments/assets/cb6e212e-4ed7-4c22-96f8-f38468b071aa" />
