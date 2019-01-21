[![Build Status](https://travis-ci.org/B2W-BIT/restQL-clojure.svg?branch=master)](https://travis-ci.org/B2W-BIT/restQL-clojure)
[![codecov.io](https://codecov.io/github/B2W-BIT/restQL-clojure/coverage.svg?branch=master)](https://codecov.io/github/B2W-BIT/restQL-clojure?branch=master)
[![Dependencies Status](https://jarkeeper.com/B2W-BIT/restQL-clojure/status.svg)](https://jarkeeper.com/B2W-BIT/restQL-clojure)

# restQL-clojure

restQL-clojure allows you to run restQL queries directly from JVM applications, making easy to fetch information from multiple services in the most efficient manner. To query using HTTP calls from any client check [restQL-http](https://github.com/B2W-BIT/restQL-http):

```
from search
    with
        role = "hero"

from hero as heroList
    with
        name = search.results.name
```

Links
* [#restql](https://clojurians.slack.com/messages/C8S6EG8BF): [clojurians](https://clojurians.slack.com) restQL Slack channel
* [@restQL](https://t.me/restQL): restQL Telegram Group
* [@restQLBR](https://t.me/restQLBR): Brazilian restQL Telegram Group
* [restql.b2w.io](http://restql.b2w.io): Project home page,
* [game.b2w.io](http://game.b2w.io): A game developed to teach the basics of restQL language,
* [restQL-http](https://github.com/B2W-BIT/restQL-http): The main restQL repo. It spin up a restQL-http that will listen for queries. Can be used by any language/project.
* [restQL-java](https://github.com/B2W-BIT/restQL-java): If you want to embed restQL directly into your Java application,
* [restQL-manager](https://github.com/B2W-BIT/restQL-manager): To manage saved queries and resources endpoints. restQL-manager requires a MongoDB instance.
* [Tackling microservice query complexity](https://medium.com/b2w-engineering/restql-tackling-microservice-query-complexity-27def5d09b40): Project motivation and history
* [Wiki](https://github.com/B2W-BIT/restQL-http/wiki/RestQL-Query-Language): Project documentation.

Who're talking about restQL

* [infoQ: restQL, a Microservices Query Language, Released on GitHub](https://www.infoq.com/news/2018/01/restql-released)
* [infoQ: 微服务查询语言restQL已在GitHub上发布](http://www.infoq.com/cn/news/2018/01/restql-released)
* [OSDN Mag: マイクロサービスクエリ言語「restQL 2.3」公開](https://mag.osdn.jp/18/01/12/160000)

## Getting Started

### Installation

Add restQL dependency to your project

**Lein**

```
[b2wdigital/restql-core "2.4.0"]
```

### First query

```clojure
(require '[restql.core.api.restql :as restql])
(restql/execute-query :mappings { :user "http://your.api.url/users/:name" } :query "from user with name = $name" :params { :name "Duke Nukem" } )
```

In the example above restQL will call user API passing "Duke Nukem" in the name param.

## Building From Source Code

As prerequisites to build restQL from source we have:

+ Java 8
+ Leiningen 2.x

Just clone this repo and run "lein jar".

## Help and community

If you need help you can reach the community on Telegram
- https://t.me/restQL (English)
- https://t.me/restQLBR (Portuguese)

## License

Copyright © 2016 B2W Digital

Distributed under the MIT License.
