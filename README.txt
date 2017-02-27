SQL Schema Diff
====

Status: Not yet functional.

Compare databases specified as plain "CREATE TABLE" scripts to live databases.
Compare tables, views, procedures, functions, triggers.

This should be available programmatically to use as a *unit test*.

sqlschemadiff [-e ENGINE] [--localdb db] source target

engine: Currently only MYSQL. Future: MSSQL (just use "database project"!), Postgres, ...?
localdb: DbConnection  
source & target: DbConnection/DatabaseName, or a directory containing .sql files
DbConnection: connection.cnf, or [user:pass@]host[:port]

Uses mysqldbcompare.