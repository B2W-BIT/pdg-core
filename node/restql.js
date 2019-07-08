var restql = require('../dist/lib/restql')

module.exports = {
  executeQuery: restql.restql.core.api.restql.execute_query_async
}
