How to run it
=====================================

Download and install Play 2.2

Run your MongoDB server and create a "sales" capped collection inside a "menhirexpress" db.

```
$ mkdir sales
$ mongod --dbpath sales &
$ mongo
$ use menhirexpress
$ db.createCollection("sales", {capped: true, size: 1000, max: 1000})
```

Run the application with `play run`

Then go to http://localhost:9000/?role=MANAGER or http://localhost:9000/mobile


![Final app](https://raw.githubusercontent.com/mathieuancelin/menhirexpress-devoxxfr14/master/codestory.gif)
